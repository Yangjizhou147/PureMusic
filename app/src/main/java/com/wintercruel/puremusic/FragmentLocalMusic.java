package com.wintercruel.puremusic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.wintercruel.puremusic.adapter.MusicListAdapter;
import com.wintercruel.puremusic.entity.Music;
import com.wintercruel.puremusic.service.MyMusicService;
import com.wintercruel.puremusic.tools.LoadLocalMusicEvent;
import com.wintercruel.puremusic.tools.LocalMusicBackEvent;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.PlayingMusicEvent;
import com.wintercruel.puremusic.utils.DBOpenHelper;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FragmentLocalMusic extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private static final int REQUEST_PERMISSION_READ_MEDIA_AUDIO = 1;
    private RecyclerView musicListView;
    private MusicListAdapter musicListAdapter;
    private List<MusicListItem> musicListItems;
    private static final int PAGE_SIZE = 12;
    private int currentPage = 0;
    private DBOpenHelper dbOpenHelper;
    private boolean isLoading = false; // 加载状态
    private int Position=0;
    private List<Music> playlist;
    private List<MediaItem> mediaItems;
    private MyMusicService musicService;
    private ExoPlayer exoPlayer;
    private boolean isBound = false;

    public FragmentLocalMusic() {
        // Required empty public constructor
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyMusicService.LocalBind binder = (MyMusicService.LocalBind) service;
            musicService = binder.getService();
            exoPlayer=musicService.getPlayer();
            isBound = true;
            Load();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };


    @Override
    public void onStart() {
        super.onStart();
        Intent intent1 = new Intent(getContext(), MyMusicService.class);
        getContext().bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);



    }

    @Override
    public void onResume(){
        super.onResume();

    }





    public static FragmentLocalMusic newInstance(String param1, String param2) {
        FragmentLocalMusic fragment = new FragmentLocalMusic();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }






    @Override
    public void onCreate(Bundle savedInstanceState) {
        playlist=new ArrayList<>();
        mediaItems=new ArrayList<>();
        super.onCreate(savedInstanceState);
        dbOpenHelper=new DBOpenHelper(getContext());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // 注册返回按钮回调
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 在这里处理返回逻辑
                MainActivity mainActivity=(MainActivity) getActivity();
                mainActivity.selectFragment(1);


            }
        });

    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_local_music, container, false);
        musicListView=view.findViewById(R.id.MusicList1);
        musicListView.setItemAnimator(new DefaultItemAnimator());
        musicListView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicListItems = new ArrayList<>();
        musicListAdapter = new MusicListAdapter(getContext(), musicListItems);
        musicListView.setAdapter(musicListAdapter);

        EventBus.getDefault().register(this);



        ImageButton imageButton=view.findViewById(R.id.MusicListEdit1);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditLocalMusic(view);
            }
        });

        ImageButton imageButton1=view.findViewById(R.id.LocalMusicBack1);
        imageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBackEventToActivity(1);
            }
        });

        // 检查权限
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION_READ_MEDIA_AUDIO);
        }
        return view;
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onLoadLocalMusicEvent(LoadLocalMusicEvent event){
        MusicHolder.setPlayingMode(false);
        if(!MusicHolder.isPlayingMode()){
            if(exoPlayer.getMediaItemCount()!=playlist.size()||exoPlayer.getMediaItemCount()==0){
                loadMusic();
            }

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void PlayingMusicPosition(PlayingMusicEvent event){
        Position=event.position;
        exoPlayer.prepare();
        exoPlayer.seekTo(Position,0);
        exoPlayer.play();

//        HandlerThread handlerThread = new HandlerThread("NetworkThread");
//        handlerThread.start();
//        Handler handler = new Handler(handlerThread.getLooper());
//
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                LoadMusic(Position);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        UpdateUI();
//                        //通知MainActivity更新UI
//                        EventBus.getDefault().post(new MusicUpdateEvent());
//                    }
//                });
//            }
//        });

    }





    private void sendBackEventToActivity(int i){
        EventBus.getDefault().post(new LocalMusicBackEvent(i));
    }

    private void loadMusic() {
        if(exoPlayer.isPlaying()){
            exoPlayer.pause();
        }

        exoPlayer.clearMediaItems();
        HandlerThread handlerThread = new HandlerThread("NetworkThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        DBOpenHelper dbHelper = new DBOpenHelper(getContext());
        Cursor cursor = dbHelper.getAllMusic();
        handler.post(new Runnable() {
            @Override
            public void run() {


                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        @SuppressLint("Range") String uri = cursor.getString(cursor.getColumnIndex("uri"));
                        @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
                        @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex("artist"));
                        @SuppressLint("Range") Long albumId = cursor.getLong(cursor.getColumnIndex("albumId"));
                        playlist.add(new Music(uri, title, artist, albumId));
                        MediaItem mediaItem = MediaItem.fromUri(uri);
                        mediaItems.add(mediaItem);
                    }
                    cursor.close();
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MyMusicService.AddPlayList(playlist);
                        MyMusicService.AddMediaItem(mediaItems);
                    }
                });
            }
        });



    }


    private void LoadPage(int page) {
        if(isLoading) return;;
        isLoading = true; // 设置加载状态
        new Thread(() -> {
            int offset = page * PAGE_SIZE;
            Cursor cursor = dbOpenHelper.getMusicPaginated(offset, PAGE_SIZE);
            if (cursor != null && cursor.moveToFirst()) {
                List<MusicListItem>newItems=new ArrayList<>();
                do {
                    @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex("uri"));
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("title"));
                    @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex("artist"));
                    @SuppressLint("Range") long albumId = cursor.getLong(cursor.getColumnIndex("albumId"));

                    Bitmap albumArt = getAlbumArt(albumId); // 获取专辑封面
                    MusicListItem item = new MusicListItem();
                    item.setMusicName(title);
                    item.setArtistName(artist);
                    item.setMusicImage(albumArt);
                    newItems.add(item);
//                    MediaItem mediaItem = MediaItem.fromUri(id);
//                    mediaItems.add(mediaItem);

                } while (cursor.moveToNext());
                cursor.close();
                getActivity().runOnUiThread(()->{

//                    MyMusicService.AddPlayList(playlist);
//
//                    MyMusicService.AddMediaItem(mediaItems);

                    int startPosition = musicListItems.size(); // 新数据的起始位置
                    musicListItems.addAll(newItems); // 添加新数据到列表中
                    musicListAdapter.notifyItemRangeInserted(startPosition, newItems.size()); // 通知适配器
                    isLoading = false; // 更新加载状态
                });
            }else {
                // 如果没有更多数据
               getActivity().runOnUiThread(() -> {
                    isLoading = false; // 更新加载状态
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_MEDIA_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                scanMusic();
            } else {
                Toast.makeText(getContext(), "需要音频权限才能扫描音乐", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void Load(){
        LoadPage(currentPage); // 加载第一页
        // 设置滑动监听器以实现分页加载
        musicListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) musicListView.getLayoutManager();
                if (layoutManager != null) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                    // 判断是否接近底部，设定接近底部的阈值，比如距离底部5项
                    if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 8 && !isLoading) {
                        // 到达底部，加载下一页
                        currentPage++;
                        LoadPage(currentPage);
                    }
                }
            }
        });
    }

    private void scanMusic() {
        musicListItems.clear();
        musicListAdapter.notifyDataSetChanged();
        ContentResolver contentResolver = requireContext().getContentResolver(); // 使用 requireContext() 获取 ContentResolver
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = contentResolver.query(musicUri, projection, selection, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            do {
                long id = cursor.getLong(idColumn);
                String uri = ContentUris.withAppendedId(musicUri, id).toString();
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                long albumId = cursor.getLong(albumIdColumn);
                //获取专辑封面
                dbOpenHelper.insertMusic(uri, title, artist, albumId);

            } while (cursor.moveToNext());
            cursor.close();

            Log.d("MainActivity", "Scanned music items: " + musicListItems.size());
        }
        currentPage = 0;
        isLoading = false;
        Load();
    }

    // 获取专辑封面的方法
    private Bitmap getAlbumArt(long albumId) {
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(artworkUri, albumId);
        ContentResolver res = requireContext().getContentResolver(); // 使用 requireContext() 获取 ContentResolver
        InputStream in = null;
        try {
            in = res.openInputStream(uri);
            return BitmapFactory.decodeStream(in);
        } catch (FileNotFoundException e) {
            // 如果没有找到专辑封面，返回一个默认图片
            return BitmapFactory.decodeResource(getResources(), R.drawable.music_item);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void EditLocalMusic(View view) {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.menu);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Window window = dialog.getWindow();
        if (window != null) {
            // 设置对话框位置
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.END;
            // 可选：设置对话框与屏幕边缘的距离
            params.x = 20; // 距离右边缘的距离（单位：像素）
            params.y = 200; // 距离上边缘的距离（单位：像素）
            window.setAttributes(params);
            // 可选：设置对话框进入和退出的动画
//            window.setWindowAnimations(R.style.DialogAnimation);
        }
        dialog.show();
        Button button=dialog.findViewById(R.id.ScanMusic);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                scanMusic();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(isBound){
            getActivity().unbindService(serviceConnection);
        }
        // 注销回调以避免内存泄漏
//        requireActivity().getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
    }

    @Override
    public void onStop(){
        super.onStop();

    }


}