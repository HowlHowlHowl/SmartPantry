package com.example.smartpantry;

public class Ingredient {
    public String name, quantity, info;
    public boolean avail;
    public Ingredient(String name, String quantity, String info, int avail) {
        this.name = name;
        this.quantity = quantity;
        this.info = info;
        this.avail = avail == 1;
    }

}
