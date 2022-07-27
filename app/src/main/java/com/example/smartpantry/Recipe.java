package com.example.smartpantry;

import org.json.JSONArray;

public class Recipe {
    String id;
    String name;
    String main_ingredient;
    String procedure;
    String portions;
    String type;
    String notes;
    Double score;

    JSONArray ingredients;
    public Recipe(String id, String name, String main_ingredient, String procedure, String portions, String type, String notes, Double score, JSONArray ingredients) {
        this.id=id;
        this.name = name;
        this.main_ingredient = main_ingredient;
        this.procedure = procedure;
        this.portions = portions;
        this.type = type;
        this.notes = notes;
        this.score = score;
        this.ingredients = ingredients;
    }



}
