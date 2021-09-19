package com.example.smartpantry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private CheckBox rememberCheckBox;
    private TextView passwordField;
    private TextView emailField;


    private final int DAYS_FOR_TOKEN_TO_EXPIRE = Global.token_valid_days;

    private final String loginURL = Global.login_url;
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
            if(checkConnectionAvailability()) {
                Intent sign = new Intent(this, RegisterActivity.class);
                startActivity(sign);
                this.finish();
            } else {
                Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void authenticateUser() {
        if (checkConnectionAvailability()) {
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest loginRequest = new StringRequest(Request.Method.POST, loginURL,
                    response -> {
                        try {
                            JSONObject credentials = new JSONObject(response);
                            String token = credentials.get("accessToken").toString();
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
                    params.put("email", emailField.getText().toString());
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
                .getSharedPreferences("Login", MODE_PRIVATE)
                .getString("email", "");

        if(!email.equals(savedEmail)){
            askToDropTables(token, password, email, remember);
        } else {
            login(token, password, email, remember);
        }
    }

    private void askToDropTables(String token, String password, String email, boolean remember) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.warningText));
        builder.setMessage(getResources().getString(R.string.dropTablesText))
                .setPositiveButton(
                        getResources().getString(R.string.yesText),
                        (dialog, id) -> {
                            login(token, password, email, remember);
                            dropAllTables();
                            this.finish();
                        })
                .setNegativeButton(
                        getResources().getString(R.string.noText),
                        (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void dropAllTables() {
        DBHelper db = new DBHelper(getApplicationContext());
        db.dropAllTables();
        db.close();
    }

    private void login (String token, String password, String email, boolean remember) {
        SharedPreferences.Editor loginEdit = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE).edit();

        //Edit Login SharedPrefs.
        //REMOVED EMAIL AND PWD
        loginEdit.putBoolean("stayLogged", remember);
        loginEdit.putString("accessToken", token);
        loginEdit.putBoolean("currentSession", true);
        loginEdit.putString("validDate", getNewValidDate());
        loginEdit.commit();

        //Edit UserData SharedPrefs.
        SharedPreferences.Editor userDataEdit = getApplicationContext().getSharedPreferences("UserData", MODE_PRIVATE).edit();
        userDataEdit.putString("email", email);
        userDataEdit.putString("password", password);
        userDataEdit.commit();

        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        this.finish();
    }

    public boolean checkConnectionAvailability() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private String getNewValidDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar c = Calendar.getInstance();
        String today = sdf.format(c.getTime());
        String validDate = today;
        try {
            c.setTime(sdf.parse(today));
            c.add(Calendar.DATE, DAYS_FOR_TOKEN_TO_EXPIRE);
            validDate = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validDate;
    }

}