package com.wintercruel.puremusic;


public class MusicListItemCloud {

    private String MusicName;
    private String ArtistName;
    private String MusicImage;

    public String getMusicImage() {
        return MusicImage;
    }

    public String getArtistName() {
        return ArtistName;
    }

    public String getMusicName() {
        return MusicName;
    }

    public void setArtistName(String artistName) {
        ArtistName = artistName;
    }

    public void setMusicImage(String musicImage) {
        MusicImage = musicImage;
    }

    public void setMusicName(String musicName) {
        MusicName = musicName;
    }

}
