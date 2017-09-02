package jang.worktogether.group;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.LoginActivity;
import jang.worktogether.basic.SplashActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;

public class MainActivity extends AppCompatActivity {

    TabLayout tabLayout;
    Toolbar toolbar;
    InputMethodManager inputMethodManager;
    WTapplication wtApplication;
    HttpUtil httpUtil; // 친구 목록을 불러오기 위한 http통신

    GroupListFragment groupListFragment;
    FriendsFragment friendsFragment;
    RequestFragment requestFragment;
    ProfileFragment profileFragment;

    BroadcastReceiver broadcastReceiver;
    BroadcastReceiver loginBroadcastReceiver;

    public static String NEW_FRIEND = "NewFriend";
    public static String NEW_GROUP = "NewGroup";
    public static String NEW_FRIEND_REQUEST = "NewFriendRequest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wtApplication = (WTapplication)getApplicationContext();

        loginBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(LoginActivity.RE_LOGIN_NEEDED)){
                    MainActivity.this.finish();
                    Intent loginIntent = new Intent(MainActivity.this, SplashActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                    wtApplication.clearApplication();
                    startActivity(loginIntent);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(loginBroadcastReceiver,
                new IntentFilter(LoginActivity.RE_LOGIN_NEEDED));

        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.main_mainLayout), this);
        inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

        toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        ((TextView)findViewById(R.id.title)).setTypeface(Typeface.createFromAsset(getAssets(),
                "HelveticaBold.ttf"));

        tabLayout = (TabLayout)findViewById(R.id.tabLayout);

        replaceFragment(new GroupListFragment());
        tabLayout.addTab(tabLayout.newTab().setText("GROUPS"), 0, true);
        tabLayout.addTab(tabLayout.newTab().setText("FRIENDS"), 1);
        tabLayout.addTab(tabLayout.newTab().setText("REQUEST"),2);
        tabLayout.addTab(tabLayout.newTab().setText("PROFILE"), 3);

        Typeface tf = Typeface.createFromAsset(getAssets(), "HelveticaBold.ttf");
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = vg.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
            int tabChildsCount = vgTab.getChildCount();
            for (int i = 0; i < tabChildsCount; i++) {
                View tabViewChild = vgTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    ((TextView) tabViewChild).setTypeface(tf);
                }
            }
        }

        groupListFragment = new GroupListFragment();
        friendsFragment = new FriendsFragment();
        requestFragment = new RequestFragment();
        profileFragment = new ProfileFragment();
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                inputMethodManager.hideSoftInputFromWindow(findViewById(R.id.main_mainLayout).getWindowToken(), 0);
                if(tab.getPosition() == 0){
                    replaceFragment(groupListFragment);
                }
                else if(tab.getPosition() == 1){
                    replaceFragment(friendsFragment);
                }
                else if(tab.getPosition() == 2){
                    replaceFragment(requestFragment);
                }
                else{
                    replaceFragment(profileFragment);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        int noti = getIntent().getIntExtra("noti", 0);
        switch (noti){
            case 251 : {
                TabLayout.Tab tab = tabLayout.getTabAt(2);
                tab.select();
                break;
            }
            case 255 : {
                TabLayout.Tab tab = tabLayout.getTabAt(0);
                tab.select();
                break;
            }
            case 256 : {
                TabLayout.Tab tab = tabLayout.getTabAt(1);
                tab.select();
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        httpUtil = new HttpUtil(this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(MainActivity.this, ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT)
                            .show();
                }
                else{
                    try{
                        JSONArray jsonArray = new JSONArray(response);
                        int length = jsonArray.length();
                        HashMap<String, User> friends = new HashMap<>();
                        for(int i=0; i<length; i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            friends.put(jsonObject.getString("u_id"),
                                    new User(jsonObject.getString("u_id"),
                                    jsonObject.getString("u_name"),
                                    jsonObject.getString("u_status"),
                                    jsonObject.getString("u_profile"),
                                    User.Relation.Friend));
                        }
                        wtApplication.getMyself().setFriends(friends);
                        friendsFragment.friendListLoaded();
                        registerReceiver();
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        httpUtil.setUrl("friend_list.php")
                .setUseSession(true)
                .postData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(NEW_FRIEND)){
                    if(friendsFragment != null){
                        friendsFragment.friendListLoaded();
                    }
                    if(requestFragment != null){
                        requestFragment.requestChanged();
                    }
                }
                else if(intent.getAction().equals(NEW_GROUP)){
                    if(groupListFragment != null){
                        groupListFragment.groupListChanged();
                    }
                }
                else if(intent.getAction().equals(NEW_FRIEND_REQUEST)){
                    if(requestFragment != null){
                        Log.i("chat", "b받음");
                        requestFragment.requestChanged();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NEW_FRIEND);
        intentFilter.addAction(NEW_GROUP);
        intentFilter.addAction(NEW_FRIEND_REQUEST);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
