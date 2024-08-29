package com.wintercruel.puremusic;

public class PlayListItem {
    private String playListId;
    private String playListName;
    private String imgUrl;
    private String trackCount;


    public void setPlayListId(String playListId) {
        this.playListId = playListId;
    }

    public void setPlayListName(String playListName) {
        this.playListName = playListName;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }


    public String getPlayListId() {
        return playListId;
    }

    public String getPlayListName() {
        return playListName;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(String trackCount) {
        this.trackCount = trackCount;
    }
}
