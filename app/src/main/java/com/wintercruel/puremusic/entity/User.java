package com.wintercruel.puremusic.entity;

public class User {
    private String userId;
    private String token;
    private String nickname;
    private String userName;
    private int vipType;
    private String avatarUrl;
    private String backgroundUrl;
    private String cookie;


    public int getVipType() {
        return vipType;
    }

    public String getUserId() {
        return userId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return token;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setVipType(int vipType) {
        this.vipType = vipType;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
}

