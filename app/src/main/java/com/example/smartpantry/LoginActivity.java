                                    package com.example.smartpantry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
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
    private Button loginBtn;
    private Button signBtn;
    private CheckBox rememberCheckBox;
    private TextView passwordField;
    private TextView emailField;

    private final String loginURL = "https://lam21.modron.network/auth/login";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.loginBtn);
        rememberCheckBox = findViewById(R.id.rememberCheckBox);
        passwordField = findViewById(R.id.passwordField);
        emailField = findViewById(R.id.emailField);

        loginBtn.setOnClickListener(v -> {
            boolean passwordEmpty = passwordField.getText().toString().isEmpty();
            boolean emailEmpty = emailField.getText().toString().isEmpty();
            if (passwordEmpty) {
                passwordField.setError(getResources().getString(R.string.passwordMissingErrorText));
            }
            if (emailEmpty) {
                emailField.setError(getResources().getString(R.string.emailMissingErrorText));
            }
            if (!passwordEmpty && !emailEmpty) {
                getToken();
            }
        });
        signBtn = findViewById(R.id.signinBtn);
        signBtn.setOnClickListener(v -> {
            Intent sign = new Intent(this, SigninActivity.class);
            startActivity(sign);
        });
    }

    public void getToken() {
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
                    error -> error.printStackTrace()) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("email", passwordField.getText().toString());
                    params.put("password", emailField.getText().toString());
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
            Ed.putBoolean("userLogged", true);
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
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private String getNewValidDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar c = Calendar.getInstance();
        String today = sdf.format(c.getTime());
        String validDate = today;
        try {
            c.setTime(sdf.parse(today));
            c.add(Calendar.DATE, 6);
            validDate = sdf.format(c.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return validDate;
    }

}