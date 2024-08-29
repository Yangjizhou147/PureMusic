package com.wintercruel.puremusic;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.LocalMusicBackEvent;

import com.wintercruel.puremusic.tools.MusicPlayPauseEvent;
import com.wintercruel.puremusic.tools.MusicUpdateEvent;
import com.wintercruel.puremusic.tools.MusicUpdateEventIndexToPlay;
import com.wintercruel.puremusic.tools.TransparentBar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 2;
    private final Fragment fragment_index=new FragmentIndex();
    private final Fragment fragment_find_music=new FragmentFindMusic();
    private final Fragment fragment_local_music=new FragmentLocalMusic();
    private final Fragment fragment_music_search=new FragmentMusicSearch();
    private FragmentManager fragmentManager;

    private ImageButton floatingButton;
    //底部菜单栏
    private ImageButton index;
    private ImageButton findMusic;
    private TextView index_text;
    private TextView findMusic_text;
    private BroadcastReceiver receiver;
    private ObjectAnimator rotateAnimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        MusicUpdateEvent stickyEvent = EventBus.getDefault().getStickyEvent(MusicUpdateEvent.class);
        if (stickyEvent != null) {
            onMusicUpdateEvent(stickyEvent);
        }
        TransparentBar.transparentNavBar(this);
        TransparentBar.transparentStatusBar(this);
        initView();
        initFragment();
        selectFragment(1);
        index.setImageResource(R.drawable.me_b);
        index_text.setTextColor(getResources().getColor(R.color.button_b));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @OptIn(markerClass = UnstableApi.class) @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, MusicPlayNext.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_bottom,R.anim.slide_out_top);
                EventBus.getDefault().postSticky(new MusicUpdateEventIndexToPlay());
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
        }

        InitRotate();
    }




    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMusicUpdateEvent(MusicUpdateEvent event) {

        if(!MusicHolder.isPlayingMode()){
            // 在这里更新 UI
            Log.d("BroadcastReceiver", "接收到专辑封面");
            Bitmap AlbumImage= MusicHolder.getAlbumArt();
            if(AlbumImage!=null){
//            floatingButton.setImageBitmap(AlbumImage);
                Glide.with(MainActivity.this)
                        .load(AlbumImage)
                        .placeholder(floatingButton.getDrawable())  // 使用当前的图片作为占位符
                        .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                        .into(floatingButton);
//            startRotating();
            }else {
                Glide.with(MainActivity.this)
                        .load(R.drawable.head_image)
                        .placeholder(floatingButton.getDrawable())  // 使用当前的图片作为占位符
                        .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                        .into(floatingButton);
//            startRotating();
            }
        }else {
            Glide.with(MainActivity.this)
                    .load(MusicHolder.getAlbumArtUrl())
                    .placeholder(floatingButton.getDrawable())  // 使用当前的图片作为占位符
                    .transition(DrawableTransitionOptions.withCrossFade(300)) // 设置淡入效果
                    .into(floatingButton);
        }


    }



    private void InitRotate(){
        // 初始化 ObjectAnimator
        rotateAnimator = ObjectAnimator.ofFloat(floatingButton, "rotation", 0f, 360f);
        rotateAnimator.setDuration(15000); // 旋转一圈的时间
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void backEvent(LocalMusicBackEvent event){
        int Event=event.event;
        Log.d("接收到事件","返回");
        selectFragment(Event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicUpdateEvent(MusicPlayPauseEvent event) {
        // 在这里更新 UI
        if(event.isPlaying){
            startRotating();
        }else {
            pauseRotating();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }



    @SuppressLint("ResourceAsColor")
    @Override
    public void onClick(View view){
        reStartButton();
        switch (view.getId())
        {
            case R.id.IndexMe:
                selectFragment(1);
                index.setImageResource(R.drawable.me_b);
                index_text.setTextColor(getResources().getColor(R.color.button_b));
                break;
            case R.id.IndexMusic:
                selectFragment(2);
                findMusic.setImageResource(R.drawable.music_bottom_b);
                findMusic_text.setTextColor(getResources().getColor(R.color.button_b));
                break;
            default:
                break;

        }
    }

    private void reStartButton(){
        //未点击状态
        index.setImageResource(R.drawable.me);
        index_text.setTextColor(getResources().getColor(R.color.black));
        findMusic.setImageResource(R.drawable.music_bottom);
        findMusic_text.setTextColor(getResources().getColor(R.color.black));
    }

    private void initView(){
        index=findViewById(R.id.IndexMe);
        index.setOnClickListener(this);
        index_text=findViewById(R.id.IndexMeText);
        findMusic=findViewById(R.id.IndexMusic);
        findMusic.setOnClickListener(this);
        findMusic_text=findViewById(R.id.IndexMusicText);
        floatingButton=findViewById(R.id.RoundButton);

    }

    private void initFragment(){
        fragmentManager=getSupportFragmentManager();
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        transaction.add(R.id.frame_content,fragment_index);
        transaction.add(R.id.frame_content,fragment_find_music);
        transaction.add(R.id.frame_content,fragment_local_music);
        transaction.add(R.id.frame_content,fragment_music_search);
        transaction.commit();
    }

    private void hideView(FragmentTransaction transaction){
        transaction.hide(fragment_index);
        transaction.hide(fragment_find_music);
        transaction.hide(fragment_local_music);
        transaction.hide(fragment_music_search);
    }

    public void selectFragment(int i){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        hideView(transaction);
        switch (i){
            case 1:
                transaction.show(fragment_index);
                break;
            case 2:
                transaction.show(fragment_find_music);
                break;
            case 3:
                transaction.show(fragment_local_music);
                break;
            case 4:
                transaction.show(fragment_music_search);
                break;
            default:
                break;
        }
        transaction.commit();
    }



}