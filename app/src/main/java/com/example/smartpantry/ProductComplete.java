package com.example.smartpantry;

public class ProductComplete extends ProductPantryItem {
    boolean in_pantry;
    public ProductComplete(String name, String description, String expire_date, String id, String icon,
                           int is_favorite, long quantity, long shopping_qnt, int in_pantry) {
        super(name, description, expire_date, id, icon, is_favorite, quantity, shopping_qnt);
        this.in_pantry = in_pantry == 1;
    }
}
