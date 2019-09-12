package com.morocco.hamssa.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.morocco.hamssa.LoginEmailFragment;
import com.morocco.hamssa.LoginQuickFragment;
import com.morocco.hamssa.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hmontaner on 31/07/15.
 */
public class LoginFragmentPagerAdapter extends FragmentPagerAdapter {
    List<String> tabTitles = new ArrayList<>();
    private Context context;

    public LoginFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        tabTitles.add(context.getString(R.string.login_quick));
        tabTitles.add(context.getString(R.string.login_email));
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0){
            return new LoginQuickFragment();
        }else{
            return new LoginEmailFragment();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }
}
