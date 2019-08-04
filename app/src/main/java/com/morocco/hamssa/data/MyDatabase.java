package com.morocco.hamssa.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabase extends SQLiteOpenHelper {

    public static final String DB_NAME = "mydata";
    public static final int DB_VERSION = 1;
    public MyDatabase(Context context){
        super(context,DB_NAME,null,DB_VERSION );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
