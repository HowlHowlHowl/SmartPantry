package com.example.smartpantry;

public class ProductAlreadySaved {
    protected String name;
    protected String icon;
    protected String expire_date;
    protected boolean in_pantry;
    protected long quantity;
    protected int position;

    public ProductAlreadySaved(String name, String icon, String expire_date,
                               int in_pantry, long quantity, int position) {
        this.name = name;
        this.icon = icon;
        this.expire_date = expire_date;
        this.in_pantry = in_pantry == 1;
        this.quantity = quantity;
        this.position = position;
    }
}
