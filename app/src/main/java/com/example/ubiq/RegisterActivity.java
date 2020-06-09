package com.example.ubiq;

import androidx.appcompat.app.AppCompatActivity;
import java.lang.String;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {
    public static final String TAG = "TAG";
    private PrefManager preferences;
    private EditText mEmail, mPassword, mConfirmPassword, mUsername;
    private Button mSubmitButton;
    private TextView changeActionButton, errorText;
    private ProgressBar progressBar;
    private String action = "register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        preferences = new PrefManager(this);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mConfirmPassword = findViewById(R.id.confirm_password);
        mUsername = findViewById(R.id.username);
        mSubmitButton = findViewById(R.id.submit_button);
        changeActionButton = findViewById(R.id.createText);
        errorText = findViewById(R.id.error_text);
        progressBar = findViewById(R.id.progressBar);

        //login automático se o user já estiver registado
        if (preferences.isLoggedIn()) {
            if(System.currentTimeMillis() - preferences.getAPITokenTime() < 1209599999){
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                finish();
            }
            else
                sendGetTokenRequest(preferences.getUsername(), preferences.getPassword());
        }

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();
                final String confirmPassword = mConfirmPassword.getText().toString().trim();
                final String username = mUsername.getText().toString();

                if (action.equals("register") && TextUtils.isEmpty(email)) {
                    errorText.setText("Email is required.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                if (TextUtils.isEmpty(username)) {
                    errorText.setText("Username is required.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    errorText.setText("Password is required.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                if (action.equals("register") && TextUtils.isEmpty(confirmPassword)) {
                    errorText.setText("Password confirmation is required.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }
                else if (action.equals("register") && !confirmPassword.equals(password)) {
                    errorText.setText("Passwords do not match.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                if (action.equals("register") && password.length() < 8) {
                    errorText.setText("Password must have at least 8 characters and contain at least one digit and one uppercase letter.");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                if(action.equals("register"))
                    sendPostRegisterRequest(email, username, password);
                else sendGetTokenRequest(username, password);
            }
        });

        changeActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(action.equals("register")) {
                    mEmail.setVisibility(View.GONE);
                    mConfirmPassword.setVisibility(View.GONE);
                    changeActionButton.setText(R.string.register_text);
                    mSubmitButton.setText("Login");
                    action = "login";
                }
                else{
                    mEmail.setVisibility(View.VISIBLE);
                    mConfirmPassword.setVisibility(View.VISIBLE);
                    changeActionButton.setText(R.string.login_text);
                    mSubmitButton.setText("Register");
                    action = "register";
                }
                errorText.setVisibility(View.GONE);
            }
        });

    }

    private void sendPostRegisterRequest(String email, String username, String password) {
        errorText.setVisibility(View.GONE);
        String url = "https://ubiq.azurewebsites.net/api/Account/Register";

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        sendGetTokenRequest(username, password);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse errorRes = error.networkResponse;
                String stringData = "";
                if(errorRes != null && errorRes.data != null){
                    try {
                        stringData = new String(errorRes.data, "UTF-8");
                        errorText.setText(getRegisterErrorDetails(stringData).get(0));
                        errorText.setVisibility(View.VISIBLE);
                    } catch(Exception e){e.printStackTrace();}
                }
                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                return headers;
            }
            @Override
            public Map getParams() throws AuthFailureError {
                HashMap params = new HashMap();
                params.put("Email", email);
                params.put("UserName", username);
                params.put("Password", password);
                params.put("ConfirmPassword", password);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    public void sendGetTokenRequest(String username, String password) {
        errorText.setVisibility(View.GONE);
        String url = "https://ubiq.azurewebsites.net/token";

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        preferences.setUsername(username);
                        preferences.setPassword(password);
                        preferences.createLogin();
                        setAPItoken(response);
                        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse errorRes = error.networkResponse;
                String stringData = "";
                if(errorRes != null && errorRes.data != null){
                    try {
                        stringData = new String(errorRes.data, "UTF-8");
                        errorText.setText(getTokenErrorDetails(stringData));
                        errorText.setVisibility(View.VISIBLE);
                    } catch(Exception e){e.printStackTrace();}
                }
                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
            @Override
            public Map getParams() throws AuthFailureError {
                HashMap params = new HashMap();
                params.put("grant_type", "password");
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    public void setAPItoken(String response){
        try{
            JSONObject obj = new JSONObject(response);
            String token = obj.getString("access_token");
            preferences.setAPIAccessToken(token);
            preferences.setAPITokenTime(System.currentTimeMillis());
        }
        catch (Exception e){e.printStackTrace();}
    }

    public ArrayList<String> getRegisterErrorDetails(String error){
        ArrayList<String> errors = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(error).getJSONObject("ModelState");
            JSONArray array = obj.getJSONArray("");
            for(int i = 0; i < array.length(); i++)
                errors.add(array.getString(i));
        }catch (Exception e){e.printStackTrace();}
        return errors;
    }

    public String getTokenErrorDetails(String error){
        String errorStr = "";
        try {
            JSONObject obj = new JSONObject(error);
            errorStr = obj.getString("error_description");
        }catch (Exception e){e.printStackTrace();}
        return errorStr;
    }
}