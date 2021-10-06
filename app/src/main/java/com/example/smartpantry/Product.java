package com.example.smartpantry;

public class Product extends ProductPantryItem {
    boolean in_pantry;
    public Product(String name, String description, String expire_date, String id, String icon,
                   int is_favorite, int quantity, int in_pantry) {
        super(name, description,expire_date,id,icon, is_favorite, quantity);
        this.in_pantry = in_pantry == 1;
    }
}
