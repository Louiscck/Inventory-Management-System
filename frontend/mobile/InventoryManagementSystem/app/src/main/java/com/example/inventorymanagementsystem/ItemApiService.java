package com.example.inventorymanagementsystem;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ItemApiService {
    @GET("item")
    Call<List<Item>> getItems(@Query("search") String search);

    @POST("item")
    Call<ResponseBody> createItem(@Body Item item);

    @PUT("item/{id}")
    Call<ResponseBody> updateItem(@Path("id") int id, @Body Item item);

    // Delete an item by ID
    @DELETE("item/{id}")
    Call<ResponseBody> deleteItem(@Path("id") int id);

    @GET("ping")
    Call<Void> pingServer();
}
