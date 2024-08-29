package com.wintercruel.puremusic.NeteaseCloud;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class NetworkUtils {

    private static final OkHttpClient client = new OkHttpClient();

    public static void getDataWithToken(String url, String token, String cookie) {
        // 创建请求对象
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", cookie)  // 添加 Cookie 头
                .addHeader("Content-Type", "application/json")  // 添加 Content-Type 头
                .build();

        // 异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败处理
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 请求成功，处理响应数据
                    final String responseData = response.body().string();
                    // 在 UI 线程中更新 UI
                    // 例如：runOnUiThread(() -> textView.setText(responseData));
                    System.out.println(responseData);
                } else {
                    // 请求失败的处理
                    System.out.println("Request failed: " + response.code());
                }
            }
        });
    }









}