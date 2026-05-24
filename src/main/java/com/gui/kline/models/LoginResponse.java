package com.gui.kline.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName(value = "accessToken", alternate = {"access_token", "token"})
    private String accessToken;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}