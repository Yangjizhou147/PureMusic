package com.wintercruel.puremusic.tools;

import android.media.MediaPlayer;
import android.os.Handler;

import androidx.media3.exoplayer.ExoPlayer;

import me.zhengken.lyricview.LyricView;

public class LyricsManager {

    private ExoPlayer mediaPlayer;
    private LyricView lyricView;
    private Handler handler = new Handler();

    public LyricsManager(ExoPlayer mediaPlayer, LyricView lyricView) {
        this.mediaPlayer = mediaPlayer;
        this.lyricView = lyricView;
    }

    // 开始滚动歌词
    public void startScrollingLyrics() {
        handler.post(updateLyricsRunnable);
    }

    // 停止滚动歌词
    public void stopScrollingLyrics() {
        handler.removeCallbacks(updateLyricsRunnable);
    }

    // 定期更新歌词显示
    private Runnable updateLyricsRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                // 获取当前播放的时间（以毫秒为单位）
                long currentPosition = mediaPlayer.getCurrentPosition();
                // 更新歌词视图，滚动到当前播放的时间
                lyricView.setCurrentTimeMillis(currentPosition+100);
            }
            // 每隔 100 毫秒更新一次歌词显示
            handler.postDelayed(this, 100);
        }
    };
}