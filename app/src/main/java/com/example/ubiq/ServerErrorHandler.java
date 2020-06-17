package com.example.ubiq;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ServerErrorHandler {

    public String getRegisterErrorString(VolleyError error){
        NetworkResponse errorRes = error.networkResponse;
        String errorStr = "";
        if(errorRes != null && errorRes.data != null) {
            try {
                errorStr = new String(errorRes.data, "UTF-8");
                JSONObject obj = new JSONObject(errorStr);
                errorStr = obj.getString("error_description");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return errorStr;
    }

    public String getErrorString(VolleyError error){
        NetworkResponse errorRes = error.networkResponse;
        String errorStr = "";
        if(errorRes != null && errorRes.data != null) {
            try {
                errorStr = new String(errorRes.data, "UTF-8");
                JSONObject obj = new JSONObject(errorStr);
                errorStr = obj.getString("Message");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return errorStr;
    }

    public ArrayList<String> getErrorArray(VolleyError error){
        ArrayList<String> errors = new ArrayList<>();
            NetworkResponse errorRes = error.networkResponse;
            String errorStr = "";
            if(errorRes != null && errorRes.data != null) {
                try {
                    errorStr = new String(errorRes.data, "UTF-8");
                    JSONObject obj = new JSONObject(errorStr).getJSONObject("ModelState");
                    JSONArray array = obj.getJSONArray("");
                    for (int i = 0; i < array.length(); i++)
                        errors.add(array.getString(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        return errors;
    }
}
