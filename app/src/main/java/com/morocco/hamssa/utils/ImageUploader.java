package com.morocco.hamssa.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.morocco.hamssa.R;
import com.morocco.hamssa.data.Database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ImageUploader {
    public interface OnReceiveJSON{
        public void onJSON(JSONObject jsonObject);
    }
	private Context context;
    OnReceiveJSON listener;
	boolean uploadForUserProfile;
	public ImageUploader(Context context, boolean uploadForUserProfile, OnReceiveJSON listener){
		this.context = context;
        this.uploadForUserProfile = uploadForUserProfile;
        this.listener = listener;
	}
    ProgressDialog progressDialog;

	public void upload(final Bitmap bitmap){
		// Two steps, first ask server for upload URL, then upload image

        progressDialog = ProgressDialog.show(context, context.getString(R.string.please_wait), context.getString(R.string.connecting_with_server), true);
        List<NameValuePair> params = HTTPTask.getParams(context);
		
		new HTTPTask(context, Constants.TASK.UPLOAD_IMAGE, new HTTPTask.Callback() {
            String uploadUrl = null;
			@Override
			public void parseResponse(JSONObject jsonObject){
				if(Utils.isSuccess(jsonObject)){
                    try {
                        uploadUrl = jsonObject.getString("uploadURL");
                    }catch(JSONException e){}
                }
			}
			@Override
			public void onDataReceived(JSONObject jsonObject) {
				if(uploadUrl != null) {
                    uploadImage(uploadUrl, bitmap);
                }else{
                    progressDialog.dismiss();
					Toast.makeText(context, context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				}
			}
		}).executeParallel(params);
	}
	

	private void uploadImage(String uploadURL, Bitmap bitmap){
		List<NameValuePair> params = HTTPTask.getParams(context);
        if(!uploadForUserProfile){
            params.add(new NameValuePair("just_upload", 1));
        }
		new HTTPTask(context, uploadURL, bitmap, new HTTPTask.Callback() {
			@Override
			public void onDataReceived(JSONObject jsonObject) {
                progressDialog.dismiss();
				if(jsonObject == null){
                    Toast.makeText(context, context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }else if(listener != null){
					listener.onJSON(jsonObject);
                }
			}
		}).executeParallel(params);
	}
}
