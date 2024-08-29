package com.wintercruel.puremusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wintercruel.puremusic.R;
import com.wintercruel.puremusic.entity.HotSearchListItem;
import com.wintercruel.puremusic.tools.SearchHot;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class HotSearchAdapter extends RecyclerView.Adapter<HotSearchAdapter.ViewHolder> {

    private Context mContext;
    private List<HotSearchListItem> mHotSearchItem;

    public HotSearchAdapter(final Context context,final List<HotSearchListItem> hotSearchListItems){
        mContext=context;
        mHotSearchItem=hotSearchListItems;

    }
    @NonNull
    @Override
    public HotSearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.top_search_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mHotSearchItem.get(position));
    }

    @Override
    public int getItemCount() {
        return mHotSearchItem.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView Number;
        private TextView Name;
        private ImageView Icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
                Number=itemView.findViewById(R.id.Number);
                Name=itemView.findViewById(R.id.HotSearchName);
                Icon=itemView.findViewById(R.id.HotSearchIcon);
            itemView.setOnClickListener(this);
        }

        public void bind(HotSearchListItem hotSearchListItem){
            Number.setText(String.valueOf(hotSearchListItem.getNumber()));
            Name.setText(hotSearchListItem.getName());
            Glide.with(itemView.getContext())
                    .load(hotSearchListItem.getIconUrl())
                    .into(Icon);

        }

        @Override
        public void onClick(View v) {
            int position=getAdapterPosition();
            if(position!=RecyclerView.NO_POSITION){
                String name=mHotSearchItem.get(position).getName();
                EventBus.getDefault().post(new SearchHot(name));

            }
        }
    }
}
