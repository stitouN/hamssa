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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hmontaner on 10/09/15.
 */
public class TopicsFragment extends Fragment {

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
    private void setupList(){
        RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(20));

        Cursor cursor = getCursor();
        adapter = new TopicCursorRecyclerViewAdapter(context, cursor);

        adapter.setEmptyView(rootView.findViewById(R.id.empty_view));

        adapter.setOnItemClickListener(new TopicCursorRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onClick(String itemId) {
                Intent intent = new Intent(context, TopicActivity.class);
                intent.putExtra(ARG_TOPIC_ID, itemId);
                startActivity(intent);
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
                loadPlaylistMetadata(swipeRefreshLayout);
            }
        });
        loadPlaylistMetadata(swipeRefreshLayout);
    }

    private void onTopicLongClick(final String topicId){

        Topic topic = new Database(getContext()).getTopic(topicId);
     //   if(topic.isMine(getContext()))
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
      //  }
    }


    private void removeTopic(final String topicId){
        List<NameValuePair> params = HTTPTask.getParams(context);
        Constants.TASK task = Constants.TASK.REMOVE_TOPIC;
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        final Map<String, Object> data = new HashMap<>();
        data.put("token", params.get(1).getValue());
        data.put("topicId", topicId);
        final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
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


    private void loadPlaylistMetadata(final SwipeRefreshLayout swipeRefreshLayout) {
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
                            swipeRefreshLayout.setRefreshing(false);
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

            }
        });
    }



}
