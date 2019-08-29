package com.morocco.hamssa.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.morocco.hamssa.MessagesFragment;
import com.morocco.hamssa.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hmontaner on 31/07/15.
 */
public class MessagesFragmentPagerAdapter extends FragmentPagerAdapter {
    List<String> tabTitles = new ArrayList<>();
    private Context context;
    private String topicId;

    public MessagesFragmentPagerAdapter(FragmentManager fm, Context context, String topicId) {
        super(fm);
        this.context = context;
        this.topicId = topicId;
        tabTitles.add(context.getString(R.string.last_messages));
        tabTitles.add(context.getString(R.string.most_voted_messages));
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    MessagesFragment messagesFragment = null;
    @Override
    public Fragment getItem(int position) {
        MessagesFragment.TYPE type = position == 0 ? MessagesFragment.TYPE.LATEST : MessagesFragment.TYPE.MOST_VOTED;
        MessagesFragment messagesFragmentAux = MessagesFragment.newInstance(topicId, type);
        if(position == 0) {
            messagesFragment = messagesFragmentAux;
        }
        return messagesFragmentAux;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }

    public void notifyMessageSent(){
        if(messagesFragment != null) {
            messagesFragment.loadPlaylistMetadata(null, true, null);
        }
    }
}
