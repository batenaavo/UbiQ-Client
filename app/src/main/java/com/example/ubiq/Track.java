package com.example.ubiq;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Track extends SearchResult{
    private ArrayList<Artist> artists;
    private int duration;
    private String albumName;


    public Track(String id, String name, ArrayList<Artist> artists, String album, String img, int duration, int popularity){
        super(id,name,img, popularity);
        this.artists = artists;
        this.albumName = album;
        this.duration = duration;
    }

    public Track(String id, String name, ArrayList<Artist> artists, String album, String img, int duration){
        super(id,name,img);
        this.artists = artists;
        this.albumName = album;
        this.duration = duration;
    }

    public Track(String id, String name, String album, String img, int duration, int popularity){
        super(id,name,img, popularity);
        this.albumName = album;
        this.duration = duration;
        this.artists = new ArrayList<>();
    }



    public void addArtist(Artist artist){
        this.artists.add(artist);
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getDuration() {
        return duration;
    }

    public ArrayList<Artist> getArtists() {
        return artists;
    }
}
