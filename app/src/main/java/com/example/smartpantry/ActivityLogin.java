package com.example.smartpantry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ActivityLogin extends AppCompatActivity {
    private CheckBox rememberCheckBox;
    private TextView passwordField;
    private TextView emailField;

    private final String loginURL = Global.LOGIN_URL;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.println(Log.ASSERT,"LOGIN ACTIVITY", "INSTANCE CREATED");

        Button loginBtn = findViewById(R.id.loginBtn);
        rememberCheckBox = findViewById(R.id.rememberCheckBox);
        passwordField = findViewById(R.id.passwordField);
        emailField = findViewById(R.id.emailField);

        loginBtn.setOnClickListener(v -> {
            String passwordString = passwordField.getText().toString();
            String emailString = emailField.getText().toString();
            if (passwordString.trim().isEmpty()) {
                passwordField.setError(getResources().getString(R.string.passwordMissingErrorText));
            }
            if (emailString.trim().isEmpty()) {
                emailField.setError(getResources().getString(R.string.emailMissingErrorText));
            }
            if (!passwordString.trim().isEmpty() && !emailString.trim().isEmpty()) {
                findViewById(R.id.errorDisplay).setVisibility(View.INVISIBLE);
                authenticateUser();
            }
        });
        Button signBtn = findViewById(R.id.registerBtn);
        signBtn.setOnClickListener(v -> {
            if(Global.checkConnectionAvailability(getApplicationContext())) {
                Intent sign = new Intent(this, ActivityRegister.class);
                startActivity(sign);
                this.finish();
            } else {
                Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void authenticateUser() {
        if (Global.checkConnectionAvailability(getApplicationContext())) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, loginURL,
                    response -> {
                        try {
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get(Global.ACCESS_TOKEN).toString();
                            verifyUserIsTheSameOne(
                                    emailField.getText().toString(),
                                    token,
                                    passwordField.getText().toString(),
                                    rememberCheckBox.isChecked());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        findViewById(R.id.errorDisplay).setVisibility(View.VISIBLE);
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    //password sent plain-text, lol
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("password", passwordField.getText().toString());
                    params.put(Global.EMAIL, emailField.getText().toString());
                    return params;
                }
            };
            queue.add(loginRequest);
        } else {
            Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
        }
    }

    private void verifyUserIsTheSameOne(String email, String token, String password, boolean remember) {
        String savedEmail = getApplicationContext()
                .getSharedPreferences(Global.USER_DATA, MODE_PRIVATE)
                .getString(Global.EMAIL, null);
        if(!email.equals(savedEmail) && savedEmail != null){
            askToDropTables(token, password, email, remember);
        } else {
            login(token, password, email, remember);
        }
    }

    private void askToDropTables(String token, String password, String email, boolean remember) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle(getResources().getString(R.string.warningText));
        builder.setMessage(getResources().getString(R.string.dropTablesText))
                .setPositiveButton(
                        getResources().getString(R.string.yesText),
                        (dialog, id) -> {
                            login(token, password, email, remember);
                            //TODO DELETE ALL SHARED PREFERENCES
                            // deleteSharedPreferences();
                            DBHelper db = new DBHelper(getApplicationContext());
                            db.dropAllTables();
                            db.close();
                            this.finish();
                        })
                .setNegativeButton(
                        getResources().getString(R.string.noText),
                        (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.setOnShowListener(arg0 -> {
            alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.button_confirm));

            alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.app_color));
        });
        alert.show();
    }



    private void login (String token, String password, String email, boolean remember) {
        SharedPreferences.Editor loginEdit = getApplicationContext()
                .getSharedPreferences(Global.LOGIN, MODE_PRIVATE).edit();

        //Edit Login SharedPrefs.
        loginEdit.putBoolean(Global.STAY_LOGGED, remember);
        loginEdit.putString(Global.ACCESS_TOKEN, token);
        loginEdit.putBoolean(Global.CURRENT_SESSION, true);
        loginEdit.putString(Global.VALID_DATE, Global.getNewValidDate());
        loginEdit.commit();

        //Edit UserData SharedPrefs.
        SharedPreferences.Editor userDataEdit = getApplicationContext()
                .getSharedPreferences(Global.USER_DATA, MODE_PRIVATE)
                .edit();
        userDataEdit.putString(Global.EMAIL, email);
        userDataEdit.putString("password", password);
        userDataEdit.commit();

        Intent main = new Intent(this, ActivityMain.class);
        startActivity(main);
        this.finish();
    }


}