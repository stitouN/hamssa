package com.morocco.hamssa.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.morocco.hamssa.MessagesFragment;
import com.morocco.hamssa.R;
import com.morocco.hamssa.TopicsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hmontaner on 31/07/15.
 */
public class TopicsFragmentPagerAdapter extends FragmentPagerAdapter {
    List<String> tabTitles = new ArrayList<>();
    private Context context;

    public TopicsFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        tabTitles.add(context.getString(R.string.last_news));
        tabTitles.add(context.getString(R.string.most_commented_news));
        tabTitles.add(context.getString(R.string.my_posts));
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0){
            return TopicsFragment.newInstance(TopicsFragment.TYPE.LATEST);
        }else if(position == 1){
            return TopicsFragment.newInstance(TopicsFragment.TYPE.MOST_COMMENTED);
        }else{
            return TopicsFragment.newInstance(TopicsFragment.TYPE.MY_POSTS);
            //return MessagesFragment.newInstance(MessagesFragment.TYPE.MOST_VOTED);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }
}
