package com.example.ubiq;

//Classe abstrata que engloba todas as formas de resultados de pesquisa: as classes Artist, MusicAlbum e Track

public abstract class SearchResult {
    protected String spotifyId;
    protected String name;
    protected String img;
    protected int popularity;

    public SearchResult(String spotifyId, String name, String img, int popularity){
        this.spotifyId = spotifyId;
        this.name = name;
        this.img = img;
        this.popularity = popularity;
    }

    public SearchResult(String spotifyId, String name, String img){
        this.spotifyId = spotifyId;
        this.name = name;
        this.img = img;
    }

    public SearchResult(String spotifyId, String name){
        this.spotifyId = spotifyId;
        this.name = name;
    }

    public SearchResult(String name){
        this.name = name;
    }

    public String getImg() {
        return img;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getName() {
        return name;
    }

    public int getPopularity() {
        return popularity;
    }
}
