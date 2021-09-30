package com.example.smartpantry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

public class ActivityRegister extends AppCompatActivity {
    private final String REGISTER_URL = Global.REGISTER_URL;

    private EditText usernameField;
    private EditText emailField;
    private EditText passwordField;
    private EditText confirmPasswordField;
    private Toast toast = null;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField  = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);

        //Register user button
        findViewById(R.id.registerBtn).setOnClickListener(v -> {
            //IT WOULD BE GREAT TO HASH THE PASSWORD BUT TO DO SO
            // WE NEED SERVER SIDE MODIFICATIONS TO IMPLEMENT SALT MECHANISMS AND CONSISTENT HASHING
            if(checkFields()) {
                registerUser();
            }
        });

        //Already registered user, sign in button
        findViewById(R.id.alreadyRegistered).setOnClickListener(v->{
            Intent login = new Intent(this, ActivityLogin.class);
            startActivity(login);
            this.finish();
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
            emailField.setError(getResources().getString(R.string.emailRegistrationError));
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
        StringRequest loginRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    try {
                        JSONObject credentials = new JSONObject(response);

                        SharedPreferences sp = getApplicationContext().getSharedPreferences(Global.USER_DATA, MODE_PRIVATE);
                        SharedPreferences.Editor Ed = sp.edit();
                        Ed.putString(Global.EMAIL, credentials.get(Global.EMAIL).toString());
                        Ed.putString(Global.USERNAME, credentials.get(Global.USERNAME).toString());
                        Ed.putString(Global.ID, credentials.get(Global.ID).toString());
                        Ed.commit();

                        Intent login = new Intent(this, ActivityLogin.class);
                        startActivity(login);
                        this.finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    error.printStackTrace();
                    if (toast!=null && toast.getView().isShown()) {
                        toast.cancel();
                    }
                    if (error.networkResponse.statusCode == 500) {
                        toast = Toast.makeText(this, getResources().getString(R.string.alreadyExistsUserError), Toast.LENGTH_LONG);
                    } else {
                        toast = Toast.makeText(this, getResources().getString(R.string.genericError), Toast.LENGTH_SHORT);
                    }
                    toast.show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(Global.USERNAME, usernameField.getText().toString());
                params.put(Global.EMAIL, emailField.getText().toString());
                params.put("password", passwordField.getText().toString());
                return params;
            }
        };
        queue.add(loginRequest);
    }
}
