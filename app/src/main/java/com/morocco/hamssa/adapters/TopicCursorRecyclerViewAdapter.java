package com.morocco.hamssa.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.SoundPool;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.ads.NativeExpressAdView;
import com.morocco.hamssa.R;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.RecordVoice;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.ImageUtils;

import java.util.List;

/**
 * Created by hmontaner on 25/06/15.
 */
public class TopicCursorRecyclerViewAdapter extends CursorRecyclerViewAdapter<TopicCursorRecyclerViewAdapter.ViewHolder>{


    private boolean isLoading;
    private int visibleThreshold = 5;
    private int lastVisibleItem, totalItemCount;
    Context context;
    private OnLoadMoreListener onLoadMoreListener;
    private int visibleItemCount,firstVisibleItemPosition;
    private List<Object> mDataSet;

    private int spaceBetweenAds=3;

    public TopicCursorRecyclerViewAdapter(RecyclerView recyclerView,Context context,final Cursor cursor,List<Object> mDataSet,int spaceBetweenAds){
        super(context,cursor);
        this.context = context;
        this.mDataSet=mDataSet;
        this.spaceBetweenAds=spaceBetweenAds;
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
                lastVisibleItem = totalItemCount-firstVisibleItemPosition;
                if(!isLoading && lastVisibleItem<=2){
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading=true;
                }
              //  Toast.makeText(this, "Visible Item Total:"+String.valueOf(visibleItemCount), Toast.LENGTH_SHORT).show();
            }
           /* @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                if (!isLoading && totalItemCount >= lastVisibleItem) {
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }*/
        });
        initIndexes(cursor);
    }



    @Override
    public Cursor swapCursor(Cursor newCursor){
        initIndexes(newCursor);
        return super.swapCursor(newCursor);
    }

    int idIndex, titleIndex, descriptionIndex, urlIndex, numMessagesIndex, linkIndex, userNameIndex, userImageUrlIndex;
    boolean isPlaying = false;
    public static RecordVoice recordVoice = new RecordVoice();
    Database db = new Database(context);

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

    // "Loading item" ViewHolder
    private class LoadingViewHolder extends ViewHolder {
        public ProgressBar progressBar;

        public LoadingViewHolder(View view) {
            super(view);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
        }
    }

    // View Holder for Admob Native Express Ad Unit
    public class NativeExpressAdViewHolder extends RecyclerView.ViewHolder {
        NativeExpressAdViewHolder(View view) {
            super(view);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView image, contentImage;
        public View imageWrapper, link, userNameWrapper, contentImageWrapper, expand, player, btn_play_pause;
        public TextView description, numMessages, title, userName, description_without_img;
        public Chronometer chronometer;
        SoundPool soundPool;
        int soundId;
        long mLastClickTime = 0;
        public ViewHolder(View view){
            super(view);
            description = (TextView) view.findViewById(R.id.description);
            description_without_img = (TextView) view.findViewById(R.id.description_without_image);
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
            player = view.findViewById(R.id.card_player);
            btn_play_pause = view.findViewById(R.id.btn_play_stop);
            chronometer = (Chronometer) view.findViewById(R.id.chronometer_sound);
            View.OnClickListener onClickListener=new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Preventing multiple clicks, using threshold of 1 second
                  if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                  mLastClickTime = SystemClock.elapsedRealtime();

                    if (onItemClickListener != null) {
                        String id = getTopicId(getAdapterPosition());
                        onItemClickListener.onClick(id);
                    }
                }
            };
            imageWrapper.setOnClickListener(onClickListener);
            view.setOnClickListener(onClickListener);
            contentImageWrapper.setOnClickListener(onClickListener);
            contentImage.setOnClickListener(onClickListener);
            numMessages.setOnClickListener(onClickListener);
            image.setOnClickListener(onClickListener);

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Preventing multiple clicks, using threshold of 1 second
             /*       if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return false;
                    }*/
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
                /*    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }*/
                   // mLastClickTime = SystemClock.elapsedRealtime();
                    if(onItemClickListener != null){
                        String id = getTopicId(getAdapterPosition());
                        onItemClickListener.onClick(id);
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

            btn_play_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Topic topic = db.getTopic(getTopicId(getAdapterPosition()));
                    if(!isPlaying) {
                        recordVoice.startPlaying(topic.getImageUrl());
                        //recordVoice.setAudioFilePath(topic.getImageUrl());
                        //recordVoice.playSound(1.5f);
                        btn_play_pause.setBackgroundResource(R.drawable.ic_stop);
                        isPlaying = true;
                    }else{
                        recordVoice.stopPlaying();
                        btn_play_pause.setBackgroundResource(R.drawable.ic_play_button);
                        isPlaying = false;
                    }
                }
            });

        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        // Switch Case for creating ViewHolder based on viewType
    //      switch (viewType) {
      //      case VIEW_TYPE_ITEM:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.topic_list_item, parent, false);
                return new ViewHolder(view);
     //     case NATIVE_EXPRESS_AD_VIEW_TYPE:
                // fall through
      // default:
       //    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.topic_list_item, parent, false);
        //   return new ViewHolder(view);

        //}
    }
    @Override
    public void onBindViewHolder(ViewHolder  viewHolder, Cursor cursor) {
        int viewType = getItemViewType(lastVisibleItem);

        // Binding data based on View Type
     //   switch (viewType) {
       //     case VIEW_TYPE_ITEM:
                boolean personal = cursor.getString(userNameIndex) != null
                        && !cursor.getString(userNameIndex).isEmpty();

                String description = cursor.getString(descriptionIndex);
                if (description == null || description.isEmpty()) {
                    description = cursor.getString(titleIndex);
                }
                viewHolder.description.setText(description);
                viewHolder.description_without_img.setText(description);
                String title = cursor.getString(titleIndex);
                if (title != null && !title.isEmpty()) {
                    viewHolder.title.setVisibility(View.VISIBLE);
                    viewHolder.title.setText(title);
                } else {
                    viewHolder.title.setVisibility(View.GONE);
                }

                String userName = cursor.getString(userNameIndex);
                if (userName != null && !userName.isEmpty()) {
                    viewHolder.userNameWrapper.setVisibility(View.VISIBLE);
                    viewHolder.userName.setText(userName);
                } else {
                    viewHolder.userNameWrapper.setVisibility(View.GONE);
                }

                viewHolder.numMessages.setText(cursor.getString(numMessagesIndex));

                if (Constants.SHOW_LINKS) {
                    String linkUrl = cursor.getString(linkIndex);
                    if (linkUrl != null && !linkUrl.isEmpty()) {
                        viewHolder.link.setVisibility(View.VISIBLE);
                    } else {
                        viewHolder.link.setVisibility(View.GONE);
                    }
                }
                ////
                viewHolder.contentImageWrapper.setVisibility(View.VISIBLE); //Gone
                if (personal) {
                    String userImageUrl = cursor.getString(userImageUrlIndex);
                    if (userImageUrl == null || userImageUrl.isEmpty()) {
                        viewHolder.imageWrapper.setVisibility(View.VISIBLE);
                    } else {
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
                    viewHolder.description_without_img.setVisibility(View.VISIBLE);
                    viewHolder.contentImageWrapper.setVisibility(View.GONE);
                    viewHolder.player.setVisibility(View.GONE);
                    String contentImageUrl = cursor.getString(urlIndex);
                    if (contentImageUrl != null && !contentImageUrl.isEmpty()) {

                        if (contentImageUrl.contains("sounds")) {
                            viewHolder.description_without_img.setVisibility(View.GONE);
                            viewHolder.contentImageWrapper.setVisibility(View.GONE);
                            viewHolder.player.setVisibility(View.VISIBLE);

                        } else {
                            viewHolder.description_without_img.setVisibility(View.GONE);
                            viewHolder.contentImageWrapper.setVisibility(View.VISIBLE);

                            Glide.with(context)
                                    .load(contentImageUrl)
                                    .crossFade()
                                    //.centerCrop()
                                    .into(viewHolder.contentImage);
                        }
                    } else {
                        viewHolder.description_without_img.setVisibility(View.VISIBLE);
                    }
                } else {
                    String url = cursor.getString(urlIndex);
                    if (url == null || url.isEmpty()) {
                        viewHolder.imageWrapper.setVisibility(View.GONE);
                    } else {
                        viewHolder.imageWrapper.setVisibility(View.VISIBLE);
                        Glide.with(context)
                                .load(url)
                                .crossFade()
                                .centerCrop()
                                .into(viewHolder.image);
                    }
                }
         //       break;
           // case NATIVE_EXPRESS_AD_VIEW_TYPE:
                // fall throug
        //}
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

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.onLoadMoreListener = mOnLoadMoreListener;
    }



    public void setLoaded() {
        isLoading = false;
    }

    public int getLastVisibleItem() {
        return lastVisibleItem;
    }

    public int getTotalItemCount() {
        return totalItemCount;
    }
}