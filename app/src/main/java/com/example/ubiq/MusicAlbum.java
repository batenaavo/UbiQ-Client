package com.example.ubiq;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MusicAlbum extends SearchResult{
    private int year;
    private String artist;
    private ArrayList<String> genres;
    private ArrayList<Track> tracks;

    public MusicAlbum(String id, String title, int year, String artist, String img){
        super(id,title,img);
        this.year = year;
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    public void setTracks(String response, String img){
        this.tracks = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(response);
            if (obj.has("items") && !obj.isNull("items")) {
                JSONArray items = obj.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject item = items.getJSONObject(i);
                    String id = item.getString("uri");
                    String name = item.getString("name");
                    int duration = item.getInt("duration_ms");
                    ArrayList<Artist> artistList = new ArrayList<>();
                    JSONArray artists = item.getJSONArray("artists");
                    for (int j = 0; j < artists.length(); j++) {
                        JSONObject artist = artists.getJSONObject(j);
                        String artistName = artist.getString("name");
                        artistList.add(new Artist(artistName));
                    }
                    Track track = new Track(id, name, artistList, this.name, img, duration);
                    tracks.add(track);
                }
            }
        } catch (Exception e){e.printStackTrace();}
    }

    public int getYear() {
        return year;
    }
}
