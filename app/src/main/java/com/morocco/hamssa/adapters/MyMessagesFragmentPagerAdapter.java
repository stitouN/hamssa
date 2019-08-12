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
public class MyMessagesFragmentPagerAdapter extends FragmentPagerAdapter {
    List<String> tabTitles = new ArrayList<>();
    private Context context;
    String userName;

    public MyMessagesFragmentPagerAdapter(FragmentManager fm, Context context) {
        this(fm, context, null);
    }
    public MyMessagesFragmentPagerAdapter(FragmentManager fm, Context context, String userName) {
        super(fm);
        this.context = context;
        this.userName = userName;
        if(userName == null) {
            tabTitles.add(context.getString(R.string.received));
        }
        tabTitles.add(context.getString(R.string.sent));
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    @Override
    public Fragment getItem(int position) {
        MessagesFragment.TYPE type = position == 0 ? MessagesFragment.TYPE.RECEIVED : MessagesFragment.TYPE.SENT;
        if(userName != null){
            type = MessagesFragment.TYPE.SENT;
        }

        MessagesFragment messagesFragmentAux = MessagesFragment.newInstance(userName, type);
        return messagesFragmentAux;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }
}
