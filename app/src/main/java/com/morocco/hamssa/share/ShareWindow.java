package com.morocco.hamssa.share;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;
import com.facebook.messenger.ShareToMessengerParamsBuilder;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.morocco.hamssa.R;
import com.morocco.hamssa.adapters.MyListAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hmontaner on 31/05/15.
 */
public class ShareWindow {
    PopupWindow popupWindow;
    Activity activity;
    String shareText, imageUrl;
    public ShareWindow(Activity activity, String shareText, String imageUrl){
        this.activity = activity;
        this.shareText = shareText;
        this.imageUrl = imageUrl;
        initialize();
        popupWindow = new PopupWindow(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.invitation_list_layout, null);

        popupWindow.setHeight(ActionBar.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth(ActionBar.LayoutParams.WRAP_CONTENT);
        //popupWindow.setHeight(200);
        //popupWindow.setWidth(400);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        //popupWindow.setBackgroundDrawable(new BitmapDrawable());
        //popupWindow.setBackgroundDrawable(new ColorDrawable(R.color.background));
        if(Build.VERSION.SDK_INT >= 21) {
            popupWindow.setElevation(10);
        }

        manageList((ListView)view.findViewById(R.id.list));

        popupWindow.setContentView(view);

    }

    public void show(View anchorView){
        popupWindow.showAsDropDown(anchorView);
    }

    public void showCenter(View anchorView){
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }

    static class Item implements MyListAdapter.Viewable{
        PROVIDER provider;
        String name;
        int resId;
        public Item(PROVIDER provider, String name, int resId){
            this.name = name;
            this.resId = resId;
            this.provider = provider;
        }
        class ViewHolder{
            public TextView name;
            public ImageView image;
            public ViewHolder(View view){
                name = (TextView)view.findViewById(R.id.name);
                image = (ImageView)view.findViewById(R.id.image);
            }
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ViewHolder viewHolder;
            if(convertView == null){
                LayoutInflater inf = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inf.inflate(R.layout.invitation_item, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder)convertView.getTag();
            }
            viewHolder.name.setText(name);
            Context context = parent.getContext();
            viewHolder.image.setImageDrawable(context.getResources().getDrawable(resId/*, context.getTheme()*/));

            return convertView;
        }
        public PROVIDER getProvider(){
            return provider;
        }
    }
    public enum PROVIDER{
        FACEBOOK_INVITATION,
        FACEBOOK_MESSENGER,
        WHATSAPP,
        HANGOUT,
        OTHERS,
        GOOGLE_PLAY;

        public String getPackage(){
            if(this == FACEBOOK_MESSENGER){
                return "com.facebook.orca";
            }else if(this == WHATSAPP){
                return "com.whatsapp";
            }else if(this == HANGOUT){
                return "com.google.android.talk";
            }
            return null;
        }
    }
    static List<Item> items = new ArrayList<>();
    private void initialize(){
        items.clear();
        items.add(new Item(PROVIDER.FACEBOOK_INVITATION, "Facebook", R.drawable.facebook));
        items.add(new Item(PROVIDER.WHATSAPP, "Whatsapp", R.drawable.whatsapp));
        items.add(new Item(PROVIDER.FACEBOOK_MESSENGER, "Facebook Messenger", R.drawable.messenger));
        items.add(new Item(PROVIDER.HANGOUT, "Hangout", R.drawable.hangout));
        //items.add(new Item(PROVIDER.GOOGLE_PLAY, "Google Play Invitation", R.drawable.google_play));
        items.add(new Item(PROVIDER.OTHERS, activity.getString(R.string.others), R.drawable.others));
    }

    private boolean isInstalled(String packageString, Context context){
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageString, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    private void manageList(ListView listView){
        Iterator<Item> it = items.iterator();
        while(it.hasNext()){
            Item item = it.next();
            String packageName = item.provider.getPackage();
            if(packageName != null && !isInstalled(packageName, listView.getContext())){
                it.remove();
            }
        }
        MyListAdapter<Item> adapter = new MyListAdapter<>(listView.getContext(), items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PROVIDER provider = items.get(position).getProvider();
                onProviderClick(provider);
            }
        });
    }

    private void onProviderClick(PROVIDER provider) {
        popupWindow.dismiss();
        switch (provider){
            case FACEBOOK_INVITATION:
                //inviteFacebook();
                shareFacebook();
                break;
            /*case GOOGLE_PLAY:
                inviteGooglePlay();
                return;*/
            case FACEBOOK_MESSENGER:
                shareMessenger();
                break;
            case WHATSAPP:
                shareWhatsapp();
                return;
            case HANGOUT:
                shareHangout();
                break;
            case OTHERS:
                shareOthers();
                break;
        }
    }

    private void shareWhatsapp() {
        String title = activity.getString(R.string.app_name);
        String subject = shareText;
        String text = activity.getString(R.string.download)+" "+activity.getString(R.string.app_name)+" "+infoUrl;
        shareGeneric(PROVIDER.WHATSAPP, title, subject, text);
    }
    private void shareHangout() {
        String title = activity.getString(R.string.app_name);
        String subject = null;
        String text = shareText+" - "+activity.getString(R.string.download)+" "+activity.getString(R.string.app_name)+" "+infoUrl;
        if(shareText == null){
            text = activity.getString(R.string.download)+" "+activity.getString(R.string.app_name)+" "+infoUrl;
        }
        shareGeneric(PROVIDER.HANGOUT, title, subject, text);
    }
    private void shareMessenger(){
        String title = activity.getString(R.string.app_name);
        String subject = null;
        String text = shareText+" - "+infoUrl;
        if(shareText == null){
            text = infoUrl;
        }
        shareGeneric(PROVIDER.FACEBOOK_MESSENGER, title, subject, text);
    }
    private void shareOthers(){
        String title = activity.getString(R.string.app_name);
        String subject = activity.getString(R.string.download)+" "+activity.getString(R.string.app_name);
        String text = shareText+" - "+activity.getString(R.string.download)+" "+activity.getString(R.string.app_name)+" "+infoUrl;
        shareGeneric(PROVIDER.OTHERS, title, subject, text);
    }

    private static final String googlePlayUrl = "https://play.google.com/store/apps/details?id=com.morocco.hamssa";
    private static final String infoUrl = "http://foot-chat.appspot.com/info.html";
    private void shareGeneric(PROVIDER provider, String title, String subject, String text){

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        if(title != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        if(subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if(text != null) {
            intent.putExtra(Intent.EXTRA_TEXT, text);
        }

        intent.setType("text/plain");
        String packageString = provider.getPackage();
        if(packageString != null){
            intent.setPackage(packageString);
        }
        activity.startActivity(intent);
    }

    private void shareFacebook(){
        /*
        ShareButton shareButton = (ShareButton)findViewById(R.id.fb_share_button);
        shareButton.setShareContent(content);
        */
        //callbackManager = CallbackManager.Factory.create();
        ShareDialog shareDialog = new ShareDialog(activity);

        if (ShareDialog.canShow(ShareLinkContent.class)) {
            if(shareText == null){
                shareText = activity.getString(R.string.promo_text);
            }
            ShareLinkContent.Builder builder = new ShareLinkContent.Builder()
                    .setContentTitle(activity.getString(R.string.app_name))
                    .setContentDescription(shareText)
                    //.setContentUrl(Uri.parse("http://foot-chat.appspot.com/post.jsp"))
                    .setContentUrl(Uri.parse("https://fb.me/1643149139279455"));
            if(imageUrl != null) {
                builder.setImageUrl(Uri.parse(imageUrl));
            }

            ShareLinkContent linkContent = builder.build();

            shareDialog.show(linkContent);
        }
    }
}
