package com.example.ubiq;

import com.spotify.protocol.types.Album;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Artist extends SearchResult{
    private ArrayList<String> genres;
    private ArrayList<MusicAlbum> albums;
    private ArrayList<Track> topTracks;

    public Artist(String name){
        super(name);
    }

    public Artist(String id, String name, ArrayList<String> genres){
        super(id,name);
        this.genres = genres;
    }

    public Artist(String id, String name, String url, ArrayList<String> genres, int popularity){
        super(id,name,url, popularity);
        this.genres = genres;
    }

    public void setAlbums(String jsonString){
        this.albums = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonString);
            if(obj.has("items") && !obj.isNull("items")) {
                JSONArray items = obj.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String id = item.getString("id");
                    String[] dateParts = item.getString("release_date").split("-");
                    String yearStr = dateParts[0];
                    int year = Integer.parseInt(yearStr);
                    String name = item.getString("name");
                    String image = "";
                    JSONArray images = item.getJSONArray("images");
                    if (images.length() > 0) {
                        JSONObject img = images.getJSONObject(0);
                        image = img.getString("url");
                    }
                    MusicAlbum album = new MusicAlbum(id, name, year, this.name, image);
                    this.albums.add(album);
                }
            }
        } catch (Exception e){}
    }

    public void setTopTracks(String jsonString){
        this.topTracks = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonString);
            if(obj.has("tracks") && !obj.isNull("tracks")){
                JSONArray items = obj.getJSONArray("tracks");
                for (int i = 0; i < items.length() && i < 5; i++) {
                    JSONObject item = items.getJSONObject(i);
                    String id = item.getString("uri");
                    String name = item.getString("name");
                    int duration = item.getInt("duration_ms");
                    JSONObject album = item.getJSONObject("album");
                    String albumName = album.getString("name");
                    int popularity = item.getInt("popularity");
                    String image = "";
                    JSONArray images = album.getJSONArray("images");
                    if (images.length() > 0) {
                        JSONObject img = images.getJSONObject(0);
                        image = img.getString("url");
                    }
                    ArrayList<Artist> artist = new ArrayList<>();
                    artist.add(this);
                    Track track = new Track(id, name, artist, albumName, image, duration, popularity);
                    topTracks.add(track);
                }
            }
        } catch (Exception e){}
    }

    public ArrayList<String> getGenres(){
        return this.genres;
    }

    public ArrayList<Track> getTopTracks() {
        return topTracks;
    }

    public ArrayList<MusicAlbum> getAlbums() {
        return albums;
    }
}
