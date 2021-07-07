package com.example.smartpantry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private CheckBox rememberCheckBox;
    private TextView passwordField;
    private TextView emailField;


    //TODO: FOR DEBUG PURPOSE ONLY, CHANGE TO 6
    private final int DAYS_FOR_TOKEN_EXPIRE = 1;
    private final String loginURL = "https://lam21.modron.network/auth/login";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.println(Log.ASSERT,"LOGIN", "ISTANCE CREATED");
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
                authenticateUser();
            }
        });
        Button signBtn = findViewById(R.id.signinBtn);
        signBtn.setOnClickListener(v -> {
            if(checkConnectionAvailability()) {
                Intent sign = new Intent(this, SigninActivity.class);
                startActivity(sign);
            } else {
                Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG);
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
                            login(token,
                                  passwordField.getText().toString(),
                                  emailField.getText().toString(),
                                  rememberCheckBox.isChecked()
                            );
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, getResources().getString(R.string.loginError), Toast.LENGTH_LONG);
                        findViewById(R.id.errorDisplay).setVisibility(View.VISIBLE);
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("password", passwordField.getText().toString());
                    params.put("email", emailField.getText().toString());
                    return params;
                }
            };
            queue.add(loginRequest);
        } else {
            Toast.makeText(this, getResources().getString(R.string.connectionError), Toast.LENGTH_LONG);
        }
    }

    private void login (String token, String password, String email, boolean remember) {
            SharedPreferences sp = getApplicationContext().getSharedPreferences("Login", MODE_PRIVATE);
            SharedPreferences.Editor Ed = sp.edit();
            Ed.putString("email", email);
            Ed.putString("password", password);
            Ed.putBoolean("stayLogged", remember);
            Ed.putString("accessToken", token);
            Ed.putBoolean("activeSession", true);
            Ed.putString("validDate", getNewValidDate());
            Ed.commit();
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            this.finish();
    }

    public boolean checkConnectionAvailability() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
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
            c.add(Calendar.DATE, DAYS_FOR_TOKEN_EXPIRE);
            validDate = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validDate;
    }

}