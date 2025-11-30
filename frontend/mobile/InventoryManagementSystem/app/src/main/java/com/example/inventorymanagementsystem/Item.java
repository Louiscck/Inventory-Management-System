package com.example.inventorymanagementsystem;

public class Item {
    private int id;
    private String name;
    private String category;
    private String specification;
    private String unit;
    private int amount;

    public Item (int id, String name, String category, String specification, String unit, int amount){
        this.id = id;
        this.name = name;
        this.category = category;
        this.specification = specification;
        this.unit = unit;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getSpecification() {
        return specification;
    }

    public String getUnit() {
        return unit;
    }

    public int getAmount() {
        return amount;
    }
}
