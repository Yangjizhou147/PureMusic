package com.wintercruel.puremusic;

import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetUser;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.SaveUser;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.login;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.wintercruel.puremusic.NeteaseCloud.CookieExample;
import com.wintercruel.puremusic.NeteaseCloud.LoginTools;
import com.wintercruel.puremusic.entity.User;
import com.wintercruel.puremusic.net.server;
import com.wintercruel.puremusic.tools.LoginSuccessful;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Login extends AppCompatActivity {

    private EditText UserAccount;
    private EditText Captcha;
    private ImageButton Login;
    private ImageButton GetCapChar;
    private String PhoneNumber;
    private String MyCapChar;
    private ImageView QR_code;
    private Button QR_code_login;
    private TextView Reminder;
    private Boolean isFirstClick=true;

    private static final String BASE_URL = server.ADDRESS;
    private String qrKey = "your_generated_key";  // 替换为实际生成的key
    private boolean isPolling = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        UserAccount = findViewById(R.id.userAccount);
        Captcha = findViewById(R.id.AuthCode);
        Login= findViewById(R.id.button2);
        GetCapChar = findViewById(R.id.GetAuthCode);
        QR_code=findViewById(R.id.QR_code);
        QR_code_login=findViewById(R.id.QR_codeLogin);
        Reminder=findViewById(R.id.Reminder);
        QR_code.setVisibility(View.GONE);
        EventBus.getDefault().register(this);

        QR_code_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFirstClick){
                    QR_code.setVisibility(View.VISIBLE);
                    UserAccount.setVisibility(View.GONE);
                    Captcha.setVisibility(View.GONE);
                    Login.setVisibility(View.GONE);
                    GetCapChar.setVisibility(View.GONE);

                    QR_code_login.setText("验证码登录");
                    GetQRCode();
                    CookieExample.initialize(Login.getContext());
                    // 开始轮询二维码状态
                    pollQrCodeStatus();
                    isFirstClick=false;
                }else {
                    QR_code.setVisibility(View.GONE);
                    UserAccount.setVisibility(View.VISIBLE);
                    Captcha.setVisibility(View.VISIBLE);
                    Login.setVisibility(View.VISIBLE);
                    GetCapChar.setVisibility(View.VISIBLE);
                    isFirstClick=true;
                    QR_code_login.setText("二维码登录");
                    isPolling = false; // 停止轮询
                    Reminder.setText("");
                }
            }
        });
        // 设置 GetCapChar 按钮的点击事件
        GetCapChar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = UserAccount.getText().toString();
                Log.d("手机号", phoneNumber);
                // 假设你有一个 LoginTools 类来处理验证码请求
                LoginTools loginTools = new LoginTools();
                loginTools.sendCaptcha(phoneNumber);
            }
        });

        // 设置 Login 按钮的点击事件
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = UserAccount.getText().toString();
                String enteredCaptcha = Captcha.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        login(phoneNumber,enteredCaptcha, Login.getContext());

                    }
                }).start();
            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginSuccessful(LoginSuccessful event) {
        finish();
    }


    private void GetQRCode(){
        OkHttpClient client = new OkHttpClient();
            String url = server.ADDRESS+"/login/qr/key";

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
                        try {
                            JSONObject jsonObject=new JSONObject(responseData);
                            JSONObject dataObject = jsonObject.getJSONObject("data");
                            String key=dataObject.getString("unikey");
                            GetBase(key);
                            qrKey=key;

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    } else {
                        // Handle error here
                        System.out.println("Request failed with status code: " + response.code());
                    }
                }
            });

    }


    private void GetBase(String key) {
        OkHttpClient client = new OkHttpClient();
        String url = server.ADDRESS+"/login/qr/create?key=" + key;

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

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        String QRImage = dataObject.getString("qrurl");

                        // 使用runOnUiThread确保UI更新在主线程中进行
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                    Bitmap bitmap = barcodeEncoder.encodeBitmap(QRImage, BarcodeFormat.QR_CODE, 400, 400);
                                    QR_code.setImageBitmap(bitmap);
                                } catch (WriterException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("Request failed with status code: " + response.code());
                }
            }
        });
    }


    private void pollQrCodeStatus() {
        new Thread(() -> {
            while (isPolling) {
                try {
                    // 构建请求URL
                    String url = BASE_URL + "/login/qr/check?key=" + qrKey;

                    // 构建请求
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    // 执行请求
                    Response response = CookieExample.client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("QRCodeCheck", "收到二维码检查响应: " + responseBody);
                        handleQrCodeResponse(responseBody);
                    } else {
                        Log.e("QRCodeCheck", "Request failed: " + response.code());
                    }

                    // 每隔一定时间轮询一次
                    Thread.sleep(1000); // 2秒轮询一次

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleQrCodeResponse(String responseBody) {
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            int code = jsonObject.getInt("code");

            switch (code) {
                case 800:
                        runOnUiThread(()->{
                            Reminder.setText("二维码已过期，请重新生成");
                        });
                    break;

                case 801:
                    runOnUiThread(()->{
                        Reminder.setText("等待扫码");
                    });
                    break;

                case 802:
                    runOnUiThread(()->{
                        Reminder.setText("等待确认");
                    });
                    break;

                case 803:
                        runOnUiThread(()->{
                            Reminder.setText("授权登录成功");
                        });
                        isPolling = false; // 停止轮询

                    // 处理返回的 cookies
                    // 你可以在这里做后续的操作，比如保存用户信息等
                    User user= GetUser(responseBody);
                    SaveUser(user,this);
                    break;

                default:
                    Log.e("QRCodeCheck", "Unknown status code: " + code);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止轮询
        EventBus.getDefault().unregister(this); // 注销 EventBus
        isPolling = false;
    }



}