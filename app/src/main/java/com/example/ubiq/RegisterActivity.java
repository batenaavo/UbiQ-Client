package com.example.ubiq;

import androidx.appcompat.app.AppCompatActivity;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONObject;


//Atividade onde se efetua o registo e login.

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
                startActivity(new Intent(getApplicationContext(), SplashActivity.class));
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

    //procede à autenticação por parte do Spotify
    public void authenticateSpotifyClient(){
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(preferences.getSpotifyClientId(), AuthenticationResponse.Type.TOKEN, preferences.getSpotifyRedirectUri());

        builder.setScopes(new String[]{"app-remote-control", "user-read-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, preferences.getRequestCode(), request);
    }

    //resultado da chamada do método anterior
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == preferences.getRequestCode()) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    preferences.setSpotifyAccessToken(response.getAccessToken());
                    preferences.setSpotifyTokenTime(System.currentTimeMillis());
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                    finish();
                    break;

                // Auth flow returned an error
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.spotify_auth_error);
                    System.out.println("error: " + response.toString());
                    break;
                // Most likely auth flow was cancelled
                default:
                    progressBar.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.spotify_auth_error);
                    System.out.println(response.toString());
            }
        }
    }

    //Envia pedido de registo ao servidor e pede um token de acesso se tiver sucesso
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
                if(error instanceof NoConnectionError){
                    errorText.setText(R.string.no_connection_err);
                    errorText.setVisibility(View.VISIBLE);
                }
                else if (error instanceof TimeoutError) {
                    sendGetTokenRequest(username, password);
                } else if (error instanceof AuthFailureError) {
                    errorText.setText(R.string.auth_failure_err);
                    errorText.setVisibility(View.VISIBLE);
                } else if (error instanceof ServerError) {
                    errorText.setText(new ServerErrorHandler().getErrorArray(error).get(0));
                    errorText.setVisibility(View.VISIBLE);
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

    //envia pedido de token de acesso ao servidor
    //Se tiver sucesso guarda os dados de utilizador em cache e passa para a SplashActivity
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
                        preferences.setUsername(username.toLowerCase());
                        preferences.setPassword(password);
                        preferences.createLogin();
                        setAPItoken(response);
                        if((preferences.getSpotifyAccessToken() == null ||
                                System.currentTimeMillis() - preferences.getSpotifyTokenTime() > 3500000)){
                            authenticateSpotifyClient();
                        }
                        else {
                            startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                            finish();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof NoConnectionError){
                    errorText.setText(R.string.no_connection_err);
                    errorText.setVisibility(View.VISIBLE);
                }
                else if (error instanceof TimeoutError) {
                    sendGetTokenRequest(username, password);
                } else if (error instanceof AuthFailureError) {
                    errorText.setText(R.string.auth_failure_err);
                    errorText.setVisibility(View.VISIBLE);
                } else if (error instanceof ServerError) {
                    errorText.setText(new ServerErrorHandler().getRegisterErrorString(error));
                    errorText.setVisibility(View.VISIBLE);
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




}