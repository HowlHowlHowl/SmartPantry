package com.example.smartpantry;

public class ProductPantryItem {
    String name;
    String id;
    String expire_date;
    boolean is_favorite;
    int quantity;
    ProductPantryItem(String name, String expire_date, String id,
                      int is_favorite, int quantity) {
        this.name = name;
        this.expire_date = expire_date;
        this.id = id;
        this.is_favorite = is_favorite == 1;
        this.quantity = quantity;

    }
}

