package com.example.ubiq;

import java.util.ArrayList;
import java.util.Arrays;

public class HttpResponseManager {

    public HttpResponseManager(){}

    public ArrayList<String> responseToStringList(String result){
        String mid = result.substring(1, result.length() - 1);
        ArrayList<String> list = new ArrayList<>();
        if(mid.length()>0) {
            ArrayList<String> tmp = new ArrayList<>(Arrays.asList(mid.split(",")));
            for(String s : tmp) {
                list.add(s.substring(1, s.length() - 1));
            }
        }
        return list;
    }

}
