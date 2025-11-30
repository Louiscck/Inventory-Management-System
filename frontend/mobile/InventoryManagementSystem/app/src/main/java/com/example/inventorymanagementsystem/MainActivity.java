package com.example.inventorymanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pingServer();
        findViewById(R.id.addItemBtn).setOnClickListener(v-> generateAddItemLayout());
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        ItemAdapter adapter = new ItemAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        findViewById(R.id.searchItemBtn).setOnClickListener(v-> getItem(adapter));
    }

    private void pingServer(){
        //backend server is hosted on Render, which sleeps during inactivity, takes about 1 min to wake up
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(3, TimeUnit.MINUTES)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://inventory-management-system-znti.onrender.com")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ItemApiService apiService = retrofit.create(ItemApiService.class);

        TextView text = findViewById(R.id.serverStatusInfoTxt);
        text.setText(R.string.server_status_wakeup);
        apiService.pingServer().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 204) {
                    text.setText(R.string.server_status_live);
                } else {
                    text.setText("Unexpected error");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                text.setText(R.string.server_status_error);
            }
        });
    }

    private void generateAddItemLayout(){
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View layout = getLayoutInflater().inflate(R.layout.add_item_layout, null);

        EditText nameInput = layout.findViewById(R.id.inputName);
        EditText categoryInput = layout.findViewById(R.id.inputCategory);
        EditText specInput = layout.findViewById(R.id.inputSpecification);
        EditText unitInput = layout.findViewById(R.id.inputUnit);
        EditText amountInput = layout.findViewById(R.id.inputAmount);

        Button submitBtn = layout.findViewById(R.id.btnSubmit);
        Button cancelBtn = layout.findViewById(R.id.btnCancel);

        submitBtn.setOnClickListener(v -> {

            String name = nameInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();
            String spec = specInput.getText().toString().trim();
            String unit = unitInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();

            // Validation
            if (name.isEmpty()) { nameInput.setError("Required"); return; }
            if (category.isEmpty()) { categoryInput.setError("Required"); return; }
            if (spec.isEmpty()) { specInput.setError("Required"); return; }
            if (unit.isEmpty()) { unitInput.setError("Required"); return; }
            if (amountStr.isEmpty()) { amountInput.setError("Required"); return; }

            int amount;
            try {
                amount = Integer.parseInt(amountStr);
                if (amount <= 0) {
                    amountInput.setError("Must be positive");
                    return;
                }
            } catch (NumberFormatException e) {
                amountInput.setError("Invalid number");
                return;
            }

            Item item = new Item(0, name, category, spec, unit, amount);
            addItem(item);

            bottomSheet.dismiss();
        });

        cancelBtn.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.setContentView(layout);
        bottomSheet.show();
    }

    private void addItem(Item item){
        ItemApiService apiService = ItemApiClient.getApiService();

        Call<ResponseBody> call = apiService.createItem(item);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.code() == 201) {
                    Toast.makeText(MainActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                } else if (response.code() >= 400 && response.code() <= 500){
                    String errorMsg = "Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            Gson gson = new Gson();
                            MessageResponse msgResponse = gson.fromJson(response.errorBody().string(), MessageResponse.class);
                            if (msgResponse != null && msgResponse.getMessage() != null) {
                                errorMsg = msgResponse.getMessage();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getItem(ItemAdapter adapter){
        ItemApiService apiService = ItemApiClient.getApiService();

        String searchText = ((EditText) findViewById(R.id.searchEditTxt)).getText().toString().trim();
        Call<List<Item>> call = apiService.getItems(searchText);

        call.enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Item> fetchedItems = response.body();
                    // 3. Update adapter with new items
                    adapter.updateItems(fetchedItems);
                } else {
                    // Handle server error
                    String errorMsg = "Failed to fetch items: " + response.code();
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                // Handle network failure
                Toast.makeText(MainActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}