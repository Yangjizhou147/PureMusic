package com.wintercruel.puremusic.service;

import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetLyrics;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;


import com.wintercruel.puremusic.MainActivity;
import com.wintercruel.puremusic.R;
import com.wintercruel.puremusic.entity.Music;
import com.wintercruel.puremusic.tools.CloudMusicUpdateUiEvent;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.MusicPlayPauseEvent;
import com.wintercruel.puremusic.tools.MusicUpdateEvent;
import com.wintercruel.puremusic.tools.MyMusicPlayerSeek;
import com.wintercruel.puremusic.tools.MyMusicPlayerUpdateUI;
import com.wintercruel.puremusic.tools.PlayingMusicEvent;
import com.wintercruel.puremusic.utils.DBOpenHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MyMusicService extends Service {
    public static final String ACTION_PLAY_MUSIC="com.wintercruel.puremusic.action.PLAYCLOUD";
    public static final String EXTRA_POSITION_MUSIC="com.wintercruel.puremusic.extra.POSITIONCLOUD";
    private static final String TAG="MyMusicService";
    private static final String CHANNEL_ID="music_service_channel";
    private static ExoPlayer player;
    private static List<Music> playlist;
    private static List<MediaItem> mediaItems;
    private final IBinder binder=new LocalBind();
    private static int Position;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static String PlayListNowId;
    private int currentTrackIndex = -1; // 当前曲目索引
    private static final int MY_MUSIC_NOTIFICATION_ID = 1;
    private static final String MY_MUSIC_CHANNEL_ID = "music_playback_channel";
    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat mediaControllerCompat;


    @OptIn(markerClass = UnstableApi.class) @Override
    public void onCreate(){
        super.onCreate();
        playlist = new ArrayList<>();
        mediaItems=new ArrayList<>();
        player=new ExoPlayer.Builder(this).build();
        // 创建 MediaSessionCompat
        // 创建 MediaSessionCompat 实例
        mediaSessionCompat = new MediaSessionCompat(this, "MusicService");

        EventBus.getDefault().register(this);
        player.addListener(new Player.Listener() {
            @Override
            public void onEvents(Player player, Player.Events events) {
                Player.Listener.super.onEvents(player, events);
            }
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // 监听媒体项的切换
                Log.d("ExoPlayer", "切换到新的媒体项: " + mediaItem + "，原因: " + reason);
                Log.d("MediaDateChanged","监测到媒体数据发生改变");
                currentTrackIndex=player.getCurrentWindowIndex();

                new Thread(()->{
                    if(MusicHolder.isPlayingMode()){
                        LoadMusic(currentTrackIndex);
                    }else {
                        handler.post(() -> {

                            MediaMetadataCompat mediaMetadataCompat=new MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MusicHolder.getMusicName())
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MusicHolder.getArtistName())
                                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album Name")
                                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,MusicHolder.getAlbumArt())
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,player.getDuration())
                                    .build();
                            mediaSessionCompat.setMetadata(mediaMetadataCompat);
                            EventBus.getDefault().postSticky(new MyMusicPlayerUpdateUI());
                            Log.d("切换音乐发布粘性事件：", "发布");
                            //通知MainActivity更新UI
                            EventBus.getDefault().post(new MusicUpdateEvent());
                            EventBus.getDefault().postSticky(new CloudMusicUpdateUiEvent());
//                            ShowNotification();
                        });
                    }
                }).start();

            }
            @Override
            public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
                if(!MusicHolder.isPlayingMode()){
                    // 检查是否包含有效的元数据
                    updateAlbumArt(mediaMetadata);
                }

                new Thread(()->{
                    if(MusicHolder.isPlayingMode()){
                        LoadMusic(currentTrackIndex);
                    }else {
                        handler.post(() -> {
                            MediaMetadataCompat mediaMetadataCompat=new MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MusicHolder.getMusicName())
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MusicHolder.getArtistName())
                                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album Name")
                                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,MusicHolder.getAlbumArt())
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,player.getDuration())
                                    .build();
                            mediaSessionCompat.setMetadata(mediaMetadataCompat);
                            EventBus.getDefault().postSticky(new MyMusicPlayerUpdateUI());
                            Log.d("切换音乐发布粘性事件：", "发布");
                            //通知MainActivity更新UI
                            EventBus.getDefault().post(new MusicUpdateEvent());
                            EventBus.getDefault().postSticky(new CloudMusicUpdateUiEvent());
//                            ShowNotification();
                        });
                    }
                }).start();


            }



            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason){

                EventBus.getDefault().postSticky(new MusicPlayPauseEvent(playWhenReady));

            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // 当播放状态变化时更新 MediaSession 的状态
                switch (playbackState) {
                    case Player.STATE_READY:
                        if (player.getPlayWhenReady()) {
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                        } else {
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                        }
                        break;
                    case Player.STATE_ENDED:
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
                        break;
                    case Player.STATE_IDLE:
                        updatePlaybackState(PlaybackStateCompat.STATE_NONE);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e("MyMusicService", "Player error: " + error.getMessage());
                Toast.makeText(MyMusicService.this, "音源加载出错", Toast.LENGTH_SHORT).show();

                updatePlaybackState(PlaybackStateCompat.STATE_NONE);
            }



        });




        // 设置回调
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                // 开始播放音乐
                // update playback state
                if (player != null) {
                    player.play();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }

            @Override
            public void onPause() {
                super.onPause();
                // 暂停音乐
                if (player != null) {
                    player.pause();
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                }
            }

            @Override
            public void onStop() {
                super.onStop();
                // 停止音乐
                if (player != null) {
                    player.stop();
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
                    mediaSessionCompat.release();
                    stopSelf();
                }
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                if (player != null) {
                    player.seekTo(pos);
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                if (player != null) {
                    // 切换到下一首曲目
                    player.seekToNext();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                if (player != null) {
                    // 切换到上一首曲目
                    player.seekToPrevious();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                }
            }
        });

        // 允许媒体按钮控制
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSessionCompat.setActive(true);
        // 设置初始的播放状态
        updatePlaybackState(PlaybackStateCompat.STATE_NONE);
        // 激活 MediaSession
        mediaSessionCompat.setActive(true);
    }

    private void updateAlbumArt(MediaMetadata mediaMetadata) {

        String musicName = (String) mediaMetadata.title;
        String artist = (String) mediaMetadata.artist;
        byte[] albumArt = mediaMetadata.artworkData;
        int position= player.getCurrentMediaItemIndex();


        Log.d("检查是否为null", "musicName: " + musicName + ", artist: " + artist + ", albumArt: " + (albumArt != null ? "not null" : "null"));

        if (albumArt == null) {
            Bitmap bitmap=null;
            MusicHolder.setAlbumArt(bitmap);
            Log.e(TAG, "albumArt 是空值");
        } else {
            Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            MusicHolder.setAlbumArt(bitmap);
        }

        MusicHolder.setMusicUrl(playlist.get(position).getUri());
        MusicHolder.setMusicName(musicName);
        MusicHolder.setArtistName(artist);

        EventBus.getDefault().post(new MusicUpdateEvent());
    }

    private void updatePlaybackState(int state) {
        long position = player.getCurrentPosition();
        long bufferedPosition = player.getBufferedPosition();
        long duration = player.getDuration();  // 获取总时长
        float playbackSpeed = player.getPlaybackParameters().speed;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(state, position, playbackSpeed)
                .setBufferedPosition(bufferedPosition)
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO // 确保启用拖动
                );

        // 如果播放器支持获取总时长，进一步设置持续时间
        stateBuilder.setExtras(Bundle.EMPTY);  // 确保 Extras 存在


        mediaSessionCompat.setPlaybackState(stateBuilder.build());
    }


    public static void AddMediaItem(List<MediaItem> mediaItem){
        mediaItems=mediaItem;
        player.addMediaItems(mediaItems);
    }

    public static void AddPlayList(List<Music> Playlist){
        playlist=Playlist;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        super.onStartCommand(intent,flags,startId);
//
//        ShowNotification();

        return START_STICKY;
    }

//    private void ShowNotification(){
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle(MusicHolder.getMusicName())
//                .setContentText(MusicHolder.getArtistName())
//                .setSmallIcon(R.drawable.head_image)
//                .build();
//
//        startForeground(1, notification);
//    }


    private void createNotificationChannel() {
        // 仅在 Android 8.0 及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Music Playback";
            String description = "Channel for music playback controls";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(MY_MUSIC_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // 注册通知渠道
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void SeekMusic(MyMusicPlayerSeek event) {
        // 在这里处理接收到的事件，例如更新UI
        Log.d("接收到切换音乐事件：","接收");
//        Position=player.getCurrentMediaItemIndex();
//        if(event.seek){
//            Log.d("切换后的音乐位置：", String.valueOf(Position));
//            new Thread(()->{
//                LoadMusic(Position);
//
//            }).start();
//
//        }else {
//
//            Log.d("切换后的音乐位置：", String.valueOf(Position));
//            new Thread(()->{
//                LoadMusic(Position);
//            }).start();
//        }
    }


    private void LoadMusic(int position){
        String Name="PlayList"+MusicHolder.getPlayListId();
        SharedPreferences sharedPreferences = this.getSharedPreferences(Name, MODE_PRIVATE);
        String MusicDate = sharedPreferences.getString("PlayList", null);

        if(MusicDate==null||MusicHolder.isIsSearchMode()){
            MusicDate=MusicHolder.getSearchResult();
        }

        Log.d("音乐搜索的模式：", String.valueOf(MusicHolder.isIsSearchMode()));

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
            Bitmap bitmap=getBitmapFromURL(MusicHolder.getAlbumArtUrl());

            String id=musicData.getString("id");
            String lyrics=GetLyrics(getApplicationContext(),id);
            MusicHolder.setLyrics(lyrics);
            // 确保在主线程中发布事件
            handler.post(() -> {
                MusicHolder.setArtistName(artistNames.toString());

                MediaMetadataCompat mediaMetadataCompat=new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MusicHolder.getMusicName())
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MusicHolder.getArtistName())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album Name")
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,bitmap)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,player.getDuration())
                        .build();


                mediaSessionCompat.setMetadata(mediaMetadataCompat);


                EventBus.getDefault().postSticky(new MyMusicPlayerUpdateUI());
                Log.d("切换音乐发布粘性事件：", "发布");
                //通知MainActivity更新UI
                EventBus.getDefault().post(new MusicUpdateEvent());
                EventBus.getDefault().postSticky(new CloudMusicUpdateUiEvent());
//                ShowNotification();
            });


        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    private static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release(); // 释放 ExoPlayer 资源
        }
        mediaSessionCompat.release();
        EventBus.getDefault().unregister(this);
    }



    public MyMusicService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;

    }


    public class LocalBind extends Binder{
        public MyMusicService getService(){
            return MyMusicService.this;
        }
    }

    public ExoPlayer getPlayer(){
        return player;
    }

    public static void SetNowPlayListId(String ID){
        PlayListNowId=ID;
    }

    public static String GetNowPlayListId(){
//        System.out.println("获取到的歌单ID："+PlayListNowId.toString());
        return PlayListNowId;
    }




}