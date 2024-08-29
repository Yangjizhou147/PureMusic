package com.wintercruel.puremusic;

import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetHotSearch;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.GetLyrics;
import static com.wintercruel.puremusic.NeteaseCloud.CookieExample.client;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Printer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.wintercruel.puremusic.NeteaseCloud.CookieExample;
import com.wintercruel.puremusic.adapter.HotSearchAdapter;
import com.wintercruel.puremusic.adapter.MusicListAdapterCloud;
import com.wintercruel.puremusic.adapter.SearchResultAdapter;
import com.wintercruel.puremusic.entity.HotSearchListItem;
import com.wintercruel.puremusic.entity.MusicCloud;
import com.wintercruel.puremusic.net.server;
import com.wintercruel.puremusic.service.MyMusicService;
import com.wintercruel.puremusic.tools.MusicHolder;
import com.wintercruel.puremusic.tools.MusicUpdateEvent;
import com.wintercruel.puremusic.tools.MyMusicPlayerUpdateUI;
import com.wintercruel.puremusic.tools.PlayingMusicEvent;
import com.wintercruel.puremusic.tools.SearchHot;
import com.wintercruel.puremusic.tools.SearchPlayingMusicEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentMusicSearch#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentMusicSearch extends Fragment {

    private RecyclerView searchResult;
    private RecyclerView hotSearch;
    private TextView hotSearchText;
    private boolean hotSearchIsShow=false;
    private ImageButton showHotSearch;
    private SearchResultAdapter musicListAdapterCloud;
    private HotSearchAdapter hotSearchAdapter;
    private List<HotSearchListItem>hotSearchListItems;
    private List<MusicListItemCloud>musicListItemClouds;
    private Button Search;
    private SearchView SearchMusic;
    private List<MusicCloud>playList;
    private List<MediaItem>mediaItems;
    private MyMusicService musicService;
    private ExoPlayer exoPlayer;
    private boolean isBound=false;
    private int Position=0;
    private String requireResult;




    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FragmentMusicSearch() {
        // Required empty public constructor
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyMusicService.LocalBind binder = (MyMusicService.LocalBind) service;
            musicService = binder.getService();

            exoPlayer=musicService.getPlayer();
            isBound = true;


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    public static FragmentMusicSearch newInstance(String param1, String param2) {
        FragmentMusicSearch fragment = new FragmentMusicSearch();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        playList=new ArrayList<>();
        mediaItems=new ArrayList<>();

        // 注册返回按钮回调
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 在这里处理返回逻辑
                MainActivity mainActivity=(MainActivity) getActivity();
                mainActivity.selectFragment(1);
                MusicHolder.setIsSearchMode(false);

            }
        });


        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view=inflater.inflate(R.layout.fragment_music_search,container,false);

        //热搜列表
        hotSearchText=view.findViewById(R.id.HotSearchText);
        showHotSearch=view.findViewById(R.id.ShowHotSearch);
        hotSearch=view.findViewById(R.id.HotSearch);
        hotSearch.setItemAnimator(new DefaultItemAnimator());
        hotSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        hotSearchListItems=new ArrayList<>();
        hotSearchAdapter=new HotSearchAdapter(getContext(),hotSearchListItems);
        hotSearch.setAdapter(hotSearchAdapter);


        //搜索结果
        searchResult=view.findViewById(R.id.SearchResult);
        searchResult.setItemAnimator(new DefaultItemAnimator());
        searchResult.setLayoutManager(new LinearLayoutManager(getContext()));
        musicListItemClouds=new ArrayList<>();
        musicListAdapterCloud=new SearchResultAdapter(getContext(),musicListItemClouds);
        searchResult.setAdapter(musicListAdapterCloud);


        Search=view.findViewById(R.id.Search);
        SearchMusic=view.findViewById(R.id.SearchText);

        hotSearchText.setVisibility(View.GONE);

        Intent intent=new Intent(requireContext(),MyMusicService.class);
        requireContext().bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);




        SearchMusic.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                hotSearch.setVisibility(View.GONE);
                hotSearchText.setVisibility(View.GONE);
                searchResult.setVisibility(View.VISIBLE);

                new Thread(()->{

                    LoadSearchResult(query);
                    MusicHolder.setIsSearchMode(true);

                }).start();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hotSearch.setVisibility(View.GONE);
                hotSearchText.setVisibility(View.GONE);
                searchResult.setVisibility(View.VISIBLE);

                new Thread(()->{

                    String query=SearchMusic.getQuery().toString();
                    LoadSearchResult(query);
                    MusicHolder.setIsSearchMode(true);

                }).start();

            }
        });

        showHotSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(hotSearchIsShow){
                    hotSearch.setVisibility(View.GONE);
                    hotSearchText.setVisibility(View.GONE);

                    searchResult.setVisibility(View.VISIBLE);

                    hotSearchIsShow=false;
                }else {
                    hotSearch.setVisibility(View.VISIBLE);
                    hotSearchText.setVisibility(View.VISIBLE);

                    LoadHotSearch();

                    searchResult.setVisibility(View.GONE);

                    hotSearchIsShow=true;
                }

            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    @Subscribe
    public void onUserRegistered(SearchHot event) {
        hotSearch.setVisibility(View.GONE);
        hotSearchText.setVisibility(View.GONE);

        searchResult.setVisibility(View.VISIBLE);

        new Thread(()->{

            LoadSearchResult(event.name);
            MusicHolder.setIsSearchMode(true);

        }).start();

    }


    private void LoadHotSearch(){
        HandlerThread handlerThread = new HandlerThread("NetworkThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(()->{
           String HotSearch= GetHotSearch(requireContext());
           if(HotSearch!=null){
               try {
                   JSONObject jsonObject=new JSONObject(HotSearch);
                   JSONArray jsonArray=jsonObject.getJSONArray("data");

                   List<HotSearchListItem> hotSearchListItems1=new ArrayList<>();

                   for(int i=0;i<jsonArray.length();i++){
                       JSONObject jsonObject1=jsonArray.getJSONObject(i);

                       String searchWord=jsonObject1.getString("searchWord");
                       String url=jsonObject1.getString("iconUrl");

                       HotSearchListItem hotSearchListItem=new HotSearchListItem();
                       hotSearchListItem.setName(searchWord);
                       hotSearchListItem.setIconUrl(url);
                       hotSearchListItem.setNumber(i+1);

                       hotSearchListItems1.add(hotSearchListItem);

                   }

                   getActivity().runOnUiThread(() -> {
                       hotSearchListItems.clear();
                       hotSearchAdapter.notifyDataSetChanged();
                       hotSearchListItems.addAll(hotSearchListItems1);
                       int position=hotSearchListItems.size();
                       hotSearchAdapter.notifyItemChanged(position,hotSearchListItems1.size());
                   });


               } catch (JSONException e) {
                   Toast.makeText(requireContext(),"热搜获取失败",Toast.LENGTH_LONG).show();
               }
           }else {

               Toast.makeText(requireContext(),"热搜获取失败,请检查网络连接",Toast.LENGTH_LONG).show();
           }

        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void PlayingMusicPosition(SearchPlayingMusicEvent event){

        MusicHolder.setIsSearchMode(true);

        HandlerThread handlerThread = new HandlerThread("NetworkThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        MusicHolder.setPlayingMode(true);
        MyMusicService.SetNowPlayListId("00000000");


        exoPlayer.clearMediaItems();
//
        LoadMusicBackground();

        handler.post(new Runnable() {
            @Override
            public void run() {

                LoadMusic(Position);
                Log.d("扫描音乐点击位置：", String.valueOf(Position));

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Position=event.position;
                        exoPlayer.prepare();
                        exoPlayer.seekTo(Position,0);
                        exoPlayer.play();
                        //通知MainActivity更新UI
                        EventBus.getDefault().post(new MusicUpdateEvent());
                    }
                });
            }
        });

    }

    //加载正在播放的音乐信息
    private void LoadMusic(int position){

        try {
            JSONObject jsonObject=new JSONObject(requireResult);
            JSONObject jsonObject1=jsonObject.getJSONObject("result");



            JSONArray musicArray=jsonObject1.getJSONArray("songs");
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

            String lyrics=GetLyrics(getContext(),id);
            MusicHolder.setLyrics(lyrics);
            getActivity().runOnUiThread(()->{
                EventBus.getDefault().postSticky(new MyMusicPlayerUpdateUI());
                Log.d("切换音乐发布粘性事件：","发布");
            });

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void LoadMusicBackground(){
        if(MusicHolder.isPlayingMode()&&MusicHolder.isIsSearchMode()){
            HandlerThread handlerThread=new HandlerThread("NetworkThread");
            handlerThread.start();
            Handler handler=new Handler(handlerThread.getLooper());

            handler.post(new Runnable() {
                @Override
                public void run() {
                    LoadMusicToPlayList();
                }
            });

        }

    }


    private void LoadMusicToPlayList(){
        if(requireResult!=null){
            try {
                JSONObject jsonObject=new JSONObject(requireResult);
                JSONObject jsonObject1=jsonObject.getJSONObject("result");
                JSONArray jsonArray=jsonObject1.getJSONArray("songs");
                StringBuilder idBuilder=new StringBuilder();

                for(int i=0;i<jsonArray.length();i++){
                    JSONObject music=jsonArray.getJSONObject(i);
                    String id=music.getString("id");
                    if(i>0){
                        idBuilder.append(",");
                    }
                    idBuilder.append(id);
                }

                String combinedUrl="/song/url?id="+idBuilder.toString();
                // 调用方法获取音乐流地址
                fetchMusicUrls(combinedUrl);

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 新增方法：根据合并的 URL 获取音乐流地址
    private void fetchMusicUrls(String combinedUrl) {
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

                MediaItem mediaItem=MediaItem.fromUri(url);
                mediaItems.add(mediaItem);
                // 在 UI 线程中添加媒体项
            }

            getActivity().runOnUiThread(() -> {

                MyMusicService.AddMediaItem(mediaItems);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    private void LoadSearchResult(String SearchText) {
        String result = CookieExample.SearchMusic(requireActivity(), SearchText);
        requireResult=result;
        List<MusicListItemCloud> Items = new ArrayList<>();
        if (result != null && !result.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                // 获取嵌套的 "result" 对象
                JSONObject resultObject = jsonObject.getJSONObject("result");

                MusicHolder.setSearchResult(String.valueOf(resultObject));

                // 现在从 "result" 对象中获取 "songs" 数组
                JSONArray jsonArray = resultObject.getJSONArray("songs");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject musicData = jsonArray.getJSONObject(i);
                    String name = musicData.getString("name");
                    String id = musicData.getString("id");
                    // 获取艺术家数组
                    JSONArray arArray = musicData.getJSONArray("ar");
                    StringBuilder artistNames = new StringBuilder();

                    for (int j = 0; j < arArray.length(); j++) {
                        JSONObject artistObject = arArray.getJSONObject(j);
                        String artistName = artistObject.getString("name");
                        if (j > 0) {
                            artistNames.append("/"); // 用斜杠隔开
                        }
                        artistNames.append(artistName);
                    }

                    // 获取专辑信息
                    JSONObject albumObject = musicData.getJSONObject("al");
                    String albumPicUrl = albumObject.getString("picUrl");


                    MusicListItemCloud item = new MusicListItemCloud();
                    item.setMusicImage(albumPicUrl);
                    item.setMusicName(name);
                    item.setArtistName(artistNames.toString());
                    Items.add(item);
                }

                getActivity().runOnUiThread(() -> {
                    musicListItemClouds.clear();
                    musicListAdapterCloud.notifyDataSetChanged();
                    musicListItemClouds.addAll(Items);
                    int position=musicListItemClouds.size();
                    musicListAdapterCloud.notifyItemChanged(position,Items.size());
                });

            } catch (JSONException e) {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireActivity(), "搜索出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });


            }
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(requireActivity(), "无搜索结果", Toast.LENGTH_LONG).show();
                }
            });

        }
    }




}