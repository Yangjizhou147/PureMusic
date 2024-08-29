package com.wintercruel.puremusic;

import static android.content.Context.MODE_PRIVATE;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.logout;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.wintercruel.puremusic.NeteaseCloud.CookieExample;
import com.wintercruel.puremusic.NeteaseCloud.LoginUpdateUI;
import com.wintercruel.puremusic.adapter.PlayListAdapter;
import com.wintercruel.puremusic.tools.LoadLocalMusicEvent;
import com.wintercruel.puremusic.tools.MusicHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import okhttp3.OkHttpClient;

public class FragmentIndex extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private RecyclerView playListView;
    private PlayListAdapter playListAdapter;
    private List<PlayListItem> playListItems;
    private boolean isLogin=false;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FragmentIndex() {
        // Required empty public constructor
    }


    public static FragmentIndex newInstance(String param1, String param2) {
        FragmentIndex fragment = new FragmentIndex();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        CookieExample.initialize(getContext());
        new Thread(new Runnable() {
            @Override
            public void run() {

//                GetPlayList(getContext());
//                GetPlayList(getContext());
//                CookieExample.fetchData();
//                CookieExample.GetPlayList(getActivity());

            }
        }).start();
//        LoginTools loginTools=new LoginTools();
//        loginTools.GetUsrPlayList(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_index, container, false);
        View button=view.findViewById(R.id.LocalMusic);
        View login=view.findViewById(R.id.HeadImageSet);
        View nickname=view.findViewById(R.id.LoginName);
        View background=view.findViewById(R.id.BackGround);
        View to_search=view.findViewById(R.id.SearchMusic);
        playListView=view.findViewById(R.id.PlayList);
        playListView.setItemAnimator(new DefaultItemAnimator());
        playListView.setLayoutManager(new LinearLayoutManager(getContext()));
        playListItems=new ArrayList<>();
        playListAdapter=new PlayListAdapter(getContext(),playListItems);
        playListView.setAdapter(playListAdapter);
        LoadUsers((ImageButton) login, (TextView) nickname, (ImageView) background);

        LoadPlayList(requireContext());
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(FragmentIndex.this.getActivity(),
//                        new String[]{Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION},
//                        MODE_PRIVATE);
//            }
//        }




        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity=(MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.selectFragment(3);
                }
                MusicHolder.setPlayingMode(false);
                EventBus.getDefault().postSticky(new LoadLocalMusicEvent());
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLogin){
//                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog);
//                    // 底部弹出的布局
//                    View bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.pop_window, null);
//                    bottomSheetDialog.setContentView(bottomView);
//                    bottomSheetDialog.show();
//
//                    Button close=bottomView.findViewById(R.id.ClosePopWindow);
                    showBottomSheetDialog();

                }else {
                    Intent intent=new Intent(getContext(), Login.class);
                    startActivity(intent);
                }


            }
        });

        to_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity=(MainActivity) getActivity();
                mainActivity.selectFragment(4);


            }
        });

        return view;
    }


    private void LoadPlayList(Context context){
        List<PlayListItem> Items=new ArrayList<>();
        SharedPreferences sharedPreferences= context.getSharedPreferences("playList",MODE_PRIVATE);
        String playListJSONDate= sharedPreferences.getString("playList",null);
        if(playListJSONDate!=null){
            try {
                JSONObject jsonObject=new JSONObject(playListJSONDate);
                JSONArray playlistArray=jsonObject.getJSONArray("playlist");

                String coverImgUrl=null;
                String name=null;
                String trackCount=null;

                // 遍历每个歌单
                for (int i = 0; i < playlistArray.length(); i++) {
                    JSONObject playlistObject = playlistArray.getJSONObject(i); // 获取每个歌单对象
                    coverImgUrl = playlistObject.getString("coverImgUrl"); // 获取封面图 URL
                    name = playlistObject.getString("name"); // 获取歌单名称
                    trackCount=playlistObject.getString("trackCount");//歌单音乐数量

                    PlayListItem Item=new PlayListItem();
                    Item.setPlayListName(name);
                    Item.setImgUrl(coverImgUrl);
                    Item.setTrackCount(trackCount);
                    Items.add(Item);
                }


            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        playListItems.clear();
        playListAdapter.notifyDataSetChanged();
        playListItems.addAll(Items);
        int startPosition=playListItems.size();
        playListAdapter.notifyItemChanged(startPosition,Items.size());
    }


    private void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog);

        // Inflate the layout for the bottom sheet dialog
        View bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.pop_window, null);
        bottomSheetDialog.setContentView(bottomView);

        // Find the close button in the bottom sheet layout
        Button close = bottomView.findViewById(R.id.ClosePopWindow);
        Button logout=bottomView.findViewById(R.id.Logout);

        // Set an OnClickListener for the close button
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the bottom sheet dialog when the button is clicked
                bottomSheetDialog.dismiss();
            }
        });

        //退出登录
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                logout(getContext());
                isLogin=false;
                ImageButton imageButton=getView().findViewById(R.id.HeadImageSet);
                TextView textView=getView().findViewById(R.id.LoginName);
                ImageView imageView=getView().findViewById(R.id.BackGround);

                imageButton.setImageResource(R.drawable.head_image);
                textView.setText("登录");
                imageView.setImageResource(R.drawable.background1);
//                LoadUsers((ImageButton) getView().findViewById(R.id.HeadImageSet),
//                        (TextView) getView().findViewById(R.id.LoginName),
//                        (ImageView) getView().findViewById(R.id.BackGround));
            }
        });




        // Show the bottom sheet dialog
        bottomSheetDialog.show();
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        LoginUpdateUI stickyEvent = EventBus.getDefault().getStickyEvent(LoginUpdateUI.class);
        if (stickyEvent != null) {
            onLoginUpdateUI(stickyEvent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginUpdateUI(LoginUpdateUI event) {
        // 调用 LoadUsers 方法更新 UI
        LoadUsers((ImageButton) getView().findViewById(R.id.HeadImageSet),
                (TextView) getView().findViewById(R.id.LoginName),
                (ImageView) getView().findViewById(R.id.BackGround));

        LoadPlayList(requireContext());


    }


    private void LoadUsers(ImageButton HeadImage, TextView Nickname, ImageView Background){
        SharedPreferences sharedPreferences = this.getContext().getSharedPreferences("User", MODE_PRIVATE);
        String avatarUrl=sharedPreferences.getString("avatarUrl",null);
        String nickname=sharedPreferences.getString("nickname",null);
        String background=sharedPreferences.getString("backgroundUrl",null);

        if(avatarUrl!=null&&nickname!=null&&background!=null){
            isLogin =true;
            Glide.with(getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.head_image)
                    .into(HeadImage);
//            HeadImage.setImageURI(Uri.parse(avatarUrl));
            Glide.with(getContext())
                    .load(background)
                    .placeholder(R.drawable.background1)
                    .into(Background);


            Nickname.setText(nickname);
//            Background.setImageURI(Uri.parse(background));
        }

    }




}