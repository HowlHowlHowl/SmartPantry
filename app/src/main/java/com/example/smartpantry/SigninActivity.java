package com.example.smartpantry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SigninActivity extends AppCompatActivity {
    private final String registerURL = "https://lam21.modron.network/users";

    private EditText usernameField;
    private EditText emailField;
    private EditText passwordField;
    private EditText confirmPasswordField;
    private Button registerBtn;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField  = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerBtn = findViewById(R.id.signinBtn);

        registerBtn.setOnClickListener(v -> {
            if(checkFields()) {
                registerUser();
            }
        });
    }
    public boolean checkFields() {
        boolean fieldsOk = true;
        if (usernameField.getText().toString().trim().isEmpty()) {
            usernameField.setError(getResources().getString(R.string.usernameRegistrationError));
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "username empty");
        }
        if (emailField.getText().toString().trim().isEmpty()) {
            emailField.setError(getResources().getString(R.string.emailRegistrationError));
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "email empty");
        }
        if (passwordField.getText().toString().trim().isEmpty()) {
            passwordField.setError(getResources().getString(R.string.passwordRegistrationError));
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "password empty");
        }
        if (confirmPasswordField.getText().toString().trim().isEmpty()) {
            confirmPasswordField.setError(getResources().getString(R.string.confirmPasswordRegistrationError));
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "confirmation password empty");
        }
        if (!passwordField.getText().toString().equals(confirmPasswordField.getText().toString())) {
            confirmPasswordField.setError(getResources().getString(R.string.passwordMatchRegistrationError));
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "Password don't match\n"
                    + passwordField.getText().toString() + "\t"
                    + confirmPasswordField.getText().toString());
        }
        if (!isEmailValid(emailField.getText().toString())) {
            fieldsOk = false;
            Log.println(Log.ASSERT, "REGISTRATION", "Not a valid email");
        }
        return fieldsOk;
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private  void registerUser() {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest loginRequest = new StringRequest(Request.Method.POST, registerURL,
                response -> {
                    try {
                        /*
                        EXAMPLE
                        "id": "ckna223pm00002bn055hp3amj",
                        "username": "stradivarius",
                        "email": "federico.montori2@unibo.it",
                        "password": "mypassword-ENCRYPTED",
                        "createdAt": "2021-04-09T08:36:34.762Z",
                        "updatedAt": "2021-04-09T08:36:34.763Z"
                        */

                        JSONObject credentials = new JSONObject(response);
                        String id = credentials.get("id").toString();
                        String username = credentials.get("username").toString();
                        String email = credentials.get("email").toString();
                        String password_ENC = credentials.get("password").toString();
                        String createdAt = credentials.get("createdAt").toString();
                        String updatedAt = credentials.get("updatedAt").toString();

                        SharedPreferences sp = getApplicationContext().getSharedPreferences("UserData", MODE_PRIVATE);
                        SharedPreferences.Editor Ed = sp.edit();
                        Ed.putString("email", email);
                        Ed.putString("password_ENC", password_ENC);
                        Ed.putString("username", username);
                        Ed.putString("id", id);
                        Ed.putString("createdAt", createdAt);
                        Ed.putString("updatedAt", updatedAt);
                        Ed.commit();
                        Intent login = new Intent(this, LoginActivity.class);
                        startActivity(login);
                        this.finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    error.printStackTrace();
                    if (error.networkResponse.statusCode == 500) {
                        Toast.makeText(this, getResources().getString(R.string.alreadyExistsUserError), Toast.LENGTH_LONG);
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.genericError), Toast.LENGTH_LONG);
                    }

                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", usernameField.getText().toString());
                params.put("email", emailField.getText().toString());
                params.put("password", passwordField.getText().toString());
                return params;
            }
        };
        queue.add(loginRequest);
    }
}
