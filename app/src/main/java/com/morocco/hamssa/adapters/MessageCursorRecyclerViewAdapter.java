package com.morocco.hamssa.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.morocco.hamssa.MessagesFragment;
import com.morocco.hamssa.MyMessagesActivity;
import com.morocco.hamssa.R;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.ClickableTextViewHelper;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.ImageUtils;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by hmontaner on 25/06/15.
 */
public class MessageCursorRecyclerViewAdapter extends CursorRecyclerViewAdapter<MessageCursorRecyclerViewAdapter.ViewHolder>{

    Context context;
    Topic topic;
    MessagesFragment.TYPE type;
    ViewHolder viewHolder;
    public MessageCursorRecyclerViewAdapter(Context context, Cursor cursor, Topic topic, MessagesFragment.TYPE type){
        super(context,cursor);
        this.context = context;
        this.topic = topic;
        this.type = type;
        initIndexes(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor){
        initIndexes(newCursor);
        return super.swapCursor(newCursor);
    }

    int idIndex, textIndex, nameIndex, votesUpIndex, votesDownIndex, timeIndex, imageUrlIndex,
            ordinalIndex, topicIdIndex, topicTitleIndex, topicUserNameIndex, topicDescriptionIndex;
    public void initIndexes(Cursor cursor){
        if(cursor != null){
            idIndex = cursor.getColumnIndex(BaseColumns._ID);
            textIndex = cursor.getColumnIndex("text");
            nameIndex = cursor.getColumnIndex("name");
            votesUpIndex = cursor.getColumnIndex("votesUp");
            votesDownIndex = cursor.getColumnIndex("votesDown");
            timeIndex = cursor.getColumnIndex("time");
            imageUrlIndex = cursor.getColumnIndex("imageUrl");
            ordinalIndex = cursor.getColumnIndex("ordinal");
            topicIdIndex = cursor.getColumnIndex("topicId");
            topicTitleIndex = cursor.getColumnIndex("title");
            topicUserNameIndex = cursor.getColumnIndex("topicUserName");
            topicDescriptionIndex = cursor.getColumnIndex("description");
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView text, name, time, votesUp, votesDown, topicTitle;
        public View votesUpWrapper, votesDownWrapper, reply, topicTitleWrapper;
        public ImageView imageView;
        String messageId = null;
        Context context;
        long mLastClickTime = 0;

        public ViewHolder(View view){
            this(view, null);
        }
        public ViewHolder(View view, final String messageId){
            super(view);
            this.context = view.getContext();
            this.messageId = messageId;
            text = (TextView) view.findViewById(R.id.text);
            name = (TextView) view.findViewById(R.id.name);
            time = (TextView) view.findViewById(R.id.time);
            votesUp = (TextView) view.findViewById(R.id.votes_up);
            votesDown = (TextView) view.findViewById(R.id.votes_down);
            votesUpWrapper = view.findViewById(R.id.votes_up_wrapper);
            votesDownWrapper = view.findViewById(R.id.votes_down_wrapper);
            imageView = (ImageView)view.findViewById(R.id.profile_picture);
            topicTitle = (TextView)view.findViewById(R.id.topic_title);
            topicTitleWrapper = view.findViewById(R.id.topic_title_wrapper);
            reply = view.findViewById(R.id.reply);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(onItemClickListener != null){
                        String id = messageId;
                        if(id == null) {
                            int p = getAdapterPosition();
                            id = getMessageId(p);
                        }
                        onItemClickListener.onClick(id);
                    }
                }
            });

            votesUpWrapper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try{
                        // Preventing multiple clicks, using threshold of 1 second
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();
                        Long votes = Long.parseLong(votesUp.getText().toString());
                        votesUp.setText(""+(++votes));
                    }catch(NumberFormatException e){}
                    if (onItemClickListener != null) {
                        String id = messageId;
                        if(id == null) {
                            int p = getAdapterPosition();
                            id = getMessageId(p);
                        }
                        onItemClickListener.onUpVoteClick(id);
                    }
                }
            });
            votesDownWrapper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try{
                        // Preventing multiple clicks, using threshold of 1 second
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        Long votes = Long.parseLong(votesDown.getText().toString());
                        votesDown.setText(""+(++votes));
                    }catch(NumberFormatException e){}
                    if(onItemClickListener != null){
                        String id = messageId;
                        if(id == null) {
                            int p = getAdapterPosition();
                            id = getMessageId(p);
                        }
                        onItemClickListener.onDownVoteClick(id);
                    }
                }
            });
            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(onItemClickListener != null){
                        String id = messageId;
                        if(id == null) {
                            int p = getAdapterPosition();
                            id = getMessageId(p);
                        }
                        onItemClickListener.onReplyClick(id);
                    }
                }
            });

        }
        public Context getContext(){
            return context;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, parent, false);
        viewHolder=new ViewHolder(view);
        return viewHolder;
    }

    public static class MyDataModel {
        String url;
        public MyDataModel(String url){
            this.url = url;
        }
        public String buildUrl(int width, int height){
            // Note this is useless because width and height
            // are always the screen size as imageViews have no sizes
            return url+"=s"+Math.max(width, height);
        }
    }
    public static class MyUrlLoader extends BaseGlideUrlLoader<MyDataModel> {
        public MyUrlLoader(Context context){
            super(context);
        }
        @Override
        protected String getUrl(MyDataModel model, int width, int height) {
            // Construct the url for the correct size here.
            return model.buildUrl(width, height);
        }
    }

    private String getFromCursor(Cursor cursor, int index){
        if(index >= 0){
            return cursor.getString(index);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, Cursor cursor) {
        String topicTitle=null;
        if(topic!=null) {
            topicTitle = topic.getTitle();
        }
        if(topicTitle == null || topicTitle.isEmpty()) {
            String topicUserName = getFromCursor(cursor, topicUserNameIndex);
            String topicDescription = getFromCursor(cursor, topicDescriptionIndex);
            if(topicUserName != null && topicDescription != null) {
                topicTitle = "@" + topicUserName + " " + topicDescription;
            }
        }
        fillMessage(this, viewHolder,
                cursor.getString(textIndex),
                cursor.getLong(ordinalIndex),
                cursor.getString(nameIndex),
                cursor.getLong(votesUpIndex),
                cursor.getLong(votesDownIndex),
                cursor.getLong(timeIndex),
                cursor.getString(imageUrlIndex),
                cursor.getString(topicIdIndex),
                topicTitle, type);
    }

    static public void fillMessage(final MessageCursorRecyclerViewAdapter adapter, final ViewHolder viewHolder, String text, Long ordinal, String name, Long votesUp, Long votesDown, Long time, String imageUrl, final String topicId, String topicTitle, MessagesFragment.TYPE type){
        if(ordinal > 0) {
            text = "<strong><font color=#F44336>#" + ordinal + "</font></strong> " + text;
        }
        final Context context = viewHolder.getContext();

        //viewHolder.text.setText(Html.fromHtml(text));
        ClickableTextViewHelper.initClickableTextView(viewHolder.text, Html.fromHtml(text), new ClickableTextViewHelper.OnWordClickedListener() {
            @Override
            public void onMessageReferenceClicked(Long messageOrdinal) {
                onReferenceClicked(adapter, context, messageOrdinal, topicId);
            }
            @Override
            public void onUserReferenceClicked(String userName){
                onUserNameClicked(context, userName);
            }
        });
        viewHolder.name.setText("@" + name);
        viewHolder.votesUp.setText(""+votesUp);
        viewHolder.votesDown.setText(""+votesDown);
        viewHolder.time.setText(Utils.msToElapsedTime(viewHolder.time.getContext(), time));

        if(topicTitle == null){
            viewHolder.topicTitleWrapper.setVisibility(View.GONE);
        }else{
            viewHolder.topicTitleWrapper.setVisibility(View.VISIBLE);
            viewHolder.topicTitle.setText(topicTitle);
            int visibility = View.GONE;
            if(type != null && type != MessagesFragment.TYPE.SENT){
                visibility = View.VISIBLE;
            }
            viewHolder.reply.setVisibility(visibility);
        }

        if(imageUrl != null && !imageUrl.isEmpty()){
            Glide.with(context)
                    .using(new MyUrlLoader(context))
                    .load(new MyDataModel(imageUrl))
                    .asBitmap()
                    .centerCrop()
                    .into(new BitmapImageViewTarget(viewHolder.imageView) {
                        @Override
                        protected void setResource(Bitmap bitmap) {
                            super.setResource(ImageUtils.getRoundImage(bitmap, false));
                        }
                    });
        }else{
            viewHolder.imageView.setImageResource(R.drawable.profile_picture);
        }
    }


    static private void onReferenceClicked(final MessageCursorRecyclerViewAdapter adapter, final Context context, final Long messageOrdinal, final String topicId){
        Database db = new Database(context);
        Message message = db.getMessageByOrdinal(topicId, messageOrdinal);
        String messageId = message != null ? message.getId() : null;
        final AlertDialog alertDialog = Utils.showMessageDialog(context, message, topicId, messageId, adapter);
        if(message == null){
            List<NameValuePair> params = HTTPTask.getParams(context);
            params.add(new NameValuePair("topic_id", topicId));
            params.add(new NameValuePair("ordinal", messageOrdinal));

            Constants.TASK task = Constants.TASK.GET_MESSAGE;

            new HTTPTask(context, task, new HTTPTask.Callback() {
                Message message = null;
                @Override
                public void parseResponse(JSONObject jsonObject){
                    if(Utils.isSuccess(jsonObject)){
                        try {
                            if(jsonObject.has("message")) {
                                Database db = new Database(context);
                                db.addMessage(jsonObject.getJSONObject("message"));
                                message = db.getMessageByOrdinal(topicId, messageOrdinal);
                            }
                        }catch(JSONException e){}
                    }
                }
                @Override
                public void onDataReceived(JSONObject jsonObject) {
                    alertDialog.dismiss();
                    if(message != null){
                        Utils.showMessageDialog(context, message, topicId, message.getId(), adapter);
                    }else {
                        int stringId = jsonObject == null ? R.string.connection_error : R.string.message_not_found;
                        Toast.makeText(context, context.getString(stringId), Toast.LENGTH_SHORT).show();
                    }
                }
            }).executeParallel(params);
        }
    }

    static private void onUserNameClicked(Context context, String userName){
        Intent intent = new Intent(context, MyMessagesActivity.class);
        intent.putExtra("type", MessagesFragment.TYPE.SENT.ordinal());
        intent.putExtra("user_name", userName);
        context.startActivity(intent);
    }

    public String getMessageId(int position){
        Cursor cursor = getItem(position);
        return cursor.getString(idIndex);
    }

    public interface OnItemClickListener{
        void onClick(String itemId);
        void onUpVoteClick(String itemId);
        void onDownVoteClick(String itemId);
        void onReplyClick(String itemId);
    }
    OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    public ViewHolder getViewHolder() {
        return viewHolder;
    }
}
