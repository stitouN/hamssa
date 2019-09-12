package com.morocco.hamssa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.OnLoadMoreListener;
import com.morocco.hamssa.adapters.TopicCursorRecyclerViewAdapter;
import com.morocco.hamssa.adapters.VerticalSpaceItemDecoration;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hmontaner on 10/09/15.
 */
public class TopicsFragment extends Fragment {

    private ProgressDialog progressDialog ;
    private static final String ARG_ORDERING_TYPE = "arg_ordering_type";
    public static final String ARG_TOPIC_ID = "arg_topic_id";
    public enum TYPE{
        LATEST, MOST_COMMENTED,MY_POSTS;
        public static TYPE getType(int ordinal){
            if(ordinal == 0){
                return LATEST;
            }else if(ordinal == 1){
                return MOST_COMMENTED;
            }else if(ordinal==2){
                return MY_POSTS;
            }
            return null;
        }
    }

    public static TopicsFragment newInstance(TYPE type) {
        TopicsFragment fragment = new TopicsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ORDERING_TYPE, type.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    Context context;
    TYPE type;
    View rootView;
    private int spaceBetweenAds;
    private List<Object> mDataSet=new ArrayList<>();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        type = TYPE.getType(getArguments().getInt(ARG_ORDERING_TYPE));
        setupList();
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        adapter.swapCursor(getCursor());
    }

    private Cursor getCursor(){
        return new Database(context).getTopics(type);
    }

    TopicCursorRecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    private void setupList(){
        recyclerView= (RecyclerView)rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(20));
        Database db = new Database(context);
        db.clearAllTables();
        Cursor cursor = getCursor();
        //addNativeExpressAds();
        adapter = new TopicCursorRecyclerViewAdapter(recyclerView,context, cursor,mDataSet,spaceBetweenAds);

        adapter.setEmptyView(rootView.findViewById(R.id.empty_view));

        adapter.setOnItemClickListener(new TopicCursorRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onClick(String itemId) {
                ProgressDialog progressDialog=ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                Intent intent = new Intent(context, TopicActivity.class);
                intent.putExtra(ARG_TOPIC_ID, itemId);
                startActivity(intent);
                progressDialog.dismiss();
            }
            @Override
            public void onLinkClick(String title, String linkUrl){
                /*Intent intent = new Intent(context, WebActivity.class);
                intent.putExtra(WebActivity.ARG_URL, linkUrl);
                intent.putExtra(WebActivity.ARG_TITLE, title);
                startActivity(intent);*/
            }
            @Override
            public void onExpandClick(String itemId){
            onLongClick(itemId);
            }
            @Override
            public void onLongClick(String itemId){
                onTopicLongClick(itemId);
            }
        });

        recyclerView.setAdapter(adapter);
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.list_view_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPlaylistMetadata(swipeRefreshLayout,0);
            }
        });

        adapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if(adapter.getLastVisibleItem()==0){
                    loadPlaylistMetadata(swipeRefreshLayout,0);
                }else if(adapter.getLastVisibleItem()<=2){
                    loadPlaylistMetadata(swipeRefreshLayout,adapter.getTotalItemCount());
                }

            }
        });

        /*swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

            }
        });*/
        loadPlaylistMetadata(swipeRefreshLayout,0);
    }

    private void onTopicLongClick(final String topicId){
        ProgressDialog progressDialog=ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
        Topic topic = new Database(getContext()).getTopic(topicId);
        if(topic.isMine(getContext())){
            progressDialog.dismiss();
            CharSequence options[] = new CharSequence[]{getString(R.string.edit), getString(R.string.delete)};
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.options_about_this_topic));
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        Intent intent = new Intent(context, NewTopicActivity.class);
                        intent.putExtra(NewTopicActivity.ARG_TOPIC_ID, topicId);
                        startActivity(intent);
                    } else if (which == 1) {
                        removeTopic(topicId);
                    }
                }
            });
            builder.show();
        }else{
            progressDialog.dismiss();
            String message ="Vous n'avez pas les droits nécessaires";
            Toast toast=Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }

    }


    private void removeTopic(final String topicId){
        List<NameValuePair> params = HTTPTask.getParams(context);
        Constants.TASK task = Constants.TASK.REMOVE_TOPIC;
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        final Map<String, Object> data = new HashMap<>();
        data.put("token", params.get(1).getValue());
        data.put("topicId", topicId);

        functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
            boolean success = false;
            Cursor cursor;
            @Override
            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                Gson gson = new Gson();
                if (task.isSuccessful()) {
                    String result = gson.toJson(task.getResult().getData());
                    Cursor cursor = null;
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject != null && jsonObject.length() != 0) {
                            JSONArray jsonArray = new JSONArray();
                            JSONArray keys = jsonObject.names();
                            Utils.constructTopicsJsonArray(jsonObject, jsonArray, keys);

                            Database db = new Database(context);
                            if (type == TYPE.MY_POSTS) {
                                db.setUserId(data.get("token").toString());
                            }
                            db.removeTopic(topicId);
                            db.addTopics(jsonArray);

                            cursor = db.getTopics(type);
                            success=true;
                        }
                    }catch(JSONException ex){

                    }
                }else{
                    String message = getString(R.string.connection_error);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                if(!isAdded()){ return; }
                progressDialog.dismiss();
                if(success){
                    adapter.swapCursor(getCursor());
                }else{
                    String message = getString(R.string.connection_error);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
      /*  new HTTPTask(context, task, new HTTPTask.Callback() {
            boolean success = false;
            @Override
            public void parseResponse(JSONObject jsonObject){
                if(Utils.isSuccess(jsonObject)){
                    try {
                        JSONArray jsonArray = new JSONArray();
                        if(jsonObject.has("topics")) {
                            jsonArray = jsonObject.getJSONArray("topics");
                        }
                        Database db = new Database(context);
                        db.addTopics(jsonArray);
                        success = true;
                    }catch(JSONException e){}
                }
            }
            @Override
            public void onDataReceived(JSONObject jsonObject) {
                if(!isAdded()){ return; }
                progressDialog.dismiss();
                if(success){
                    adapter.swapCursor(getCursor());
                }else{
                    String message = getString(R.string.connection_error);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            }
        }).executeParallel(params);*/

    }


    private void loadPlaylistMetadata(final SwipeRefreshLayout swipeRefreshLayout,Integer position) {
        swipeRefreshLayout.setRefreshing(true);
        Constants.TASK task;
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        List<NameValuePair> params = HTTPTask.getParams(context);
        if (type == TYPE.MY_POSTS) {
            task=Constants.TASK.GET_TOPIC_BY_USER;
        }else{
            task=Constants.TASK.GET_TOPICS;;
        }
        final Map<String, Object> data = new HashMap<>();
        data.put("token", params.get(1).getValue());
        data.put("position",position);
        functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
            @Override
            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                    Gson gson = new Gson();
                    if (task.isSuccessful()) {
                        String result = gson.toJson(task.getResult().getData());
                        Cursor cursor = null;
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            if (jsonObject != null && jsonObject.length() != 0) {
                                JSONArray jsonArray = new JSONArray();
                                JSONArray keys = jsonObject.names();
                                Utils.constructTopicsJsonArray(jsonObject, jsonArray, keys);

                                Database db = new Database(context);
                                if (type == TYPE.MY_POSTS) {
                                    db.setUserId(data.get("token").toString());
                                }
                                db.addTopics(jsonArray);
                                cursor = db.getTopics(type);

                            }
                            adapter.setLoaded();
                            if (!isAdded()) {
                                return;
                            }
                            if (cursor != null) {
                                adapter.swapCursor(cursor);
                            } else {
                                String message = getString(R.string.connection_error);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String message = getString(R.string.connection_error);
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                swipeRefreshLayout.setRefreshing(false);
            }

        });
    }

    private void addNativeExpressAds() {

        // We are looping through our original dataset
        // And adding Admob's Native Express Ad at consecutive positions at a distance of spaceBetweenAds
        // You should change the spaceBetweenAds variable according to your need
        // i.e how often you want to show ad in RecyclerView

        for (int i = spaceBetweenAds; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
            NativeExpressAdView adView = new NativeExpressAdView(context);
            // I have used a Test ID provided by Admob below
            // you should replace it with yours
            // And if wou are just experimenting, then just copy the code
            adView.setAdUnitId("ca-app-pub-3940256099942544/2793859312");
            mDataSet.add(i, adView);
        }

        // Below we are using post on RecyclerView
        // because we want to resize our native ad's width equal to screen width
        // and we should do it after RecyclerView is created

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                float scale = context.getResources().getDisplayMetrics().density;
                int adWidth = (int) (recyclerView.getWidth() - (2 * context.getResources().getDimension(R.dimen.activity_horizontal_margin)));

                // we are setting size of adView
                // you should check admob's site for possible ads size
                AdSize adSize = new AdSize((int) (adWidth / scale), 150);

                // looping over mDataset to sesize every Native Express Ad to ew adSize
                for (int i = spaceBetweenAds; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
                    NativeExpressAdView adViewToSize = (NativeExpressAdView) mDataSet.get(i);
                    adViewToSize.setAdSize(adSize);
                }

                // calling method to load native ads in their views one by one
                loadNativeExpressAd(spaceBetweenAds);
            }
        });

    }

    private void loadNativeExpressAd(final int index) {

        if (index >= mDataSet.size()) {
            return;
        }

        Object item = mDataSet.get(index);
        if (!(item instanceof NativeExpressAdView)) {
            throw new ClassCastException("Expected item at index " + index + " to be a Native"
                    + " Express ad.");
        }

        final NativeExpressAdView adView = (NativeExpressAdView) item;

        // Set an AdListener on the NativeExpressAdView to wait for the previous Native Express ad
        // to finish loading before loading the next ad in the items list.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // The previous Native Express ad loaded successfully, call this method again to
                // load the next ad in the items list.
                loadNativeExpressAd(index + spaceBetweenAds + 1);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // The previous Native Express ad failed to load. Call this method again to load
                // the next ad in the items list.
                Log.e("AdmobMainActivity", "The previous Native Express ad failed to load. Attempting to"
                        + " load the next Native Express ad in the items list.");
                loadNativeExpressAd(index + spaceBetweenAds + 1);
            }
        });

        // Load the Native Express ad.
        //We also registering our device as Test Device with addTestDevic("ID") method
        adView.loadAd(new AdRequest.Builder().addTestDevice("YOUR_TEST_DEVICE_ID").build());
    }


}
