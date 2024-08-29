package com.wintercruel.puremusic;

import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetLyrics;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.graphics.Bitmap;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.wintercruel.puremusic.adapter.MusicListAdapterCloud;
import com.wintercruel.puremusic.entity.MusicCloud;
import com.wintercruel.puremusic.net.server;
import com.wintercruel.puremusic.service.MyMusicService;
import com.wintercruel.puremusic.tools.BrightnessTransformation;
import com.wintercruel.puremusic.tools.CloudMusicLoadEvent;
import com.wintercruel.puremusic.tools.CloudMusicUpdateUiEvent;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.MusicUpdateEvent;
import com.wintercruel.puremusic.tools.MusicUpdateEventIndexToPlay;
import com.wintercruel.puremusic.tools.MyMusicPlayerUpdateUI;
import com.wintercruel.puremusic.tools.PlayingMusicEvent;
import com.wintercruel.puremusic.tools.TransparentBar;
import com.zjy.audiovisualize.view.AudioVisualizeView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jp.wasabeef.glide.transformations.BlurTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class CloudMusic extends AppCompatActivity {

    private RecyclerView playList;
    private MusicListAdapterCloud musicListAdapter;
    private List<MusicListItemCloud> musicListItems;
    private List<MediaItem> mediaItems;
    private String PlayListId;
    private final List<MusicCloud> newPlaylist = new ArrayList<>();
    private MyMusicService musicService;
    private ExoPlayer exoPlayer;
    private boolean isBound = false;
    private int Position=0;
    private ImageButton CoverImage;
    private ImageView BackGround;
    private TextView MusicName;
    private static final int PAGE_SIZE = 10; // 每页加载的音乐数量
    private int currentPage = 0; // 当前页数
    private AudioVisualizeView vAudioVisualize;
    private boolean isInCurrentPlaylist = false; // 用于标记是否在当前歌单


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyMusicService.LocalBind binder = (MyMusicService.LocalBind) service;
            musicService = binder.getService();

            exoPlayer=musicService.getPlayer();
            isBound = true;

            LoadMyMusic();   //播放器播放列表接在
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };




    @Override
    protected void onStart() {
        super.onStart();
        // 绑定服务
        EventBus.getDefault().register(this);
        UpdateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        // 解绑服务
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cloud_music);

        Intent intent=getIntent();
        PlayListId=intent.getStringExtra("PlayListId");
        TransparentBar.transparentNavBar(this);
        TransparentBar.transparentStatusBar(this);
        CoverImage=findViewById(R.id.CoverImg1);
        BackGround=findViewById(R.id.PlayListBackGround);
        MusicName=findViewById(R.id.PlayListBackGround_Text);

        playList=findViewById(R.id.PlayListRecyclerView);
        playList.setItemAnimator(new DefaultItemAnimator());

        Intent intent1 = new Intent(this, MyMusicService.class);
        bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);

        mediaItems=new ArrayList<>();
        playList.setLayoutManager(new LinearLayoutManager(this));
        musicListItems=new ArrayList<>();
        musicListAdapter=new MusicListAdapterCloud(this,musicListItems);
        playList.setAdapter(musicListAdapter);
        vAudioVisualize = findViewById(R.id.audio_visualize_view);

        UpdateUI();
        LoadMusicBackGround();  //列表加载

        CloudMusicUpdateUiEvent cloudMusicUpdateUiEventI=EventBus.getDefault().getStickyEvent(CloudMusicUpdateUiEvent.class);
        if(cloudMusicUpdateUiEventI!=null){
            onCloudMusicUpdateUiEvent(cloudMusicUpdateUiEventI);
        }

        CoverImage.setOnClickListener(new View.OnClickListener() {
            @UnstableApi @Override
            public void onClick(View v) {
                Intent intent=new Intent(CloudMusic.this, MusicPlayNext.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_bottom,R.anim.slide_out_top);
                EventBus.getDefault().postSticky(new MusicUpdateEventIndexToPlay());
            }
        });
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCloudMusicUpdateUiEvent(CloudMusicUpdateUiEvent event) {
        // 在这里处理接收到的事件，例如更新UI
        UpdateUI();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloudMusicLoadEvent(CloudMusicLoadEvent event){
        LoadMyMusic();
    }

    //背景的效果
    MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
            new BlurTransformation(50, 4),
            new BrightnessTransformation(1.1f)
    );

    private void UpdateUI(){
        if(MusicHolder.getAlbumArtUrl()==null){
            Glide.with(this)
                    .load(MusicHolder.getAlbumArt())
                    .placeholder(BackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                    .transform(multiTransformation)
                    .into(BackGround);

            Glide.with(this)
                    .load(MusicHolder.getAlbumArt())
                    .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                    .into(CoverImage);
        }else {
            Glide.with(this)
                    .load(MusicHolder.getAlbumArtUrl())
                    .placeholder(BackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                    .transform(multiTransformation)
                    .into(BackGround);


            Glide.with(this)
                    .load(MusicHolder.getAlbumArtUrl())
                    .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                    .into(CoverImage);
        }
        MusicName.setText(MusicHolder.getMusicName());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void PlayingMusicPosition(PlayingMusicEvent event){

        if(MusicHolder.isIsSearchMode()){

            HandlerThread handlerThread = new HandlerThread("NetworkThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    LoadMusicToMediaBackground();

                    runOnUiThread(()->{

                        Position=event.position;
                        exoPlayer.prepare();
                        exoPlayer.seekTo(Position,0);
                        exoPlayer.play();

                        HandlerThread handlerThread = new HandlerThread("NetworkThread");
                        handlerThread.start();
                        Handler handler = new Handler(handlerThread.getLooper());

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                LoadMusic(Position);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        UpdateUI();
                                        //通知MainActivity更新UI
                                        EventBus.getDefault().post(new MusicUpdateEvent());
                                    }
                                });
                            }
                        });
                    });
                }
            });
        }


        Position=event.position;
        exoPlayer.prepare();
        exoPlayer.seekTo(Position,0);
        exoPlayer.play();

        HandlerThread handlerThread = new HandlerThread("NetworkThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                LoadMusic(Position);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateUI();
                        //通知MainActivity更新UI
                        EventBus.getDefault().post(new MusicUpdateEvent());
                    }
                });
            }
        });

    }


    //扫描音乐到播放列表
    private void LoadMyMusic(){

        if(MyMusicService.GetNowPlayListId()==null){
            MusicHolder.setPlayingMode(true);
            exoPlayer.pause();
            exoPlayer.clearMediaItems();
            MyMusicService.SetNowPlayListId(PlayListId);
            HandlerThread handlerThread = new HandlerThread("NetworkThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            Log.d("歌单为切换：","ID为null");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LoadMusicToMediaBackground();
                }
            });

        }else if(!MyMusicService.GetNowPlayListId().equals(PlayListId)){
            MusicHolder.setPlayingMode(true);
            exoPlayer.clearMediaItems();
//            String NowId= MyMusicService.GetNowPlayListId();
            MyMusicService.SetNowPlayListId(PlayListId);
            exoPlayer.pause();
            exoPlayer.clearMediaItems();
            Log.d("歌单为切换：","歌单ID差异");
            HandlerThread handlerThread = new HandlerThread("NetworkThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    LoadMusicToMediaBackground();
                }
            });
        } else if (!MusicHolder.isPlayingMode()) {
            MusicHolder.setPlayingMode(true);
            exoPlayer.clearMediaItems();
//            String NowId= MyMusicService.GetNowPlayListId();
            MyMusicService.SetNowPlayListId(PlayListId);
            exoPlayer.pause();
            exoPlayer.clearMediaItems();
            Log.d("歌单为切换：","从本地音乐切换而来");
            HandlerThread handlerThread = new HandlerThread("NetworkThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    LoadMusicToMediaBackground();
                }
            });
        }

    }




    private void LoadMusicToMediaBackground() {

        String Name = "PlayList" + PlayListId;
        SharedPreferences sharedPreferences = this.getSharedPreferences(Name, MODE_PRIVATE);
        String MusicDate = sharedPreferences.getString("PlayList", null);

        try {
            if (MusicDate != null) {
                JSONObject jsonObject = new JSONObject(MusicDate);
                JSONArray musicArray = jsonObject.getJSONArray("songs");
                StringBuilder idBuilder = new StringBuilder();

                // 收集所有的音乐 ID
                for (int i = 0; i < musicArray.length(); i++) {
                    JSONObject musicData = musicArray.getJSONObject(i);
                    String id = musicData.getString("id");

                    // 收集 ID
                    if (i > 0) {
                        idBuilder.append(","); // 在 ID 之间添加逗号
                    }
                    idBuilder.append(id);
                }

                // 合并请求 URL
                String combinedUrl = "/song/url?id=" + idBuilder.toString();
                // 调用方法获取音乐流地址
                fetchMusicUrls(combinedUrl);

            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // 新增方法：根据合并的 URL 获取音乐流地址
    private void fetchMusicUrls(String combinedUrl) {
        // 假设你使用的是OkHttp或者Retrofit等库发送网络请求

        Request request = new Request.Builder()
                .url(server.ADDRESS + combinedUrl) // 替换为你的基础 URL
                .build();



        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    // 解析响应并提取音乐的 URL
                    parseMusicUrls(responseData);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    // 解析音乐流地址
    private void parseMusicUrls(String responseData) {
        try {
            // 解析响应 JSON
            JSONObject jsonResponse = new JSONObject(responseData);
            JSONArray songsArray = jsonResponse.getJSONArray("data"); // 假设返回的数据在 "data" 字段中

            System.out.println("音乐数据："+songsArray);
            List<MediaItem>mediaItems=new ArrayList<>();

            for (int i = 0; i < songsArray.length(); i++) {
                JSONObject song = songsArray.getJSONObject(i);
                String url = song.getString("url"); // 根据实际 JSON 结构修改
                System.out.println(url);
                String id = song.getString("id"); // 如果需要 ID，可以提取

                MediaItem mediaItem=MediaItem.fromUri(url);
                mediaItems.add(mediaItem);
                // 在 UI 线程中添加媒体项

            }

            runOnUiThread(() -> {

                MyMusicService.AddMediaItem(mediaItems);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //后台加载音乐到列表显示
    private void LoadMusicBackGround(){
        HandlerThread handlerThread = new HandlerThread("NetworkThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                ScanCloudMusicFromJson();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });


    }


    private void ScanCloudMusicFromJson(){

        String Name="PlayList"+PlayListId;
        SharedPreferences sharedPreferences = this.getSharedPreferences(Name, MODE_PRIVATE);
        String MusicDate = sharedPreferences.getString("PlayList", null);
        System.out.println(PlayListId);
        System.out.println(MusicDate);
        List<MusicListItemCloud> Items=new ArrayList<>();
        if(MusicDate!=null){
            try {
                JSONObject jsonObject=new JSONObject(MusicDate);
                JSONArray musicArray=jsonObject.getJSONArray("songs");
                // 获取第一个音乐对象（假设有多个歌曲时，你可以迭代）

                for(int i=0;i<musicArray.length();i++){
                    JSONObject musicData=musicArray.getJSONObject(i);
                    String name=musicData.getString("name");
                    String id=musicData.getString("id");
                    // 获取专辑信息
                    JSONObject albumObject = musicData.getJSONObject("al"); // 使用 musicData 而不是 jsonObject
                    // 获取专辑图片地址
                    String albumPicUrl = albumObject.getString("picUrl");
                    // 获取艺术家数组
                    JSONArray arArray = musicData.getJSONArray("ar"); // 使用 musicData 而不是 jsonObject
                    // 使用 StringBuilder 来组合艺术家名字
                    StringBuilder artistNames = new StringBuilder();

                    // 遍历艺术家数组，拼接艺术家名称
                    for (int j = 0; j < arArray.length(); j++) {
                        JSONObject artistObject = arArray.getJSONObject(j);
                        String artistName = artistObject.getString("name");

                        // 如果不是第一个艺术家，加上分隔符
                        if (j > 0) {
                            artistNames.append("/"); // 用斜杠隔开
                        }
                        artistNames.append(artistName); // 添加艺术家名称
                    }
                    // 将组合的艺术家名称转换为字符串
                    String combinedArtistNames = artistNames.toString();

                    MusicListItemCloud Item=new MusicListItemCloud();
                    Item.setMusicImage(albumPicUrl);
                    Item.setMusicName(name);
                    Item.setArtistName(combinedArtistNames);
                    Items.add(Item);
                }

                runOnUiThread(()->{
                    musicListItems.addAll(Items);
                    int startPosition=musicListItems.size();
                    musicListAdapter.notifyItemChanged(startPosition,Items.size());
                });

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

    }



    //加载正在播放的音乐信息
    private void LoadMusic(int position){
        String Name="PlayList"+PlayListId;
        SharedPreferences sharedPreferences = this.getSharedPreferences(Name, MODE_PRIVATE);
        String MusicDate = sharedPreferences.getString("PlayList", null);
        try {
            JSONObject jsonObject=new JSONObject(MusicDate);
            JSONArray musicArray=jsonObject.getJSONArray("songs");
            // 获取第一个音乐对象（假设有多个歌曲时，你可以迭代）
            JSONObject musicData=musicArray.getJSONObject(position);
            MusicHolder.setMusicName(musicData.getString("name"));
            // 获取专辑信息
            JSONObject albumObject = musicData.getJSONObject("al"); // 使用 musicData 而不是 jsonObject
            // 获取专辑图片地址
            MusicHolder.setAlbumArtUrl(albumObject.getString("picUrl"));

            JSONArray arArray = musicData.getJSONArray("ar"); // 使用 musicData 而不是 jsonObject
            // 使用 StringBuilder 来组合艺术家名字
            StringBuilder artistNames = new StringBuilder();

            // 遍历艺术家数组，拼接艺术家名称
            for (int j = 0; j < arArray.length(); j++) {
                JSONObject artistObject = arArray.getJSONObject(j);
                String artistName = artistObject.getString("name");

                // 如果不是第一个艺术家，加上分隔符
                if (j > 0) {
                    artistNames.append("/"); // 用斜杠隔开
                }
                artistNames.append(artistName); // 添加艺术家名称
            }
            MusicHolder.setArtistName(artistNames.toString());

            String id=musicData.getString("id");

            String lyrics=GetLyrics(getApplicationContext(),id);
            MusicHolder.setLyrics(lyrics);
            runOnUiThread(()->{
                EventBus.getDefault().postSticky(new MyMusicPlayerUpdateUI());
                Log.d("切换音乐发布粘性事件：","发布");

            });

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }




}