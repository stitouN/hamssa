package com.morocco.hamssa.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.List;

/**
 * Created by hmontaner on 12/10/15.
 */
public class MyListAdapter<T extends MyListAdapter.Viewable> extends ArrayAdapter<T> {

    public interface Viewable{
        View getView(int position, View convertView, ViewGroup parent);
    }

    List<T> items;
    public MyListAdapter(Context context, List<T> items) {
        super(context, 0, items);
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItem(position).getView(position, convertView, parent);
    }

}
