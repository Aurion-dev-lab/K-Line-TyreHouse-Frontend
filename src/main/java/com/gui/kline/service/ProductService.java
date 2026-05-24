package com.gui.kline.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gui.kline.models.Product;
import com.gui.kline.utils.ApiConfig;
import com.gui.kline.utils.JsonDataParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import okhttp3.*;

import java.io.IOException;

public class ProductService {

    private static final String PRODUCTS_API_URL = ApiConfig.getBaseUrl() + "/products";

    /**
     * Functional Callback interface to pass mapped data safely back to UI layers.
     */
    public interface ProductCallback {
        void onSuccess(ObservableList<Product> products);
        void onError(String errorMessage);
    }

    /**
     * Asynchronously fetches products from the backend API.
     */
    public void getAllProducts(ProductCallback callback) {
        Request request = new Request.Builder()
                .url(PRODUCTS_API_URL)
                .get()
                .build();

        AuthService.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Connection failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        callback.onError("Server returned status code: " + response.code());
                        return;
                    }

                    String rawJson = responseBody.string();
                    JsonObject jsonObject = JsonDataParser.parse(rawJson);

                    JsonArray itemsArray = null;
                    if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                        itemsArray = jsonObject.getAsJsonArray("data");
                    } else if (jsonObject.has("products") && jsonObject.get("products").isJsonArray()) {
                        itemsArray = jsonObject.getAsJsonArray("products");
                    }

                    if (itemsArray != null) {
                        ObservableList<Product> fetchedList = FXCollections.observableArrayList();
                        for (JsonElement element : itemsArray) {
                            if (element.isJsonObject()) {
                                JsonObject item = element.getAsJsonObject();

                                String id = JsonDataParser.getString(item, "productId", "PR-UNKNOWN");
                                String name = JsonDataParser.getString(item, "productName", "Unnamed Product");
                                double sellPrice = JsonDataParser.getDouble(item, "sellingPrice", 0.0);
                                double buyPrice = JsonDataParser.getDouble(item, "buyingPrice", 0.0);
                                int stock = JsonDataParser.getInt(item, "quantity", 0);
                                String category = JsonDataParser.getString(item, "category", "Tyres");

                                fetchedList.add(new Product(name, category, buyPrice, sellPrice, stock));
                            }
                        }
                        callback.onSuccess(fetchedList);
                    } else {
                        callback.onError("Invalid response data format.");
                    }
                } catch (Exception e) {
                    callback.onError("Data extraction error: " + e.getMessage());
                }
            }
        });
    }
}