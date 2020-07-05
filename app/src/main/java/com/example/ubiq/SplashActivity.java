package com.example.ubiq;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

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

import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private PrefManager preferences;
    private String apiToken;
    private String spotifyToken;
    private RelativeLayout noConnectionLayout;
    private Button reloadBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        this.preferences = new PrefManager(this);
        this.apiToken = preferences.getAPIAccessToken();
        this.spotifyToken = preferences.getSpotifyAccessToken();
        this.noConnectionLayout = findViewById(R.id.no_connection_layout);
        this.reloadBtn = findViewById(R.id.reload_btn);

        reloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noConnectionLayout.setVisibility(View.GONE);
                if(preferences.getSpotifyAccessToken() == null ||
                        System.currentTimeMillis() - preferences.getSpotifyTokenTime() > 3500000){
                    authenticateSpotifyClient();
                } else if(preferences.getSpotifyAccountType() == null) {
                    sendGetUserTypeRequest();
                } else {
                    sendGetQueueIdReq();
                }
            }
        });

        if(!preferences.isLoggedIn() ||
                apiToken == null ||
                System.currentTimeMillis() - preferences.getAPITokenTime() > 1209599999){
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            finish();
        } else if(preferences.getSpotifyAccessToken() == null ||
                System.currentTimeMillis() - preferences.getSpotifyTokenTime() > 3500000){
            authenticateSpotifyClient();
        } else if(preferences.getSpotifyAccountType() == null) {
            sendGetUserTypeRequest();
        } else {
            sendGetQueueIdReq();
        }
    }

    public void authenticateSpotifyClient(){
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(preferences.getSpotifyClientId(), AuthenticationResponse.Type.TOKEN, preferences.getSpotifyRedirectUri());

        builder.setScopes(new String[]{"app-remote-control", "user-read-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, preferences.getRequestCode(), request);
    }

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
                    sendGetUserTypeRequest();
                    break;

                // Auth flow returned an error
                case ERROR:
                    noConnectionLayout.setVisibility(View.VISIBLE);
                    System.out.println("error: " + response.toString());
                    break;
                // Most likely auth flow was cancelled
                default:
                    noConnectionLayout.setVisibility(View.VISIBLE);
                    System.out.println(response.toString());
            }
        }
    }

    private void sendGetUserTypeRequest(){
        String url = "https://api.spotify.com/v1/me";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject obj = new JSONObject(response);
                            String type = obj.getString("product");
                            preferences.setSpotifyAccountType(type);
                            System.out.println("type: " + type);
                            sendGetQueueIdReq();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError)
                    sendGetUserTypeRequest();
                else if (error instanceof AuthFailureError)
                    authenticateSpotifyClient();
                else
                    noConnectionLayout.setVisibility(View.VISIBLE);
                System.out.println(error.toString());
            }
        })
        {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + spotifyToken);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    public void sendGetQueueIdReq(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Info";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject obj = new JSONObject(response);
                            preferences.setQueueId(obj.getInt("Id"));
                            preferences.setQueueName(obj.getString("Nome"));
                            if(obj.getBoolean("isOwner")){
                                preferences.setUserType("host");
                            }
                            else preferences.setUserType("guest");
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    sendGetQueueIdReq();
                } else if(error instanceof NoConnectionError){
                    noConnectionLayout.setVisibility(View.VISIBLE);
                } else if (error instanceof ServerError) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                } else{
                    startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                    finish();
                }
                System.out.println(error.toString());
            }
        })
        {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + apiToken);
                return headers;
            }
        };
        queue.add(stringRequest);
    }
}
