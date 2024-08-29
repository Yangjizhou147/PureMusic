package com.wintercruel.puremusic.adapter;

import static android.content.Context.MODE_PRIVATE;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetPlayListMusic;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wintercruel.puremusic.CloudMusic;
import com.wintercruel.puremusic.PlayListItem;
import com.wintercruel.puremusic.R;
import com.wintercruel.puremusic.tools.CloudMusicLoadEvent;
import com.wintercruel.puremusic.tools.GetCloudMusicEvent;
import com.wintercruel.puremusic.tools.MusicHolder;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.ViewHolder>{

    private Context mContext;
    private List<PlayListItem> mPlayListItem;

    public PlayListAdapter(final Context context,final List<PlayListItem> playListItems){
        mContext=context;
        mPlayListItem=playListItems;
    }



    @NonNull
    @Override
    public PlayListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.playlist_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayListAdapter.ViewHolder holder, int position) {
        holder.bind(mPlayListItem.get(position));
    }

    @Override
    public int getItemCount() {
        return mPlayListItem.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView PlayListImg;
        private TextView PlayListName;
        private TextView TrackCount;





        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            PlayListImg=itemView.findViewById(R.id.PlayListImg);
            PlayListName=itemView.findViewById(R.id.PlayListName);
            TrackCount=itemView.findViewById(R.id.MusicCount);
            itemView.setOnClickListener(this);

        }

        public void bind(PlayListItem playListItem){
            Log.d("歌单图片地址：",playListItem.getImgUrl());
            Glide.with(itemView.getContext())
                    .load(playListItem.getImgUrl())
                    .placeholder(R.drawable.head_image)
                    .into(PlayListImg);
            PlayListName.setText(playListItem.getPlayListName());
            TrackCount.setText(playListItem.getTrackCount()+"首");
        }

        @Override
        public void onClick(View v) {

            int position=getPosition();
            String PlayListId=null;
            Log.d("歌单点击位置：", String.valueOf(position));

            SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("playList", MODE_PRIVATE);
            String playListJSONData = sharedPreferences.getString("playList", null);


            if (playListJSONData == null) {
                Log.e("Error", "No playlist data found in SharedPreferences");
                return;  // 处理没有播放列表数据的情况
            }

            try {
                JSONObject jsonObject = new JSONObject(playListJSONData);
                JSONArray playlistArray = jsonObject.getJSONArray("playlist");

                // 确保 position 在数组范围内
                if (position < 0 || position >= playlistArray.length()) {
                    Log.e("Error", "Invalid position: " + position);
                    return; // 处理无效位置
                }

                JSONObject playlistObject = playlistArray.getJSONObject(position); // 获取每个歌单对象
                String playListId = playlistObject.getString("id");

                MusicHolder.setPlayListId(playListId);


                PlayListId=playListId;
                boolean getXML=false;
                getXML=isSharedPreferencesFileExists(itemView.getContext(),playListId);
                if (getXML) {
                    // 如果缓存存在，直接使用缓存的数据
                    Log.d("存在歌单数据", "不进行请求: " + playListId);
                    // 处理 cachedMusicList，如解析并显示音乐列表
                    // 例如: parseAndDisplayMusicList(cachedMusicList);

                    EventBus.getDefault().postSticky(new GetCloudMusicEvent());

                } else {
                    // 如果缓存不存在，进行 API 请求
                    Log.d("不存在歌单数据", "进行请求: " + playListId);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // 获取音乐列表
                            GetPlayListMusic(playListId, itemView.getContext());

                        }
                    }).start();
                }
            } catch (JSONException e) {
                Log.e("Error", "Failed to parse playlist JSON", e);
            }

            if(position !=RecyclerView.NO_POSITION){

                String finalPlayListId = PlayListId;
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(()->{
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            Intent intent=new Intent(itemView.getContext(), CloudMusic.class);
                            intent.putExtra("PlayListId", finalPlayListId);
                            itemView.getContext().startActivities(new Intent[]{intent});

                            EventBus.getDefault().post(new CloudMusicLoadEvent());

                        }).start();

            }


        }
    }


    private boolean isSharedPreferencesFileExists(Context context, String playListId) {
        // Construct the shared preferences file name
        String prefsFileName = "PlayList" + playListId + ".xml"; // SharedPreferences 文件以 .xml 结尾
        // Get the directory for shared preferences
        File sharedPrefsDir = new File(context.getApplicationInfo().dataDir + "/shared_prefs");
        // Create the file object for the specific SharedPreferences file
        File sharedPrefsFile = new File(sharedPrefsDir, prefsFileName);

        // Check if the file exists
        boolean exists = sharedPrefsFile.exists();
        Log.d("Debug", "Checking for SharedPreferences file: " + sharedPrefsFile.getAbsolutePath() + ", exists: " + exists);
        return exists;
    }

}
