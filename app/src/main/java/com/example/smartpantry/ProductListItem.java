package com.example.smartpantry;

public class ProductListItem {
    String id;
    String userId;
    String name;
    String description;
    String barcode;
    String createdAt;
    String updatedAt;
    boolean test;

    public ProductListItem(String id, String name, String description, String barcode,
                           String userId, String createdAt, String updatedAt,
                           boolean test) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.barcode = barcode;
        this.userId = userId;
        this.test = test;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
