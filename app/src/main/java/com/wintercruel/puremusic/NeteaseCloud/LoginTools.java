package com.wintercruel.puremusic.NeteaseCloud;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.wintercruel.puremusic.entity.User;
import com.wintercruel.puremusic.net.server;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
public class LoginTools {
    //验证码登录
    public void loginWithCaptcha(String phoneNumber, String captcha,Context context) {
        OkHttpClient client = new OkHttpClient();

        if(phoneNumber!=null&&captcha!=null){

            String url = server.ADDRESS+"/login/cellphone?phone=" + phoneNumber + "&captcha=" + captcha;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    e.printStackTrace();
                    // Handle failure here
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        System.out.println(responseData);
                        // Handle success here (e.g., parse the response and navigate to the next screen)
                        // Save token in SharedPreferences
                        try {
                            User user = GetUser(responseData);
                            SaveUser(user, context);
                            Log.d("保存用户信息","成功");

                            EventBus.getDefault().post(new LoginUpdateUI());

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // Handle error here
                        System.out.println("Request failed with status code: " + response.code());
                    }
                }
            });
        }else {
            Toast.makeText(context.getApplicationContext(), "请输入账号和验证码",Toast.LENGTH_LONG).show();
        }
    }


    //发送验证码
    public void sendCaptcha(String phoneNumber) {
        OkHttpClient client = new OkHttpClient();

        String url = server.ADDRESS+"/captcha/sent?phone=" + phoneNumber;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                // Handle failure here
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    // Handle success here (e.g., notify the user that the SMS has been sent)
                } else {
                    // Handle error here
                }
            }
        });
    }

    public User GetUser(String userDetail) throws JSONException {
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


    public void SaveUser(User user,Context context){
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

    public void GetUsrPlayList(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("User", MODE_PRIVATE);
        String token=sharedPreferences.getString("token",null);
//        String userId= sharedPreferences.getString("userId",null);
        String cookie=sharedPreferences.getString("cookie",null);
        String url = server.ADDRESS+"/song/url?id=1903149553&br=999000";
        NetworkUtils.getDataWithToken(url,token,cookie);

    }

}
