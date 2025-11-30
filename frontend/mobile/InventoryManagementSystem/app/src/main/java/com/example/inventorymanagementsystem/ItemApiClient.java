package com.example.inventorymanagementsystem;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ItemApiClient {
    private static final String BASE_URL = "https://inventory-management-system-znti.onrender.com";
    private static Retrofit retrofit = null;

    public static ItemApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ItemApiService.class);
    }
}
