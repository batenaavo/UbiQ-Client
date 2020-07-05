package com.example.ubiq;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "UbiQ";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String QUEUE_ID = "queue_id";
    private static final String USER_TYPE = "user_type";
    private static final String QUEUE_NAME = "queue_name";
    private static final String LOCATION_ACCESS = "location_access";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String SPOTIFY_ACCESS_TOKEN = "spotify_token";
    private static final String API_ACCESS_TOKEN = "api_token";
    private static final String API_TOKEN_TIME = "api_token_time";
    private static final String REQUEST_CODE = "request_code";
    private static final String SPOTIFY_CLIENT_ID = "client_id";
    private static final String SPOTIFY_REDIRECT_URI = "redirect_uri";
    private static final String SPOTIFY_TOKEN_TIME = "spotify_token_time";
    private static final String SPOTIFY_USER_TYPE = "spotify_user_type";

    //initializing sharedPreferences
    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        setSpotifyClientId();
        setSpotifyRedirectUri();
        setRequestCode();
    }

    public void setUsername(String email) {
        editor.putString(USERNAME, email);
        editor.commit();
    }

    public String getUsername() {
        return pref.getString(USERNAME, null);
    }

    public void setPassword(String pwd) {
        editor.putString(PASSWORD, pwd);
        editor.commit();
    }

    public String getPassword() {
        return pref.getString(PASSWORD, null);
    }

    public void setAPIAccessToken(String token) {
        editor.putString(API_ACCESS_TOKEN, token);
        editor.commit();
    }

    public String getAPIAccessToken() {
        return pref.getString(API_ACCESS_TOKEN, null);
    }

    public String getSpotifyAccessToken() {
        return pref.getString(SPOTIFY_ACCESS_TOKEN, null);
    }

    public void setSpotifyAccessToken(String token) {
        editor.putString(SPOTIFY_ACCESS_TOKEN, token);
        editor.commit();
    }

    public void setSpotifyAccountType(String type){
        editor.putString(SPOTIFY_USER_TYPE, type);
        editor.commit();
    }

    public String getSpotifyAccountType() {
        return pref.getString(SPOTIFY_USER_TYPE, null);
    }

    public void setQueueId(int id) {
        editor.putInt(QUEUE_ID, id);
        editor.commit();
    }

    public int getQueueId() {
        return pref.getInt(QUEUE_ID, 0);
    }

    public void setUserType(String type) {
        editor.putString(USER_TYPE, type);
        editor.commit();
    }

    public String getUserType() {
        return pref.getString(USER_TYPE, null);
    }

    public void setQueueName(String name) {
        editor.putString(QUEUE_NAME, name);
        editor.commit();
    }

    public String getQueueName() {
        return pref.getString(QUEUE_NAME, null);
    }


    public void setSpotifyRedirectUri() {
        editor.putString(SPOTIFY_REDIRECT_URI, "http://com.example.ubiq/callback/");
        editor.commit();
    }

    public String getSpotifyRedirectUri() {
        return pref.getString(SPOTIFY_REDIRECT_URI, null);
    }

    public void setSpotifyClientId() {
        editor.putString(SPOTIFY_CLIENT_ID, "605845c07f364d369a0506885b078adb");
        editor.commit();
    }

    public String getSpotifyClientId() {
        return pref.getString(SPOTIFY_CLIENT_ID, null);
    }

    public void setRequestCode() {
        editor.putInt(REQUEST_CODE, 1337);
        editor.commit();
    }

    public int getRequestCode() {
        return pref.getInt(REQUEST_CODE, 0);
    }

    public void setSpotifyTokenTime(long time){
        editor.putLong(SPOTIFY_TOKEN_TIME, time);
        editor.commit();
    }

    public long getAPITokenTime(){
        return pref.getLong(API_TOKEN_TIME, -1);
    }

    public void setAPITokenTime(long time){
        editor.putLong(API_TOKEN_TIME, time);
        editor.commit();
    }

    public long getSpotifyTokenTime(){
        return pref.getLong(SPOTIFY_TOKEN_TIME, -1);
    }

    //Logging in user and setting the name and profile picture
    public void createLogin(
    ) {
        //here, handle the mobile number or email or any details that you
        //use for the login. Then do this:
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
        //false is the default value in case there's nothing found with the key
    }

    public int getLocationAccess() {
        return pref.getInt(LOCATION_ACCESS, 0);
        //false is the default value in case there's nothing found with the key
    }

    public void setLocationAccess(int i){
        editor.putInt(LOCATION_ACCESS, i);
        editor.commit();
    }

    public void clearSession() {
        editor.clear();
        editor.commit();
    }
}