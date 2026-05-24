package com.gui.kline.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gui.kline.utils.ApiConfig;
import com.gui.kline.utils.JsonDataParser;
import com.gui.kline.utils.TokenManager;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthService {

    private static OkHttpClient client;
    private static OkHttpClient refreshHttpClient;
    private static final Gson gson = new Gson();

    // Safe, thread-safe memory container for cookie persistence
    private static final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

    /**
     * Functional callback interface to update UI controllers across thread states.
     */
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    /**
     * Thread-safe Singleton accessor that initializes the core OkHttpClient network pipeline.
     */
    public static synchronized OkHttpClient getClient() {
        if (client == null) {

            CookieJar sharedCookieJar = new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<>();
                }
            };

            refreshHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .cookieJar(sharedCookieJar)
                    .build();

            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(sharedCookieJar)

                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = TokenManager.getAccessToken();

                        if (token == null || original.url().toString().contains("/auth/")) {
                            return chain.proceed(original);
                        }

                        Request authorizedRequest = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .header("Accept", "application/json")
                                .build();
                        return chain.proceed(authorizedRequest);
                    })

                    .authenticator((route, response) -> {
                        if (response.priorResponse() != null) {
                            return null;
                        }

                        synchronized (AuthService.class) {
                            if (refreshTokens()) {
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + TokenManager.getAccessToken())
                                        .build();
                            }
                        }
                        return null;
                    })
                    .build();
        }
        return client;
    }

    /**
     * Asynchronously authenticates user credentials against the secure backend portal.
     */
    public void login(String username, String password, AuthCallback callback) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("userName", username);
        credentials.put("password", password);
        String jsonPayload = gson.toJson(credentials);

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, jsonPayload);

        Request request = new Request.Builder()
                .url(ApiConfig.getBaseUrl() + "/auth/login")
                .post(body)
                .build();

        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Connection failed. Is the server running?");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        JsonObject jsonObject = JsonDataParser.parse(responseBody.string());
                        String token = extractTokenFromPayload(jsonObject);

                        if (token != null && !token.isEmpty()) {
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Server sent an empty authentication packet.");
                        }
                    } else {
                        if (response.code() == 401 || response.code() == 403) {
                            callback.onError("Invalid username or password.");
                        } else {
                            callback.onError("Server Error: Status code " + response.code());
                        }
                    }
                } catch (Exception e) {
                    callback.onError("Failed to interpret secure authentication protocol.");
                }
            }
        });
    }

    /**
     * Asynchronously logs out the user, invalidating backend sessions and clearing local memory caches.
     */
    public void logout(AuthCallback callback) {
        // Create an empty body structure for the logout POST call
        RequestBody emptyBody = RequestBody.create("", MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(ApiConfig.getBaseUrl() + "/auth/logout")
                .post(emptyBody)
                .build();

        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Even if network drops, force local session purge for safety
                clearLocalSessionData();
                callback.onError("Server unreachable, but local session cleared safely.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Clear state memory regardless of what status code the server emits
                clearLocalSessionData();

                if (response.isSuccessful()) {
                    callback.onSuccess("Logged out successfully.");
                } else {
                    callback.onError("Session terminated with status code: " + response.code());
                }
            }
        });
    }

    /**
     * Resets internal token storage and wipes out stateful memory cookie instances.
     */
    private void clearLocalSessionData() {
        // 1. Wipe cached bearer token string inside TokenManager memory map
        TokenManager.setAccessToken(null);

        // 2. Erase memory persistent cookie tables
        cookieStore.clear();

        System.out.println("🔒 Local secure session credentials destroyed successfully.");
    }

    /**
     * Completely reflectionless refresh mechanism matching our modular engineering goals.
     */
    private static boolean refreshTokens() {
        RequestBody emptyBody = RequestBody.create("", MediaType.parse("application/json; charset=utf-8"));

        Request refreshRequest = new Request.Builder()
                .url(ApiConfig.getBaseUrl() + "/auth/refresh")
                .post(emptyBody)
                .build();

        try (Response response = refreshHttpClient.newCall(refreshRequest).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject jsonObject = JsonDataParser.parse(response.body().string());
                String freshToken = extractTokenFromPayload(jsonObject);

                if (freshToken != null && !freshToken.isEmpty()) {
                    TokenManager.setAccessToken(freshToken);
                    System.out.println("Access token rotated successfully via silent background refresh.");
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Token refresh call failed down network pipeline: " + e.getMessage());
        }
        return false;
    }

    private static String extractTokenFromPayload(JsonObject jsonObject) {
        if (jsonObject == null) return null;

        if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
            JsonObject dataObject = jsonObject.getAsJsonObject("data");
            return JsonDataParser.getString(dataObject, "accessToken", null);
        }
        return JsonDataParser.getString(jsonObject, "accessToken", null);
    }
}