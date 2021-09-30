package com.example.smartpantry;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
/**
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
        Utils {
            sessionToken - Token given to the user once s/he requires the products
        }

        *ORDERING PREFERENCES*
        ListsOrder {
            flow [ASC, DESC]
            order [DBHelper.COLUMN_PRODUCT_*] where * is a choosen column to sort by
            tempOrder
            tempFlow
        }
**/

public class Global {
    static final String DEFAULT_ICON = "shopping_basket.png";
    static final String ICON_DIRNAME = "groceries_icons";

    static final String DB_DATE_FORMAT = "dd/MM/yyyy";

    //TODO: FOR DEBUG PURPOSE ONLY, CHANGE TO 6
    static final int LOGIN_TOKEN_VALID_DAYS = 1;

    static final String LOGIN_URL = "https://lam21.modron.network/auth/login";
    static final String REGISTER_URL = "https://lam21.modron.network/users";

    static final String LIST_PRODUCTS_URL = "https://lam21.modron.network/products?barcode=";
    static final String ADD_PRODUCT_URL = "https://lam21.modron.network/products";
    static final String VOTE_PRODUCT_URL = "https://lam21.modron.network/votes";


    static final String DESC_ORDER = "DESC";
    static final String ASC_ORDER = "ASC";

    //SORTING SHARED PREFERENCES STRUCTURE
    static final String LISTS_ORDER = "ListsOrder";
        static final String ORDER = "order";
        static final String FLOW = "flow";
        static final String TEMP_ORDER = "tempOrder";
        static final String TEMP_FLOW = "tempFlow";

    //LOGIN SHARED PREFERENCES STRUCTURE
    static final String LOGIN = "Login";
        static final String STAY_LOGGED = "stayLogged";
        static final String ACCESS_TOKEN = "accessToken";
        static final String CURRENT_SESSION = "currentSession";
        static final String VALID_DATE = "validDate";

    //USER DATA SHARED PREFERENCES STRUCTURE
    static final String USER_DATA = "UserData";
        static final String EMAIL = "email";
        static final String USERNAME = "username";
        static final String ID = "id";

    //UTILITY SHARED PREFERENCES STRUCTURE
    static final String UTILITY = "Utils";
        static final String SESSION_TOKEN = "sessionToken";


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
            e.printStackTrace();
        }
        return formattedDate;
    }
}

