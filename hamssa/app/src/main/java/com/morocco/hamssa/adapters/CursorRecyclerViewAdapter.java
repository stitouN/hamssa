package com.morocco.hamssa.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by hmontaner on 25/06/15.
 */
public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private Context context;
    private Cursor cursor;
    private boolean dataValid;
    private int rowIdColumn;
    private DataSetObserver dataSetObserver;

    public CursorRecyclerViewAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
        dataValid = cursor != null;
        showEmptyViewIfNecessary(cursor);
        rowIdColumn = dataValid ? cursor.getColumnIndex("_id") : -1;
        dataSetObserver = new NotifyingDataSetObserver();
        if (this.cursor != null) {
            this.cursor.registerDataSetObserver(dataSetObserver);
        }
    }

    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public int getItemCount() {
        if (dataValid && cursor != null) {
            return cursor.getCount();
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }


    public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        if (!dataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        onBindViewHolder(viewHolder, cursor);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    View emptyView;

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     */
    enum ACTION{
        NONE, REMOVE, INSERT
    }
    public Cursor swapCursor(Cursor newCursor) {
        return swapCursorFor(newCursor, ACTION.NONE, null);
    }
    public Cursor swapCursorForRemoval(Cursor newCursor, Integer removedPosition) {
        return swapCursorFor(newCursor, ACTION.REMOVE, removedPosition);
    }
    public Cursor swapCursorForInsertion(Cursor newCursor, Integer insertedPosition) {
        return swapCursorFor(newCursor, ACTION.INSERT, insertedPosition);
    }
    public Cursor swapCursorFor(Cursor newCursor, ACTION action, Integer position){
        if (newCursor == cursor) {
            return null;
        }
        final Cursor oldCursor = cursor;
        if (oldCursor != null && dataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        showEmptyViewIfNecessary(newCursor);
        cursor = newCursor;
        if (cursor != null) {
            if (dataSetObserver != null) {
                cursor.registerDataSetObserver(dataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            dataValid = true;
            // Rationale: first change cursor, then notify
            if(action == ACTION.REMOVE){
                notifyItemRemoved(position);
            }else if(action == ACTION.INSERT) {
                notifyItemInserted(position);
            }else{
                notifyDataSetChanged();
            }
        } else {
            rowIdColumn = -1;
            dataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }

    private void showEmptyViewIfNecessary(Cursor cursor){
        if(emptyView != null) {
            if (cursor == null || cursor.getCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            dataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            dataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }

    public void setEmptyView(View view){
        this.emptyView = view;
        showEmptyViewIfNecessary(cursor);
    }

    public Cursor getItem(int position) {
        if (dataValid && cursor != null) {
            cursor.moveToPosition(position);
            return cursor;
        } else {
            return null;
        }
    }
}
