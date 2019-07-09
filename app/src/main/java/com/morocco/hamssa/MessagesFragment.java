package com.morocco.hamssa;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.MessageCursorRecyclerViewAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
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
public class MessagesFragment extends Fragment {

    private static final String TAG = MessagesFragment.class.getSimpleName();

    private static final String ARG_TOPIC_ID = "arg_topic_id";
    private static final String ARG_USER_NAME = "arg_user_name";
    private static final String ARG_MESSAGE_ORDERING_TYPE = "arg_message_ordering_type";
    public enum TYPE{
        LATEST, MOST_VOTED, RECEIVED, SENT;
        public static TYPE getType(String ordinal){
            if(ordinal == null){
                return null;
            }
            try{
                int ordinalInt = Integer.parseInt(ordinal);
                return getType(ordinalInt);
            }catch(NumberFormatException e){
                return null;
            }
        }
        public static TYPE getType(int ordinal){
            if(ordinal == 0){
                return LATEST;
            }else if(ordinal == 1){
                return MOST_VOTED;
            }else if(ordinal == 2){
                return RECEIVED;
            }else if(ordinal == 3){
                return SENT;
            }
            return null;
        }
    }

    public interface OnMessageClickListener{
        public void OnMessageClick(String messageId);
    }

    public static MessagesFragment newInstance(TYPE type) {
        return newInstance(null, null, type);
    }
    public static MessagesFragment newInstanceUsername(String userName, TYPE type) {
        return newInstance(null, userName, type);
    }
    public static MessagesFragment newInstance(String topicId, TYPE type) {
        return newInstance(topicId, null, type);
    }
    public static MessagesFragment newInstance(String topicId, String userName, TYPE type) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        if(topicId != null) {
            args.putString(ARG_TOPIC_ID, topicId);
        }
        if(userName != null){
            args.putString(ARG_USER_NAME, userName);
        }
        args.putInt(ARG_MESSAGE_ORDERING_TYPE, type.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    Topic topic;
    Context context;
    View rootView;
    TYPE type;
    String userName;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_messages, container, false);
        context = getActivity();

        String topicId = getArguments().getString(ARG_TOPIC_ID);
        if(topicId != null && !topicId.isEmpty()) {
            topic = new Database(context).getTopic(topicId);
        }
        type = TYPE.getType(getArguments().getInt(ARG_MESSAGE_ORDERING_TYPE));
        userName = getArguments().getString(ARG_USER_NAME, null);
        setupList();
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        adapter.swapCursor(getCursor());
    }

    private Cursor getCursor(){
        Cursor cursor = null;
        if(type == TYPE.RECEIVED){
            cursor = new Database(context).getReceivedMessages();
        }else if(type == TYPE.SENT){
            cursor = new Database(context).getSentMessages(userName);
        }else{
            String topicId = topic != null ? topic.getId() : null;
            cursor = new Database(context).getMessages(topicId, type);
        }
        return cursor;
    }

    MessageCursorRecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    String serverCursor = null;
    boolean loadingPrevItems = false;
    private void setupList(){
        recyclerView = (RecyclerView)rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(false);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        //recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(20));

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.list_view_swipe_refresh);

        if(type == TYPE.LATEST) {
            /*
            recyclerView.addOnScrollListener(new MyOnScrollListener(layoutManager) {
                int prevCurrentPage = 0;
                @Override
                public void onLoadMore(int currentPage) {
                    if (serverCursor != null && currentPage > prevCurrentPage) {
                        prevCurrentPage = currentPage;
                        Toast.makeText(context, "onLoadMore", Toast.LENGTH_SHORT).show();
                        loadPlaylistMetadata(swipeRefreshLayout, false, serverCursor);
                    }
                }
            });*/
            serverCursor = new Database(context).getTopicCursor(topic.getId());
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if(serverCursor != null && !loadingPrevItems && isBottom(layoutManager)){
                        Log.i(TAG, "Loading previous items");
                        loadingPrevItems = true;
                        loadPlaylistMetadata(swipeRefreshLayout, false, serverCursor);
                    }
                }
            });
        }

        Cursor cursor = getCursor();
        adapter = new MessageCursorRecyclerViewAdapter(context, cursor, topic, type);

        adapter.setEmptyView(rootView.findViewById(R.id.empty_view));

        adapter.setOnItemClickListener(new MessageCursorRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onClick(String itemId){
                onMessageClick(itemId);
            }
            @Override
            public void onUpVoteClick(String itemId) {
                onVoteClick(itemId, true);
            }
            @Override
            public void onDownVoteClick(String itemId) {
                onVoteClick(itemId, false);
            }
            @Override
            public void onReplyClick(String itemId){
                onMessageReplyClick(itemId);
            }
        });

        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPlaylistMetadata(swipeRefreshLayout, false, null);
            }
        });
        loadPlaylistMetadata(swipeRefreshLayout, false, null);
    }

    private boolean isBottom(LinearLayoutManager linearLayoutManager){
        int itemCount = linearLayoutManager.getItemCount();
        int firstVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
        if(itemCount - 1 == firstVisibleItemPosition){
            Log.i(TAG, "List hit the bottom");
            return true;
        }
        return false;
    }

    private void onMessageClick(String messageId){
        if(getActivity() instanceof OnMessageClickListener){
            OnMessageClickListener listener = (OnMessageClickListener)getActivity();
            listener.OnMessageClick(messageId);
        }
    }

    private void onMessageReplyClick(String messageId){
        Message message = new Database(context).getMessage(messageId);
        Utils.showMessageReplyDialog(context, message);
    }


    private void onVoteClick(final String messageId, final boolean up){
        final Database db = new Database(context);
        adapter.swapCursor(getCursor());

        List<NameValuePair> params = HTTPTask.getParams(context);
        Map<String, Object> data=new HashMap<>();
        data.put("messageId", messageId);
        data.put("value", up ? 1 : -1);
        data.put("token",params.get(1).getValue());
        data.put("topicId",topic.getId());
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        int result=db.addVote(messageId, up);
        if(result==-1){
            Constants.TASK task = Constants.TASK.UNVOTE_MESSAGE;
            functions.getHttpsCallable(task.toString())
                    .call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                @Override
                public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                    try {
                        Cursor cursor = null;
                        Integer errorStringId = null;
                        Gson gson = new Gson();
                        String result = gson.toJson(task.getResult().getData());
                        JSONObject jsonObject = new JSONObject(result);
                        if (Utils.isSuccess(jsonObject)) {
                            db.removeVote(messageId, up);
                            cursor = getCursor();
                        } else if (jsonObject != null) {
                            errorStringId = R.string.vote_duplicated;
                        }
                        if (!isAdded()) {
                            return;
                        }
                        if (cursor == null) {
                            String message = getString(errorStringId != null ? errorStringId : R.string.connection_error);
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            db.removeVote(messageId, up);
                            cursor = getCursor();
                        }
                        adapter.swapCursor(cursor);
                    } catch (JSONException e) {

                    }
                }
            });
        }else {
            data.put("value",result==-2?result:up);
            Constants.TASK task = Constants.TASK.VOTE_MESSAGE;
            functions.getHttpsCallable(task.toString())
                    .call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                @Override
                public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                    try {
                        Cursor cursor = null;
                        Integer errorStringId = null;
                        Gson gson = new Gson();
                        String result = gson.toJson(task.getResult().getData());
                        JSONObject jsonObject = new JSONObject(result);
                        if (Utils.isSuccess(jsonObject)) {
                            cursor = getCursor();
                        } else if (jsonObject != null) {
                            errorStringId = R.string.vote_duplicated;
                        }
                        if (!isAdded()) {
                            return;
                        }
                        if (cursor == null) {
                            String message = getString(errorStringId != null ? errorStringId : R.string.connection_error);
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            db.removeVote(messageId, up);
                            cursor = getCursor();
                        }
                        adapter.swapCursor(cursor);
                    } catch (JSONException e) {

                    }
                }
            });
        }
      /*  new HTTPTask(context, task, new HTTPTask.Callback() {
            Cursor cursor = null;
            Integer errorStringId = null;
            @Override
            public void parseResponse(JSONObject jsonObject){
                if(Utils.isSuccess(jsonObject)){
                    cursor = getCursor();
                }else if(jsonObject != null){
                    errorStringId = R.string.vote_duplicated;
                }
            }
            @Override
            public void onDataReceived(JSONObject jsonObject) {
                if(!isAdded()){ return; }
                if(cursor == null){
                    String message = getString(errorStringId != null ? errorStringId : R.string.connection_error);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    db.removeVote(messageId, up);
                    cursor = getCursor();
                }
                adapter.swapCursor(cursor);
            }
        }).executeParallel(params);*/
    }


    public void loadPlaylistMetadata(final SwipeRefreshLayout swipeRefreshLayout, final boolean
        moveToTop, final String prevServerCursor){
        Log.i(TAG, "loadPlaylistMetadata");
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data=new HashMap<>();
        if(swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        List<NameValuePair> params = HTTPTask.getParams(context);
        data.put("token",params.get(1).getValue());
        if(topic != null) {
            data.put("topicId",topic.getId());
        }
        if(type == TYPE.MOST_VOTED){
            data.put("order", type.ordinal());
        }
        Constants.TASK task = Constants.TASK.GET_MESSAGES;
        if(type == TYPE.RECEIVED){
            task = Constants.TASK.GET_MESSAGES_RECEIVED;
        }else if(type == TYPE.SENT){
            task = Constants.TASK.GET_MESSAGES_SENT;
            if(userName != null){
                data.put("userName", userName);
            }
        }
        if(prevServerCursor != null){
            data.put("cursor", prevServerCursor);
        }

        functions.getHttpsCallable(task.toString())
                .call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        try {
                            Cursor cursor = null;
                            Gson gson = new Gson();
                            if(task.isSuccessful() && !task.getResult().getData().toString().isEmpty()) {
                                String result = gson.toJson(task.getResult().getData());
                                JSONObject jsonObject = new JSONObject(result);
                                if (jsonObject != null) {
                                    if (type == TYPE.LATEST) {
                                        if (jsonObject.has("cursor")) {
                                            if (serverCursor == null || prevServerCursor != null) {
                                                serverCursor = jsonObject.getString("cursor");
                                                new Database(context).setTopicCursor(topic.getId(), serverCursor);
                                            }
                                        } else if (serverCursor != null) {
                                            Log.i(TAG, "NO CURSOR");
                                            serverCursor = null;
                                            new Database(context).setTopicCursor(topic.getId(), null);
                                        }
                                    }
                                    JSONArray jsonArray = new JSONArray();
                                    JSONArray keys = jsonObject.names();


                                    Log.i(TAG, "Number messages received: " + jsonArray.length());
                                    Database db = new Database(context);
                                    Utils.constructMessagesJsonArray(jsonObject, jsonArray, keys, topic.getId());
                                    if (type == TYPE.RECEIVED) {
                                        db.addReceivedMessages(jsonArray);
                                    } else if (type == TYPE.SENT && userName == null) {
                                        db.addSentMessages(jsonArray);
                                    } else {
                                        db.addMessages(jsonArray);
                                    }
                                    cursor = getCursor();

                                    if (!isAdded()) {
                                        return;
                                    }
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                    if (cursor != null) {
                                        adapter.swapCursor(cursor);
                                    } else {
                                        String message = context.getString(R.string.connection_error);
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                    }
                                    if (moveToTop) {
                                        recyclerView.smoothScrollToPosition(0);
                                    }
                                    if (prevServerCursor != null) {
                                        loadingPrevItems = false;
                                    }
                                }
                            }else if(!task.isSuccessful()){
                                String message = getString(R.string.connection_error);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }else if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }catch (JSONException e) {

                        }
                    }
                });

    }


}
