package com.example.smartpantry;

public class ProductPantryItem {
    String name;
    String description;
    String id;
    String expire_date;
    String icon;
    boolean is_favorite;
    long quantity;
    long shopping_qnt;
    ProductPantryItem(String name, String description, String expire_date, String id, String icon,
                      int is_favorite, long quantity, long shopping_qnt) {
        this.name = name;
        this.expire_date = expire_date;
        this.id = id;
        this.icon = icon;
        this.is_favorite = is_favorite == 1;
        this.quantity = quantity;
        this.description = description;
        this.shopping_qnt = shopping_qnt;

    }
}

