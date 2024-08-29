package com.wintercruel.puremusic;

import android.graphics.Bitmap;

public class MusicListItem {
    private String MusicName;
    private String ArtistName;
    private Bitmap MusicImage;

    public Bitmap getMusicImage() {
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

    public void setMusicImage(Bitmap musicImage) {
        MusicImage = musicImage;
    }

    public void setMusicName(String musicName) {
        MusicName = musicName;
    }
}
