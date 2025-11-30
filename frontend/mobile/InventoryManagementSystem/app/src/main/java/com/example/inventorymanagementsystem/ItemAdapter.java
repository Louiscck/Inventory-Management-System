package com.example.inventorymanagementsystem;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> items;
    private Context context;

    public ItemAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
    }

    public void updateItems(List<Item> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);

        holder.itemName.setText(item.getName());
        holder.itemCategory.setText("Category: " + item.getCategory());
        holder.itemSpec.setText("Specification: " + item.getSpecification());
        holder.itemAmount.setText("Amount: " + item.getAmount() + " " + item.getUnit());

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteItem(item, position))
                    .setNegativeButton("No", null)
                    .show();
        });

        holder.btnEdit.setOnClickListener(v -> showEditDialog(item, position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemCategory, itemSpec, itemUnit, itemAmount;
        Button btnEdit, btnDelete;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemCategory = itemView.findViewById(R.id.itemCategory);
            itemSpec = itemView.findViewById(R.id.itemSpecification);
            itemAmount = itemView.findViewById(R.id.itemAmount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void deleteItem(Item item, int position) {
        ItemApiService apiService = ItemApiClient.getApiService();
        apiService.deleteItem(item.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    items.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorJson = response.errorBody().string();
                        MessageResponse msg = new Gson().fromJson(errorJson, MessageResponse.class);
                        Toast.makeText(context, msg.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditDialog(Item item, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.add_item_layout, null);

        EditText nameInput = dialogView.findViewById(R.id.inputName);
        EditText categoryInput = dialogView.findViewById(R.id.inputCategory);
        EditText specInput = dialogView.findViewById(R.id.inputSpecification);
        EditText unitInput = dialogView.findViewById(R.id.inputUnit);
        EditText amountInput = dialogView.findViewById(R.id.inputAmount);

        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set default values
        nameInput.setText(item.getName());
        categoryInput.setText(item.getCategory());
        specInput.setText(item.getSpecification());
        unitInput.setText(item.getUnit());
        amountInput.setText(String.valueOf(item.getAmount()));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Item")
                .setView(dialogView)
                .create();

        dialog.show();

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Submit button
        btnSubmit.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newCategory = categoryInput.getText().toString().trim();
            String newSpec = specInput.getText().toString().trim();
            String newUnit = unitInput.getText().toString().trim();
            String newAmountStr = amountInput.getText().toString().trim();

            //Validation
            if (newName.isEmpty()) { nameInput.setError("Required"); return; }
            if (newCategory.isEmpty()) { categoryInput.setError("Required"); return; }
            if (newSpec.isEmpty()) { specInput.setError("Required"); return; }
            if (newUnit.isEmpty()) { unitInput.setError("Required"); return; }
            if (newAmountStr.isEmpty()) { amountInput.setError("Required"); return; }

            if (newName.equals(item.getName()) &&
                    newCategory.equals(item.getCategory()) &&
                    newSpec.equals(item.getSpecification()) &&
                    newUnit.equals(item.getUnit()) &&
                    newAmountStr.equals(String.valueOf(item.getAmount()))) {
                Toast.makeText(context, "No changes detected", Toast.LENGTH_SHORT).show();
                return;
            }

            int newAmount;
            try {
                newAmount = Integer.parseInt(newAmountStr);
                if (newAmount <= 0) {
                    amountInput.setError("Must be positive");
                    return;
                }
            } catch (NumberFormatException e) {
                amountInput.setError("Invalid number");
                return;
            }

            Item updatedItem = new Item(item.getId(), newName, newCategory, newSpec, newUnit, newAmount);
            submitEdit(updatedItem, position, dialog);
        });
    }

    private void submitEdit(Item updatedItem, int position, AlertDialog dialog) {
        ItemApiService apiService = ItemApiClient.getApiService();
        apiService.updateItem(updatedItem.getId(), updatedItem).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                int code = response.code();
                if (code == 204) {
                    // Success, no body returned
                    Toast.makeText(context, "Item updated Successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else if (code>=400 && code <=500) {
                    try {
                        String errorJson = response.errorBody().string();
                        MessageResponse msg = new Gson().fromJson(errorJson, MessageResponse.class);
                        Toast.makeText(context, msg.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}