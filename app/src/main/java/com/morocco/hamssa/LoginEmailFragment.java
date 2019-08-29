package com.morocco.hamssa;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Created by hmontaner on 10/09/15.
 */
public class LoginEmailFragment extends Fragment {


    Context context;
    View rootView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        rootView = inflater.inflate(R.layout.fragment_login_email, container, false);
        setupSend();
        setupRecover();
        return rootView;
    }


    private void setupSend(){
        rootView.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = ((EditText) rootView.findViewById(R.id.userName)).getText().toString();
                String password = ((EditText) rootView.findViewById(R.id.password)).getText().toString();
                List<NameValuePair> params = HTTPTask.getParams(context);
                params.add(new NameValuePair("name", name));
                params.add(new NameValuePair("password", password));
                Constants.TASK task = Constants.TASK.LOGIN;

                final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                new HTTPTask(context, task, new HTTPTask.Callback() {
                    Integer errorStringId = null;

                    @Override
                    public void parseResponse(JSONObject jsonObject) {
                        if (Utils.isSuccess(jsonObject)) {
                            if (LoginActivity.parseUserInfo(context, jsonObject)) {
                                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                                sp.edit().putLong(LoginActivity.LAST_TIME, new Date().getTime()).apply();
                            } else {
                                errorStringId = R.string.unknown_error;
                            }
                        } else if (jsonObject == null) {
                            errorStringId = R.string.connection_error;
                        } else {
                            errorStringId = R.string.unknown_error;
                            try {
                                if (jsonObject.has("error")) {
                                    int error = jsonObject.getInt("error");
                                    switch (error) {
                                        case 1:
                                            errorStringId = R.string.user_not_found;
                                            break;
                                        case 2:
                                            errorStringId = R.string.wrong_password;
                                            break;
                                    }
                                }
                            } catch (JSONException e) {
                            }
                        }
                    }

                    @Override
                    public void onDataReceived(JSONObject jsonObject) {
                        progressDialog.dismiss();
                        if (errorStringId != null) {
                            TextView textView = (TextView) rootView.findViewById(R.id.banner);
                            textView.setText(getString(errorStringId));
                        } else {
                            LoginActivity.endLogin(context);
                        }
                    }
                }).executeParallel(params);
            }
        });
    }

    private void setupRecover(){
        rootView.findViewById(R.id.recover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = ((EditText) rootView.findViewById(R.id.userName)).getText().toString();

                List<NameValuePair> params = HTTPTask.getParams(context);
                params.add(new NameValuePair("email", email));
                Constants.TASK task = Constants.TASK.RECOVER_PASSWORD;

                final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                new HTTPTask(context, task, new HTTPTask.Callback() {
                    Integer errorStringId = null;

                    @Override
                    public void parseResponse(JSONObject jsonObject) {
                        if (Utils.isSuccess(jsonObject)) {

                        } else if (jsonObject == null) {
                            errorStringId = R.string.connection_error;
                        } else {
                            errorStringId = R.string.unknown_error;
                            try {
                                if (jsonObject.has("error")) {
                                    int error = jsonObject.getInt("error");
                                    switch (error) {
                                        case 1:
                                            errorStringId = R.string.email_not_found;
                                            break;
                                        case 2:
                                            errorStringId = R.string.error_sending_email;
                                            break;
                                        case 3:
                                            errorStringId = R.string.email_not_valid;
                                            break;
                                    }
                                }
                            } catch (JSONException e) {
                            }
                        }
                    }

                    @Override
                    public void onDataReceived(JSONObject jsonObject) {
                        progressDialog.dismiss();
                        Integer stringId = errorStringId;
                        if (errorStringId == null) {
                            stringId = R.string.password_recovered;
                        }
                        new AlertDialog.Builder(context)
                                .setMessage(getString(stringId))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                }).executeParallel(params);
            }
        });
    }

}
