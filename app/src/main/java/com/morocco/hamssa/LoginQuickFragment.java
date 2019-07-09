package com.morocco.hamssa;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.TopicCursorRecyclerViewAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by hmontaner on 10/09/15.
 */
public class LoginQuickFragment extends Fragment {


    Context context;
    View rootView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        rootView = inflater.inflate(R.layout.fragment_login_quick, container, false);
        setupSend();
        return rootView;
    }


    private void setupSend(){
        rootView.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) rootView.findViewById(R.id.editText);
                String name = editText.getText().toString();
               List<NameValuePair> params = HTTPTask.getParams(context);
                Map<String, Object> data=new HashMap<>();
                data.put("name", name);
                data.put("language", Locale.getDefault().getLanguage());
                Constants.TASK task = Constants.TASK.CREATE_USER;
                FirebaseFunctions functions = FirebaseFunctions.getInstance();
                final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                functions.getHttpsCallable(task.toString())
                        .call(data)
                        .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                            @Override
                            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                                Integer errorStringId = null;
                                   if(task.isSuccessful()) {
                                       try {
                                           Gson gson = new Gson();
                                           String result = gson.toJson(task.getResult().getData());
                                           JSONObject jsonObject = new JSONObject(result);
                                           if (Utils.isSuccess(jsonObject)) {
                                               if (LoginActivity.parseUserInfo(context, jsonObject)) {
                                                   SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                                                   sp.edit().putLong(LoginActivity.LAST_TIME, new Date().getTime()).apply();
                                               } else {
                                                   errorStringId = R.string.unknown_error;
                                               }
                                           } else {
                                               int error = jsonObject.getInt("error");
                                               switch (error) {
                                                   case 1:
                                                       errorStringId = R.string.name_min_length;
                                                       break;
                                                   case 2:
                                                       errorStringId = R.string.name_max_length;
                                                       break;
                                                   case 3:
                                                       errorStringId = R.string.name_characters;
                                                       break;
                                                   case 4:
                                                       errorStringId = R.string.name_duplicated;
                                                       break;
                                               }
                                           }

                                       } catch (JSONException e) {

                                       }
                                   }
                                    progressDialog.dismiss();
                                    if (errorStringId != null) {
                                        TextView textView = (TextView) rootView.findViewById(R.id.banner);
                                        textView.setText(getString(errorStringId));
                                    } else {
                                        LoginActivity.endLogin(context);
                                    }
                                }


                        });
                       /*         addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                            @Override
                            public void onSuccess(HttpsCallableResult httpsCallableResult) {
                                Gson gson=new Gson();
                                String s = gson.toJson(httpsCallableResult.getData());
                                String result= gson.toJson(httpsCallableResult.getData());
                            }
                        });*/

            }
        });
    }


}
