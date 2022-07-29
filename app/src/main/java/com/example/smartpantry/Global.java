package com.example.smartpantry;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Global {

    //Request code for camera
    static final int CAMERA_REQUEST_CODE = 1;

    //Activities IDs
    static final int CAMERA_ACTIVITY = 101;
    static final int PRODUCTS_ACTIVITY = 102;
    static final int SHOPPING_ACTIVITY = 103;

    //Fragment Tags
    static final String FRAG_ADD_PROD = "fragmentAddProduct";
    static final String FRAG_ADD_SHOP = "fragmentAddShopping";
    static final String FRAG_ALREADY_SAVED = "fragmentAlreadySaved";
    static final String FRAG_BARCODE_DIALOG = "fragmentBarcodeDialog";
    static final String FRAG_BARCODE_LIST = "fragmentBarcodeList";
    static final String FRAG_FILTERS = "fragmentFilters";
    static final String FRAG_ICON_PICK = "fragmentIconPicker";
    static final String FRAG_MAN_ENTRY = "fragmentManEntry";
    static final String FRAG_NOTIFICATIONS = "fragmentNotificationsManager";
    static final String FRAG_PREVIEW_PROD = "fragmentPreviewProd";
    static final String FRAG_RECIPE = "fragmentRecipe";
    static final String FRAG_BEST_MATCH = "fragmentBestMatch";

    //Status Response Codes for Login check
    static final int LOGIN_STATUS_OK = 200;
    static final int LOGIN_STATUS_SHOW_LOGIN = 201;
    static final int LOGIN_STATUS_REQUEST_TOKEN = 202;

    //Icons files path and default icon file name
    static final String DEFAULT_ICON = "0_shopping_basket.png";
    static final String ICON_DIRNAME = "groceries_icons";

    //Request codes
    static final int REQUEST_CODE_CHECK_EXPIRED = 5000;
    static final int REQUEST_CODE_CHECK_FAVORITES = 5001;

    //Notification EXPIRED_PRODUCTS IDs
    static final int NOTIFICATION_EXPIRED_ID = 6000;
    static final int NOTIFICATION_FAVORITES_ID = 6001;

    //Intent action for showing expired products in main activity
    static final String EXPIRED_INTENT_ACTION = "SHOW_EXPIRED";
    //Intent action for showing favorites products missing in products activity
    static final String FAVORITES_INTENT_ACTION = "SHOW_FAVORITES";

    //Notification Channels ID
    static final String NOTIFICATION_EXP_CHANNEL = "EXPIRING_PRODUCTS";
    static final String NOTIFICATION_FAV_CHANNEL = "FAV_MISSING_PRODUCTS";

    //Date format used in the DB
    static final String DB_DATE_FORMAT = "yyyy-MM-dd";

    // FIXME: FOR DEBUG PURPOSE CHANGE TO 1
    static final int LOGIN_TOKEN_VALID_DAYS = 6;

    //Server APIs URLs
    static final String LOGIN_URL = "https://lam21.iot-prism-lab.cs.unibo.it/auth/login";
    static final String REGISTER_URL = "https://lam21.iot-prism-lab.cs.unibo.it/users";
    static final String USER_ID_URL = "https://lam21.iot-prism-lab.cs.unibo.it/users/me";
    static final String LIST_PRODUCTS_URL = "https://lam21.iot-prism-lab.cs.unibo.it/products?barcode=";
    static final String ADD_PRODUCT_URL = "https://lam21.iot-prism-lab.cs.unibo.it/products";
    static final String DELETE_PRODUCT_URL = "https://lam21.iot-prism-lab.cs.unibo.it/products/";
    static final String VOTE_PRODUCT_URL = "https://lam21.iot-prism-lab.cs.unibo.it/votes";

    //static final String LOCALHOST = "192.168.137.1:8010";
    static final String LOCALHOST = "192.168.1.100:8010";
    static final String RECIPES_URL = "http://"+LOCALHOST+"/recipes/get_avail_recipes/";
    static final String POST_RECIPE_RATING = "http://"+LOCALHOST+"/recipes/rate_recipe/";
    static final String MATCH_PRODUCT_URL = "http://"+LOCALHOST+"/ingredients/get_matching/";
    static final String POST_MATCH_VOTE = "http://"+LOCALHOST+"/ingredients/vote_match/";

    //DESC and ASC strings
    static final String DESC_ORDER = "DESC";
    static final String ASC_ORDER = "ASC";

    //RECIPES TYPE
    static final String RECIPES_PATH = "recipes_icons/";

    //SORTING SHARED PREFERENCES STRUCTURE
    static final String LISTS_ORDER = "ListsOrder";
        static final String ORDER = "order";
        static final String FLOW = "flow";
        static final String TEMP_ORDER = "tempOrder";
        static final String TEMP_FLOW = "tempFlow";
        static final String NOT_IN_PANTRY = "notInPantry";

    //LOGIN SHARED PREFERENCES STRUCTURE
    static final String LOGIN = "Login";
        static final String STAY_LOGGED = "stayLogged";
        static final String ACCESS_TOKEN = "accessToken";
        static final String CURRENT_SESSION = "currentSession";
        static final String VALID_DATE = "validDate";

    //USER DATA SHARED PREFERENCES STRUCTURE
    static final String USER_DATA = "UserData";
        static final String EMAIL = "email";
        static final String PASSWORD = "password";
        static final String USERNAME = "username";
        static final String ID = "id";

    //UTILITY SHARED PREFERENCES STRUCTURE
    static final String UTILITY = "Utils";
        static final String SESSION_TOKEN = "sessionToken";
        static final String NOTIFY_EXPIRED = "notifyExpire";
        static final String NOTIFY_FAVORITES = "notifyFavorites";


    static protected boolean checkConnectionAvailability(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    static protected boolean isDateBeforeToday(String validString) {
        SimpleDateFormat sdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        boolean isDateValid = false;
        try {
            Date validDate = sdf.parse(validString);
            isDateValid = !(new Date().after(validDate) || new Date().equals(validDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return isDateValid;
    }

    static protected String getNewValidDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        Calendar c = Calendar.getInstance();
        String today = sdf.format(c.getTime());
        String validDate = today;
        try {
            c.setTime(sdf.parse(today));
            c.add(Calendar.DATE, LOGIN_TOKEN_VALID_DAYS);
            validDate = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validDate;
    }

    static protected String changeDateFormat(String inputDate, DateFormat originalFormat, DateFormat targetFormat) {
        String formattedDate="";
        try {
            Date FDate = originalFormat.parse(inputDate);
            formattedDate = targetFormat.format(FDate);
        } catch (ParseException e) {
            Log.println(Log.INFO, "DATE FORMAT CHANGING FAILED", "INPUT = '" + inputDate +"'");
        }
        return formattedDate;
    }
}

