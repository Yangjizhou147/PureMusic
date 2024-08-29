package com.wintercruel.puremusic.entity;

public class MusicCloud {

    private String uriCloud;
    private String titleCloud;
    private String artistCloud;
    private String albumCloud;

    public MusicCloud(String uri, String title, String artist, String album) {
        this.uriCloud = uri;
        this.titleCloud = title;
        this.artistCloud = artist;
        this.albumCloud = album;
    }

    public String getUri() {
        return uriCloud;
    }

    public String getTitle() {
        return titleCloud;
    }

    public String getArtist() {
        return artistCloud;
    }

    public String getAlbumUrl() {
        return albumCloud;
    }


}
