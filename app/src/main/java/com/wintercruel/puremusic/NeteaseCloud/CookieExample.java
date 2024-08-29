package com.wintercruel.puremusic.NeteaseCloud;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.wintercruel.puremusic.entity.User;
import com.wintercruel.puremusic.net.server;
import com.wintercruel.puremusic.tools.GetCloudMusicEvent;
import com.wintercruel.puremusic.tools.LoginSuccessful;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CookieExample {
    public static OkHttpClient client;
    private static final String USER_PREFS = "User"; // 用户信息的 SharedPreferences
    private static final String COOKIE_PREFS = "Cookies_Prefs";
    private static final String COOKIE_KEY = "cookie";
    private static SharedPreferences cookiePrefs;

    // 创建一个CookieJar来管理和持久化Cookies
    private static final CookieJar cookieJar = new CookieJar() {
        private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            String host = url.host();
            cookieStore.put(host, new ArrayList<>(cookies));
            saveCookiesToSharedPreferences(host, cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            String host = url.host();
            List<Cookie> cookies = cookieStore.get(host);
            if (cookies == null || cookies.isEmpty()) {
                cookies = loadCookiesFromSharedPreferences(host);
                cookieStore.put(host, cookies);
            }
            return cookies != null ? cookies : new ArrayList<>();
        }
    };

    // 初始化方法，传入Context来获取SharedPreferences
    public static void initialize(Context context) {
        cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE);
        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
    }

    // 将Cookie保存到SharedPreferences
    private static synchronized void saveCookiesToSharedPreferences(String host, List<Cookie> cookies) {
        SharedPreferences.Editor editor = cookiePrefs.edit();
        Set<String> cookieSet = new HashSet<>();
        for (Cookie cookie : cookies) {
            cookieSet.add(serializeCookie(cookie));
        }
        editor.putStringSet(COOKIE_KEY + "_" + host, cookieSet);
        editor.apply();
    }

    // 从SharedPreferences加载Cookies
    private static synchronized List<Cookie> loadCookiesFromSharedPreferences(String host) {
        Set<String> cookieSet = cookiePrefs.getStringSet(COOKIE_KEY + "_" + host, new HashSet<>());
        List<Cookie> cookies = new ArrayList<>();
        for (String cookieString : cookieSet) {
            Cookie cookie = deserializeCookie(cookieString);
            if (cookie != null) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    // 序列化Cookie对象为String
    private static String serializeCookie(Cookie cookie) {
        return cookie.name() + "=" + cookie.value() + ";" +
                "domain=" + cookie.domain() + ";" +
                "path=" + cookie.path() + ";" +
                (cookie.expiresAt() != Long.MAX_VALUE ? "expiresAt=" + cookie.expiresAt() + ";" : "") +
                (cookie.secure() ? "secure;" : "") +
                (cookie.httpOnly() ? "httponly;" : "");
    }

    // 反序列化字符串为Cookie对象
    private static Cookie deserializeCookie(String cookieString) {
        return Cookie.parse(HttpUrl.get(server.ADDRESS), cookieString);
    }



    // 登录方法，使用GET请求
    public static void login(String phoneNumber, String captcha,Context context) {
        // 构建登录请求
        String loginUrl = String.format(server.ADDRESS+"/login/cellphone?phone=%s&captcha=%s", phoneNumber, captcha);
        Request loginRequest = new Request.Builder()
                .url(loginUrl) // 这里使用GET请求
                .build();

        try (Response response = client.newCall(loginRequest).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Login successful: " + response.code());

                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context.getApplicationContext(), "登录成功", Toast.LENGTH_LONG).show();
                    }
                });



                String responseBody=response.body().string();
                System.out.println("登录结果："+responseBody);
                User user= GetUser(responseBody);
                SaveUser(user,context);
                GetPlayList(context);
                EventBus.getDefault().postSticky(new LoginUpdateUI());
                EventBus.getDefault().post(new LoginSuccessful());
            } else {
                System.out.println(response.body().string());
                System.out.println("Login failed: " + response.code());

                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context.getApplicationContext(), "登录失败", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public static User GetUser(String userDetail) throws JSONException {
        User user = new User();
        JSONObject jsonObject = new JSONObject(userDetail);

        // 获取 account 对象，并从中提取 id 和其他信息
        JSONObject accountObject = jsonObject.getJSONObject("account");
        user.setUserId(accountObject.getString("id"));
        user.setUserName(accountObject.getString("userName"));
        user.setVipType(accountObject.getInt("vipType"));
        // 获取 token
        user.setToken(jsonObject.getString("token"));
        user.setCookie(jsonObject.getString("cookie"));
        // 获取 profile 对象，并从中提取 nickname, avatarUrl, backgroundUrl
        JSONObject profileObject = jsonObject.getJSONObject("profile");
        user.setNickname(profileObject.getString("nickname"));
        user.setAvatarUrl(profileObject.getString("avatarUrl"));
        user.setBackgroundUrl(profileObject.getString("backgroundUrl"));
        return user;
    }

    public static void SaveUser(User user,Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("User", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", user.getUserId());
        editor.putString("token", user.getToken());
        editor.putString("nickname", user.getNickname());
        editor.putString("userName", user.getUserName());
        editor.putInt("vipType", user.getVipType());
        editor.putString("avatarUrl", user.getAvatarUrl());
        editor.putString("backgroundUrl", user.getBackgroundUrl());
        editor.putString("cookie",user.getCookie());
        editor.apply();
    }


    // 退出登录方法
    public static void logout(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String logoutUrl = server.ADDRESS+"/logout"; // 退出登录的URL
                Request logoutRequest = new Request.Builder()
                        .url(logoutUrl)
                        .build();

                try (Response response = client.newCall(logoutRequest).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("Logout successful");

                        // 在主线程中更新UI
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clearCookies();
                                clearUserInfo(context);
                                clearPlayList(context);
                                Toast.makeText(context, "成功退出登录", Toast.LENGTH_SHORT).show();
                                EventBus.getDefault().postSticky(new LoginUpdateUI());
                            }
                        });
                    } else {
                        System.out.println("Logout failed");

                        // 在主线程中更新UI
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "退出登录失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    // 在主线程中更新UI
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "网络错误，退出登录失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // 清除Cookies
    private static void clearCookies() {
        SharedPreferences.Editor editor = cookiePrefs.edit();
        editor.remove(COOKIE_KEY);
        editor.apply();
    }

    // 清除用户信息
    private static void clearUserInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // 清除所有用户信息
        editor.apply();
    }

    private static void clearPlayList(Context context){
        SharedPreferences sharedPreferences=context.getSharedPreferences("playList",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }


    //获取用户歌单，保存
    public static void GetPlayList(Context context){
        SharedPreferences sharedPreferences=context.getSharedPreferences(USER_PREFS,Context.MODE_PRIVATE);
        String uid=sharedPreferences.getString("userId",null);

        Request PlayListRequest=new Request.Builder()
                .url(server.ADDRESS+"/user/playlist?uid="+uid)
                .build();
        try(Response response=client.newCall(PlayListRequest).execute()){
            if(response.isSuccessful()){
                String responseDate=response.body().string();
                System.out.println(responseDate);

                JSONObject jsonObject = new JSONObject(responseDate);
                JSONArray playlistArray = jsonObject.getJSONArray("playlist"); // 获取 playlist 数组
                String playListId = null;
                String coverImgUrl=null;
                String name=null;
                String trackCount=null;

                // 遍历每个歌单
                for (int i = 0; i < playlistArray.length(); i++) {
                    JSONObject playlistObject = playlistArray.getJSONObject(i); // 获取每个歌单对象
                    playListId = playlistObject.getString("id"); // 获取歌单 ID
                    coverImgUrl = playlistObject.getString("coverImgUrl"); // 获取封面图 URL
                    name = playlistObject.getString("name"); // 获取歌单名称
                    trackCount=playlistObject.getString("trackCount");//歌单音乐数量

                    // 打印结果
                    System.out.println("ID: " + playListId);
                    System.out.println("Cover Image URL: " + coverImgUrl);
                    System.out.println("Name: " + name);
                    System.out.println("-------------------------");

                    SharedPreferences sharedPreferences_edit = context.getSharedPreferences("playList", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences_edit.edit();
                    editor.putString("playList",responseDate);

                    editor.apply();
                }


            }else {
                Log.d("歌单扫描：","失败");
                System.out.println(response.body().string());
                System.out.println("获取歌单失败: " + response.code());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            Log.d("出错：", String.valueOf(e));

        }


    }




    public static void GetPlayListMusic(String playListId,Context context){

        Request dataRequest=new Request.Builder()
                .url(server.ADDRESS+"/playlist/track/all?id="+playListId)
                .build();

        try(Response response=client.newCall(dataRequest).execute()){
            if(response.isSuccessful()){
                String responseDate=response.body().string();
                Log.d("歌单音乐获取：","成功");
                System.out.println(responseDate);

                SharedPreferences sharedPreferences = context.getSharedPreferences("PlayList"+playListId, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("PlayList",responseDate);
                editor.apply();

                //通知更新歌单
                EventBus.getDefault().post(new GetCloudMusicEvent());
            }else {
                Log.d("歌单音乐获取：","失败");
                System.out.println(response.code());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String GetMusic(String id){
      String url=null;
      Request dataRequest=new Request.Builder()
              .url(server.ADDRESS+"/song/url?id="+id)
              .build();

        try(Response response=client.newCall(dataRequest).execute()){
            if(response.isSuccessful()){
                String responseDate=response.body().string();
                System.out.println(responseDate+"获取音乐Url");

                JSONObject jsonObject=new JSONObject(responseDate);
                JSONArray jsonArray=jsonObject.getJSONArray("data");
                JSONObject jsonObject1=jsonArray.getJSONObject(0);
                url=jsonObject1.getString("url");

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Log.d("获得的Url:",url);
        return url;
    }


    public static String GetLyrics(Context context,String id) {
        String lyrics = null;
        // 创建一个不带CookieJar的OkHttpClient实例
        OkHttpClient clientWithoutCookies = new OkHttpClient.Builder()
                .build();

        Request dataRequest = new Request.Builder()
                .url(server.ADDRESS + "/lyric?id=" + id)
                .build();

        try (Response response = clientWithoutCookies.newCall(dataRequest).execute()) {
            if (response.isSuccessful()) {
                String responseDate = response.body().string();
                System.out.println("获取到歌词: " + responseDate);

                JSONObject jsonObject = new JSONObject(responseDate);
                JSONObject jsonObject1 = jsonObject.getJSONObject("lrc");
                lyrics = jsonObject1.getString("lyric");
                System.out.println("获取到歌词: " + lyrics);
            }
        } catch (IOException | JSONException e) {
            Toast.makeText(context.getApplicationContext(),"歌词获取失败",Toast.LENGTH_LONG).show();
        }

        return lyrics;
    }




    public static String SearchMusic(Context context,String SearchText){
        String responseDate=null;

        // 创建一个不带CookieJar的OkHttpClient实例
        OkHttpClient clientWithoutCookies = new OkHttpClient.Builder()
                .build();

        Request dataRequest=new Request.Builder()
                .url(server.ADDRESS+"/cloudsearch?keywords="+SearchText+"&type=1")
                .build();

        try(Response response=clientWithoutCookies.newCall(dataRequest).execute()){
            if(response.isSuccessful()){
                responseDate=response.body().string();
                System.out.println("音乐搜索结果"+responseDate);


            }

        } catch (IOException e) {

            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(),"搜索出错请检查网络连接", Toast.LENGTH_LONG).show();
                }
            });

        }

        return responseDate;
    }

    public static String GetHotSearch(Context context){
        String responseDate=null;

        // 创建一个不带CookieJar的OkHttpClient实例
        OkHttpClient clientWithoutCookies = new OkHttpClient.Builder()
                .build();

        Request dataRequest=new Request.Builder()
                .url(server.ADDRESS+"/search/hot/detail")
                .build();

        try(Response response=clientWithoutCookies.newCall(dataRequest).execute()){
            if(response.isSuccessful()){
                responseDate=response.body().string();
                System.out.println("热搜显示结果"+responseDate);
            }

        } catch (IOException e) {

            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(),"热搜加载失败请检查网络连接", Toast.LENGTH_LONG).show();
                }
            });

        }

        return responseDate;
    }





}