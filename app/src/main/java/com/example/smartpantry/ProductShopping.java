package com.example.smartpantry;

public class ProductShopping {
    String name, description, id, icon;
    long to_buy_qnt;
    long quantity;
    Boolean updateValue;
    public ProductShopping(String name, String description, String id, String icon,
                           long to_buy_qnt, long quantity) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.icon = icon;
        this.quantity = quantity;
        this.to_buy_qnt = to_buy_qnt;
    }
}
