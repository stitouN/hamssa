package com.morocco.hamssa.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.morocco.hamssa.R;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.ImageUtils;

/**
 * Created by hmontaner on 25/06/15.
 */
public class TopicCursorRecyclerViewAdapter extends CursorRecyclerViewAdapter<TopicCursorRecyclerViewAdapter.ViewHolder>{

    Context context;
    public TopicCursorRecyclerViewAdapter(Context context, Cursor cursor){
        super(context,cursor);
        this.context = context;
        initIndexes(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor){
        initIndexes(newCursor);
        return super.swapCursor(newCursor);
    }

    int idIndex, titleIndex, descriptionIndex, urlIndex, numMessagesIndex, linkIndex, userNameIndex, userImageUrlIndex;
    public void initIndexes(Cursor cursor){
        if(cursor != null){
            idIndex = cursor.getColumnIndex(BaseColumns._ID);
            titleIndex = cursor.getColumnIndex("title");
            descriptionIndex = cursor.getColumnIndex("description");
            urlIndex = cursor.getColumnIndex("url");
            numMessagesIndex = cursor.getColumnIndex("numMessages");
            linkIndex = cursor.getColumnIndex("linkUrl");
            userNameIndex = cursor.getColumnIndex("userName");
            userImageUrlIndex = cursor.getColumnIndex("userImageUrl");
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView image, contentImage;
        public View imageWrapper, link, userNameWrapper, contentImageWrapper, expand;
        public TextView description, numMessages, title, userName;
        long mLastClickTime = 0;
        public ViewHolder(View view){
            super(view);
            description = (TextView) view.findViewById(R.id.description);
            numMessages = (TextView) view.findViewById(R.id.numMessages);
            title = (TextView) view.findViewById(R.id.title);
            userName = (TextView) view.findViewById(R.id.userName);
            image = (ImageView) view.findViewById(R.id.image);
            imageWrapper = view.findViewById(R.id.image_wrapper);
            link = view.findViewById(R.id.link);
            userNameWrapper = view.findViewById(R.id.userName_wrapper);
            contentImage = (ImageView)view.findViewById(R.id.content_image);
            contentImageWrapper = view.findViewById(R.id.content_image_wrapper);
            expand = view.findViewById(R.id.expand);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Preventing multiple clicks, using threshold of 1 second
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    if(onItemClickListener != null){
                        String id = getTopicId(getAdapterPosition());
                        onItemClickListener.onClick(id);
                    }
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Preventing multiple clicks, using threshold of 1 second
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return false;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    if(onItemClickListener != null){
                        String id = getTopicId(getAdapterPosition());
                        onItemClickListener.onLongClick(id);
                        return true;
                    }
                    return false;
                }
            });

            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Preventing multiple clicks, using threshold of 1 second
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    if(onItemClickListener != null){
                        Cursor cursor = getItem(getAdapterPosition());
                        String linkUrl = cursor.getString(linkIndex);
                        String title = cursor.getString(titleIndex);
                        onItemClickListener.onLinkClick(title, linkUrl);
                    }
                }
            });

            expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(onItemClickListener != null){
                        String id = getTopicId(getAdapterPosition());
                        onItemClickListener.onExpandClick(id);
                    }
                }
            });

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.topic_list_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        if(Constants.SHOW_LINKS) {
            viewHolder.link.setVisibility(View.VISIBLE);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        boolean personal = cursor.getString(userNameIndex) != null
                && !cursor.getString(userNameIndex).isEmpty();

        String description = cursor.getString(descriptionIndex);
        if(description == null || description.isEmpty()){
            description = cursor.getString(titleIndex);
        }
        viewHolder.description.setText(description);
        String title = cursor.getString(titleIndex);
        if(title != null && !title.isEmpty()) {
            viewHolder.title.setVisibility(View.VISIBLE);
            viewHolder.title.setText(title);
        }else{
            viewHolder.title.setVisibility(View.GONE);
        }

        String userName = cursor.getString(userNameIndex);
        if(userName != null && !userName.isEmpty()) {
            viewHolder.userNameWrapper.setVisibility(View.VISIBLE);
            viewHolder.userName.setText(userName);
        }else{
            viewHolder.userNameWrapper.setVisibility(View.GONE);
        }

        viewHolder.numMessages.setText(cursor.getString(numMessagesIndex));

        if(Constants.SHOW_LINKS){
            String linkUrl = cursor.getString(linkIndex);
            if(linkUrl != null && !linkUrl.isEmpty()){
                viewHolder.link.setVisibility(View.VISIBLE);
            }else{
                viewHolder.link.setVisibility(View.GONE);
            }
        }
        ////
        viewHolder.contentImageWrapper.setVisibility(View.VISIBLE); //Gone
        if(personal){
            String userImageUrl = cursor.getString(userImageUrlIndex);
            if(userImageUrl == null || userImageUrl.isEmpty()){
                viewHolder.imageWrapper.setVisibility(View.VISIBLE);
            }else {
                viewHolder.imageWrapper.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(userImageUrl)
                        .asBitmap()
                        .centerCrop()
                        .into(new BitmapImageViewTarget(viewHolder.image) {
                            @Override
                            protected void setResource(Bitmap bitmap) {
                                super.setResource(ImageUtils.getRoundImage(bitmap, false));
                            }
                        });
           }
            String contentImageUrl = cursor.getString(urlIndex);
            if(contentImageUrl != null && !contentImageUrl.isEmpty()){
                viewHolder.contentImageWrapper.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(contentImageUrl)
                        .crossFade()
                        //.centerCrop()
                        .into(viewHolder.contentImage);
            }
        }else{
            String url = cursor.getString(urlIndex);
            if(url == null || url.isEmpty()){
                viewHolder.imageWrapper.setVisibility(View.GONE);
            }else {
                viewHolder.imageWrapper.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(url)
                        .crossFade()
                        .centerCrop()
                        .into(viewHolder.image);
            }
        }
    }


    public String getTopicId(int position){
        Cursor cursor = getItem(position);
        return cursor.getString(idIndex);
    }

    public interface OnItemClickListener{
        void onClick(String itemId);
        void onLinkClick(String title, String linkUrl);
        void onExpandClick(String itemId);
        void onLongClick(String itemId);
    }
    OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

}
