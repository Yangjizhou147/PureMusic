package com.wintercruel.puremusic.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.wintercruel.puremusic.MusicListItem;
import com.wintercruel.puremusic.MusicListItemCloud;
import com.wintercruel.puremusic.R;
import com.wintercruel.puremusic.service.MyMusicService;
import com.wintercruel.puremusic.tools.PlayingMusicEvent;
import com.wintercruel.puremusic.tools.SearchPlayingMusicEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder>{

    private Context mContext;
    private List<MusicListItemCloud> mMusicListItems;


    public SearchResultAdapter(final Context context,final List<MusicListItemCloud> musicListItemClouds){
        mContext=context;
        mMusicListItems=musicListItemClouds;

    }

    @NonNull
    @Override
    public SearchResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.activity_music_list_item1,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mMusicListItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mMusicListItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView MusicImage;
        private TextView MusicName;
        private TextView ArtistName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            MusicImage = itemView.findViewById(R.id.MusicListImage1);
            MusicName = itemView.findViewById(R.id.MusicListName1);
            ArtistName = itemView.findViewById(R.id.MusicListArtistName1);
            itemView.setOnClickListener(this);
        }

        public void bind(MusicListItemCloud musicListItem) {
            Glide.with(itemView.getContext())
                    .load(musicListItem.getMusicImage())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(20))
                            .placeholder(R.drawable.music_item)) // 添加占位符
                    .into(MusicImage);

            MusicName.setText(musicListItem.getMusicName());
            ArtistName.setText(musicListItem.getArtistName());
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            Log.d("搜索结果点击位置", String.valueOf(position));
            if (position != RecyclerView.NO_POSITION) {
                // 播放缩放动画
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            // 处理点击事件
                            Intent intent = new Intent(itemView.getContext(), MyMusicService.class);
                            intent.setAction(MyMusicService.ACTION_PLAY_MUSIC);
                            intent.putExtra(MyMusicService.EXTRA_POSITION_MUSIC, position);
                            EventBus.getDefault().post(new SearchPlayingMusicEvent(position));
                            itemView.getContext().startService(intent);
                        }).start();
            }
        }
    }
}
