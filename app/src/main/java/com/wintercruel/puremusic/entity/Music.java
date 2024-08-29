package com.wintercruel.puremusic.entity;


public  class Music {
    private String uri;
    private String title;
    private String artist;
    private Long albumId;

    public Music(String uri, String title, String artist, Long albumId) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.albumId = albumId;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Long getAlbumId() {
        return albumId;
    }
}