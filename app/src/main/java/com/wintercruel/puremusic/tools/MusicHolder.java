package com.wintercruel.puremusic.tools;

import android.graphics.Bitmap;
import android.icu.text.StringSearch;

// 创建一个 Singleton 类来保存数据
public class MusicHolder {
    private static Bitmap albumArt;
    private static String musicName;
    private static String artistName;
    private static String musicUrl;
    private static String playListId;
    private static String albumArtUrl;
    private static String Lyrics;
    private static String SearchResult;
    private static boolean playingMode;//管理音乐模式，约定true为网络音乐，false为本地音乐
    private static boolean isSearchMode;
    public static void setAlbumArt(Bitmap art) {
        albumArt = art;
    }

    public static Bitmap getAlbumArt() {
        return albumArt;
    }
    public static void setMusicName(String MusicName){
        musicName=MusicName;
    }

    public static String getMusicName() {
        return musicName;
    }

    public static void setArtistName(String artistName) {
        MusicHolder.artistName = artistName;
    }

    public static String getArtistName() {
        return artistName;
    }

    public static String getMusicUrl() {
        return musicUrl;
    }

    public static void setMusicUrl(String musicUrl) {
        MusicHolder.musicUrl = musicUrl;
    }

    public static String getPlayListId() {
        return playListId;
    }

    public static void setPlayListId(String playListId) {
        MusicHolder.playListId = playListId;
    }

    public static String getAlbumArtUrl() {
        return albumArtUrl;
    }

    public static void setAlbumArtUrl(String albumArtUrl) {
        MusicHolder.albumArtUrl = albumArtUrl;
    }

    public static String getLyrics() {
        return Lyrics;
    }

    public static void setLyrics(String lyrics) {
        Lyrics = lyrics;
    }

    public static boolean isPlayingMode() {
        return playingMode;
    }

    public static void setPlayingMode(boolean playingMode) {
        MusicHolder.playingMode = playingMode;
    }

    public static String getSearchResult() {
        return SearchResult;
    }

    public static void setSearchResult(String searchResult) {
        SearchResult = searchResult;
    }

    public static boolean isIsSearchMode() {
        return isSearchMode;
    }

    public static void setIsSearchMode(boolean isSearchMode) {
        MusicHolder.isSearchMode = isSearchMode;
    }
}

