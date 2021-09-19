package com.example.smartpantry;

public class Global {
    public static String default_icon = "shopping_basket.png";
    public static String icon_dirname = "groceries_icons";

    //TODO: FOR DEBUG PURPOSE ONLY, CHANGE TO 6
    public static int token_valid_days = 1;

    static final String login_url = "https://lam21.modron.network/auth/login";
    static final String register_url = "https://lam21.modron.network/users";

    static final String list_products_url = "https://lam21.modron.network/products?barcode=";
    static final String add_product_url = "https://lam21.modron.network/products";
    static final String vote_product_url = "https://lam21.modron.network/votes";


    /*
    SHARED PREFERENCES:
        *LOGIN FIELDS* - CHECK  IF NEEDED
        Login {
            stayLogged  - User asked to stay logged in
            accessToken - Access Token for the current session
            currentSession - User just logged in
            validDate - Date until accessToken is valid
        }
        *USER DATA FIELDS*
        UserData {
            email
            password_ENC - Encrypted user password by server: NEVER USED
            username
            id
        }
        *UTILITY*
        Utils
            sessionToken - Token given to the user once he require the products
     */
}

