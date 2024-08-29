package com.wintercruel.puremusic;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerControlView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.wintercruel.puremusic.service.MyMusicService;
import com.wintercruel.puremusic.tools.BrightnessTransformation;
import com.wintercruel.puremusic.tools.FileUtils;
import com.wintercruel.puremusic.tools.LyricsFileUtils;
import com.wintercruel.puremusic.tools.LyricsManager;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.MusicMetadataUtils;
import com.wintercruel.puremusic.tools.MusicPlayPauseEvent;
import com.wintercruel.puremusic.tools.MyMusicPlayerSeek;
import com.wintercruel.puremusic.tools.MyMusicPlayerUpdateUI;
import com.wintercruel.puremusic.tools.TransparentBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import jp.wasabeef.glide.transformations.BlurTransformation;
import me.zhengken.lyricview.LyricView;

@UnstableApi public class MusicPlayNext extends AppCompatActivity {

    private ImageView PlayBackGround;
    @SuppressLint("UnsafeOptInUsageError")
    private PlayerControlView playerControlView;
    private ExoPlayer player;
    private static final int UPDATE_INTERVAL = 900; // 更新间隔1秒
    private MyMusicService myMusicService;
    private TextView MusicName;
    private TextView ArtistName;
    private TextView tvPlayedTime;
    private TextView tvTotalTime;
    private LyricView lyricView;
    private ImageView Music_Album;
    private ObjectAnimator rotateAnimator;
    File lyricFile;
    private boolean isBound=false;
    private Handler handler;
    private Runnable updateRunnable;
    private DefaultTimeBar timeBar;

    private final ServiceConnection serviceConnection=new ServiceConnection() {
        @OptIn(markerClass = UnstableApi.class) @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyMusicService.LocalBind bind=(MyMusicService.LocalBind) service;
            myMusicService=bind.getService();
            isBound=true;
            player=myMusicService.getPlayer();
            playerControlView.setPlayer(player);
            playerControlView.show();
            LyricsManager lyricsManager=new LyricsManager(player,lyricView);
            lyricsManager.startScrollingLyrics();
            setupCustomControls();
            player.addListener(new Player.Listener() {
                @Override
                public void onEvents(Player player, Player.Events events) {
                    Player.Listener.super.onEvents(player, events);

                }
                @Override
                public void onPlaybackStateChanged(int playbackState){
                    // 更新进度
                    updateProgress();

                }
                @Override
                public void onIsPlayingChanged(boolean isPlaying){
                    if (isPlaying) {
                        // 播放时开始更新进度
//                        UpdateUI();
                        startUpdatingProgress();
                    } else {
                        // 暂停时停止更新进度
                        stopUpdatingProgress();
                    }

                }
                @Override
                public void onTimelineChanged(Timeline timeline,int reson){
                    // 处理时间轴变化时的进度更新
                    updateProgress();
                }
                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason){
                    // 处理播放位置不连续的情况（例如用户跳转播放位置）
                    // 监听位置的不连续性（例如，手动或自动跳转到下一个/上一个项目）
                    updateProgress();
                }
            });
            // 强制启动定时任务
            startUpdatingProgress();

            setupCustomControls();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound=false;
            player=null;
        }
    };

    private void startUpdatingProgress() {
        // 你可以在这里设置一个定时任务来定期调用 updateProgress() 方法

        handler.post(updateRunnable);
    }
    // 停止更新进度
    private void stopUpdatingProgress() {
        handler.removeCallbacks(updateRunnable);
    }

    // 更新播放进度
    private void updateProgress() {
        if (player!= null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();

            // 如果播放器还未准备好，duration 可能会返回 C.TIME_UNSET
            if (duration == C.TIME_UNSET) {
                duration = 0;
            }

            updateTimes(currentPosition, duration);
        }
    }

    // 更新播放时间和总时长的方法
    public void updateTimes(long currentPosition, long duration) {
        tvPlayedTime.setText(formatTime(currentPosition));
        tvTotalTime.setText(formatTime(duration));

        // 设置进度条
        timeBar.setPosition(currentPosition);
        timeBar.setDuration(duration);
    }
    // 示例：在播放进度更新时调用

    private String formatTime(long timeMs) {
        int totalSeconds = (int) (timeMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_play_next);
        TransparentBar.transparentNavBar(this);
        TransparentBar.transparentStatusBar(this);

        InitView();
        UpdateUI();
        InitRotate();
        // 获取并处理最新的粘性事件
        MusicPlayPauseEvent stickyEvent = EventBus.getDefault().getStickyEvent(MusicPlayPauseEvent.class);
        if (stickyEvent != null) {
            onMusicEvent(stickyEvent);
        }

        MyMusicPlayerUpdateUI myMusicPlayerUpdateUI=EventBus.getDefault().getStickyEvent(MyMusicPlayerUpdateUI.class);
        if(myMusicPlayerUpdateUI!=null){
            onMyMusicPlayerUpdateUI(myMusicPlayerUpdateUI);
        }

        handler=new Handler(Looper.getMainLooper());
        updateRunnable=new Runnable(){

            @Override
            public void run() {
                updateProgress(); // 调用更新方法
                handler.postDelayed(this, UPDATE_INTERVAL); // 每秒更新一次
            }
        };

        Intent intent=new Intent(this,MyMusicService.class);
        bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);


    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMyMusicPlayerUpdateUI(MyMusicPlayerUpdateUI event) {
        // 在这里处理接收到的事件，例如更新UI
        UpdateUI();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMusicEvent(MusicPlayPauseEvent event) {
        if (playerControlView == null) {
            Log.e("MusicPlay", "playerControlView is null");
            return;
        }
        ImageButton playPause = playerControlView.findViewById(R.id.exo_play);
        if (playPause == null) {
            Log.e("MusicPlay", "playPause button not found");
            return;
        }
        if (event.isPlaying) {
            startRotating();
            playPause.setImageResource(R.drawable.icon_play);
        } else {
            pauseRotating();
            playPause.setImageResource(R.drawable.icon_pause);
        }
    }

    //初始化控件
    private void InitView(){
        PlayBackGround=findViewById(R.id.MusicPlayBackground_next);
        playerControlView=findViewById(R.id.PlayerControlView_next);
        MusicName=findViewById(R.id.MusicName_next);
        ArtistName=findViewById(R.id.ArtistName_next);
        Music_Album=findViewById(R.id.MusicPlayAlbum_next);
        lyricView=findViewById(R.id.custom_lyric_view_next);
        tvPlayedTime=findViewById(R.id.tv_played_time);
        tvTotalTime=findViewById(R.id.tv_total_time);
    }

    //背景的效果
    MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
            new BlurTransformation(80, 5),
            new BrightnessTransformation(1.1f)
    );
    //更新UI
    private void UpdateUI(){
        MusicName.setText(MusicHolder.getMusicName());
        ArtistName.setText(MusicHolder.getArtistName());


        if(!MusicHolder.isPlayingMode()){

            Glide.with(this)
                    .load(MusicHolder.getAlbumArt())
                    .placeholder(PlayBackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(2000)) // 设置淡入效果
                    .transform(multiTransformation)
                    .into(PlayBackGround);

            Glide.with(this)
                    .load(MusicHolder.getAlbumArt())
                    .placeholder(PlayBackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(2000)) // 设置淡入效果
                    .into(Music_Album);


            LoadLyrics(this,MusicHolder.getMusicUrl());
        }else {
            Glide.with(this)
                    .load(MusicHolder.getAlbumArtUrl())
                    .placeholder(PlayBackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(2000)) // 设置淡入效果
                    .transform(multiTransformation)
                    .into(PlayBackGround);

            LoadLyrics(this,MusicHolder.getMusicUrl());

            Glide.with(this)
                    .load(MusicHolder.getAlbumArtUrl())
                    .placeholder(PlayBackGround.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(2000)) // 设置淡入效果
                    .into(Music_Album);
        }
    }

    //加载歌词
    private void LoadLyrics(Context context,String uriString){
        if(!MusicHolder.isPlayingMode()){
            Uri musicUri = null;
            if(uriString!=null){
                musicUri=Uri.parse(uriString);
            }
            String filPath= FileUtils.getPathFromUri(context,musicUri);
            if(filPath!=null){
                String lyrics= MusicMetadataUtils.getLyricsFromFile(filPath);
                if(lyrics!=null){
                    System.out.println("Lyrics:"+lyrics);
                    SaveLyrics(this,lyrics);
                    lyricView.setLyricFile(lyricFile);
                }else {
                    System.out.println("本地音乐没有找到歌词");
                }
            }else {
                System.out.println("无法获取歌词文件路径");
            }
        }else {

            SaveLyrics(this,MusicHolder.getLyrics());
            lyricView.setLyricFile(lyricFile);
        }


    }
    //保存歌词文件
    private void SaveLyrics(Context context,String lyrics){
        if(lyrics !=null&&!lyrics.isEmpty()){
            String fileName=MusicHolder.getMusicName();
            File lrcFile= LyricsFileUtils.saveLyricsToLrcFile(context,lyrics,fileName);
            lyricFile=lrcFile;
            if (lrcFile != null) {
                // 文件创建成功
                System.out.println("歌词文件路径: " + lrcFile.getAbsolutePath());
            } else {
                System.out.println("歌词文件保存失败");
            }
        }else {
            System.out.println("歌词内容为空");
        }
    }

    private void InitRotate(){
        // 初始化 ObjectAnimator
        rotateAnimator = ObjectAnimator.ofFloat(Music_Album, "rotation", 0f, 360f);
        rotateAnimator.setDuration(30000); // 旋转一圈的时间
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE); // 无限循环
        rotateAnimator.setInterpolator(new LinearInterpolator()); // 线性插值器
    }


    // 开始旋转动画
    private void startRotating() {
        Log.d("动画","开始");
        if (rotateAnimator.isPaused()) {
            rotateAnimator.resume(); // 继续动画
        } else {
            rotateAnimator.start(); // 开始动画
        }
    }

    // 暂停旋转动画
    private void pauseRotating() {
        Log.d("动画","暂停");
        rotateAnimator.pause(); // 暂停动画
    }

    // 停止旋转动画
    private void stopRotating() {
        rotateAnimator.cancel(); // 停止动画
    }

    private void setupCustomControls() {
        ImageButton prevButton = playerControlView.findViewById(R.id.exo_prev);
        ImageButton playPauseButton = playerControlView.findViewById(R.id.exo_play);
        ImageButton nextButton = playerControlView.findViewById(R.id.exo_next);
        timeBar=playerControlView.findViewById(R.id.exo_progress);
        tvPlayedTime=playerControlView.findViewById(R.id.tv_played_time);
        tvTotalTime=playerControlView.findViewById(R.id.tv_total_time);

//        updateProgress();

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 实现上一曲逻辑
                // 如果您有播放列表，可以使用以下代码：
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                    EventBus.getDefault().post(new MyMusicPlayerSeek(true));
                }
                // 如果没有播放列表，您可能需要自己管理音频列表并手动加载上一首歌
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    player.pause();
                    playPauseButton.setImageResource(R.drawable.icon_pause);
                    pauseRotating();
                } else {
                    player.play();
                    playPauseButton.setImageResource(R.drawable.icon_play);
//                    startRotating();
                    rotateAnimator.resume();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 实现下一曲逻辑
                // 如果您有播放列表，可以使用以下代码：
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    EventBus.getDefault().post(new MyMusicPlayerSeek(false));
                }
                // 如果没有播放列表，您可能需要自己管理音频列表并手动加载下一首歌
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(serviceConnection);
        stopUpdatingProgress();
        EventBus.getDefault().unregister(this);
        isBound = false;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0,R.anim.slide_out_bottom);
    }
}