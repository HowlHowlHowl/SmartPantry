package com.example.smartpantry;

public class SuggestionItem {
    protected String name;
    protected String description;
    protected String icon;
    protected int positionInPantry;

    public SuggestionItem(String name, String description, String icon, int positionInPantry) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.positionInPantry = positionInPantry;
    }
}
