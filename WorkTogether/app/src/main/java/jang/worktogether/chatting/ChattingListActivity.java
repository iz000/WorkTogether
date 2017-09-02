package jang.worktogether.chatting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.DesignUtil;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.UserProfileActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.ChatRoom;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;
import jang.worktogether.fileandimage.FileListActivity;
import jang.worktogether.fileandimage.ImageListActivity;
import jang.worktogether.group.GroupListFragment;

import static jang.worktogether.R.id.tabLayout;
import static jang.worktogether.R.id.toolbar;
import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class ChattingListActivity extends BaseActivity{

    HttpUtil httpUtil;
    WTapplication wtApplication;
    DBManager dbManager;
    SQLiteDatabase db;

    DrawerLayout mainLayout;
    Toolbar toolbar;
    TabLayout tabLayout;
    EnteredChattingListFragment enteredChattingListFragment;
    NotEnteredChattingListFragment notEnteredChattingListFragment;

    BroadcastReceiver broadcastReceiver;

    public static String NEW_ENTERED = "entered";
    public static String NEW_NOTENTERED = "notentered";
    public static String NEW_MEMBER = "newmember";

    //NavigationView 부분
    TextView navigationGroupName;
    TextView navigationGroupContent;
    ListView navigationUser;
    ChattingNavigationAdapter chattingNavigationAdapter;
    LocalBroadcastManager broadCaster;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_list);
        wtApplication = (WTapplication)getApplicationContext();
        dbManager = new DBManager(this, wtApplication.getMyself().getId());
        db = dbManager.getReadableDatabase();

        mainLayout = (DrawerLayout)findViewById(R.id.chatting_list_mainLayout);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.back_arrow);
        int px = DesignUtil.dpToPx(this, 30);
        Drawable drawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                px, px, true));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        getSupportActionBar().setTitle(null);
        ((TextView)findViewById(R.id.title)).setTypeface(Typeface.createFromAsset(getAssets(),
                "HelveticaBold.ttf"));

        tabLayout = (TabLayout)findViewById(R.id.tabLayout);

        enteredChattingListFragment = new EnteredChattingListFragment();
        replaceFragment(enteredChattingListFragment);
        tabLayout.addTab(tabLayout.newTab().setText("Entered"), 0, true);
        tabLayout.addTab(tabLayout.newTab().setText("Not Entered"), 1);
        notEnteredChattingListFragment = new NotEnteredChattingListFragment();

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

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                    if(tab.getPosition() == 0){
                        replaceFragment(enteredChattingListFragment);
                    }
                    else {
                        replaceFragment(notEnteredChattingListFragment);
                    }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        httpUtil = new HttpUtil(this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Log.i("chat", ErrorcodeUtil.errorMessage(response));
                }
                else if(response.length() == 2){
                    //결과 아무것도 없을 때
                    Log.i("chat", "없음");
                }
                else{ //결과 처리
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        HashMap<String, ChatRoom> entered = new HashMap<>();
                        HashMap<String, ChatRoom> notEntered = new HashMap<>();
                        for(int i=0; i<jsonArray.length(); i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            ChatRoom chatRoom = new ChatRoom(jsonObject.getString("c_id"),
                                    jsonObject.getString("c_topic"));
                            chatRoom.setMemberNum(Integer.parseInt(jsonObject.getString("c_count")));
                            Cursor cursor = db.rawQuery("select chat_content, chat_type, " +
                                    "(select count(chat_num) " +
                                    "from chatting_log " +
                                    "where chat_read = 0 and group_id = "+wtApplication.getCurrentGroup().getId()
                                    +" and chat_id = "+chatRoom.getChatRoomID()+") as count " +
                                    "from chatting_log " +
                                    "where group_id = "+wtApplication.getCurrentGroup().getId()+
                                    " and chat_id = "+chatRoom.getChatRoomID()+
                                    " and chat_type != 'enter' and chat_type != 'out' " +
                                    "order by chat_num desc limit 1;   ", null);
                            while(cursor.moveToNext()){
                                String type = cursor.getString(cursor.getColumnIndex("chat_type"));
                                switch (type){
                                    case "text" :
                                        chatRoom.setLastChat(cursor.getString(cursor.getColumnIndex("chat_content")));
                                        break;
                                    case "file" :
                                        chatRoom.setLastChat("파일");
                                        break;
                                    case "image" :
                                        chatRoom.setLastChat("이미지");
                                        break;
                                }
                                chatRoom.setChatCount(cursor.getInt(cursor.getColumnIndex("count")));
                            }
                            if(jsonObject.getInt("c_enter") == 1){
                                entered.put(chatRoom.getChatRoomID(), chatRoom);
                            }
                            else{
                                notEntered.put(chatRoom.getChatRoomID(), chatRoom);
                            }
                        }
                        wtApplication.setCurrentGroupEnteredChatRooms(entered);
                        wtApplication.setCurrentGroupNotEnteredChatRooms(notEntered);
                        enteredChattingListFragment.dataLoaded();
                        notEnteredChattingListFragment.dataLoaded();
                        registerReceiver();
                        db.close();
                        dbManager.close();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        httpUtil.setUrl("chatting_list.php")
                .setData("g_id", wtApplication.getCurrentGroup().getId())
                .setData("u_id", wtApplication.getMyself().getId())
                .setUseSession(true)
                .postData();

        setNavigationView();
    }

    private void setNavigationView(){
        broadCaster = LocalBroadcastManager.getInstance(this);
        navigationGroupName = (TextView)findViewById(R.id.navigation_group_name);
        navigationGroupContent = (TextView)findViewById(R.id.navigation_group_content);
        navigationGroupName.setText(wtApplication.getCurrentGroup().getName());
        navigationGroupContent.setText(wtApplication.getCurrentGroup().getContent());
        navigationUser = (ListView)findViewById(R.id.navigation_group_user_list);
        chattingNavigationAdapter = new ChattingNavigationAdapter(this,
                wtApplication.getCurrentGroup().getGroupUsers());
        navigationUser.setAdapter(chattingNavigationAdapter);
        navigationUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ChattingListActivity.this, UserProfileActivity.class);
                intent.putExtra("type", "group");
                intent.putExtra("id", ((User)chattingNavigationAdapter.getItem(position)).getId());
                startActivity(intent);
            }
        });
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(NEW_ENTERED)){
                    enteredChattingListFragment.dataLoaded();
                }
                else if(intent.getAction().equals(NEW_NOTENTERED)){
                    notEnteredChattingListFragment.dataLoaded();
                }
                else if(intent.getAction().equals(NEW_MEMBER)){
                    if(chattingNavigationAdapter != null){
                        chattingNavigationAdapter.notifyDataSetChanged();
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NEW_ENTERED);
        intentFilter.addAction(NEW_NOTENTERED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wtApplication.clearChatRooms();
        wtApplication.setCurrentGroup(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        enteredChattingListFragment.dataLoaded();
        notEnteredChattingListFragment.dataLoaded();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.chatting_list_container, fragment);
        transaction.commit();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.navigation_icon : {
                if(!mainLayout.isDrawerOpen(GravityCompat.END)) {
                    chattingNavigationAdapter.notifyDataSetChanged();
                    mainLayout.openDrawer(GravityCompat.END);
                }
                break;
            }
            case R.id.navigation_group_file_list : {
                Intent intent = new Intent(ChattingListActivity.this, FileListActivity.class);
                intent.putExtra("g_id", wtApplication.getCurrentGroup().getId());
                startActivity(intent);
                break;
            }
            case R.id.navigation_group_image_list : {
                Intent intent = new Intent(ChattingListActivity.this, ImageListActivity.class);
                intent.putExtra("g_id", wtApplication.getCurrentGroup().getId());
                startActivity(intent);
                break;
            }
            case R.id.navigation_group_out_button : {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("그룹에서 나가시겠습니까?\n(그룹에서 나가면 그룹에서의 모든 채팅이 사라집니다.)")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                HttpUtil httpUtil = new HttpUtil(ChattingListActivity.this);
                                httpUtil.setCallback(new HttpUtil.Callback() {
                                    @Override
                                    public void callback(String response) {
                                        if(response.length() == 3){
                                            Toast.makeText(ChattingListActivity.this,
                                                    ErrorcodeUtil.errorMessage(response),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            String groupID = wtApplication.getCurrentGroup().getId();
                                            if(!response.equals("0")){
                                                //그룹에서 들어왔던 채팅방 모두 나가는 부분
                                                try {
                                                    JSONArray jsonArray = new JSONArray(response);
                                                    for(int i=0; i<jsonArray.length(); i++){
                                                        Packet packet = new Packet(SocketService.CHATTING_OUT);
                                                        packet.addData(groupID,
                                                                jsonArray.getString(i),
                                                                wtApplication.getMyself().getName());
                                                        Intent intent = new Intent(SocketService.CHATTING_OUT);
                                                        intent.putExtra("Packet", packet.toByteArray());
                                                        broadCaster.sendBroadcast(intent);
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            wtApplication.getMyself().getGroups().remove(
                                                    wtApplication.getCurrentGroup().getId()
                                            );
                                            wtApplication.setCurrentGroup(null);
                                            //SQLITE에서 그룹 채팅 내용들도 다 지움
                                            DBManager dbManager = new DBManager(ChattingListActivity.this,
                                                    wtApplication.getMyself().getId());
                                            SQLiteDatabase db = dbManager.getWritableDatabase();
                                            db.execSQL("delete from "+ FeedReaderContract.FeedEntry.TABLE_NAME
                                                    +" where group_id = "+groupID);
                                            db.close();
                                            dbManager.close();
                                            ChattingListActivity.this.finish();
                                        }
                                    }
                                });
                                httpUtil.setUrl("group_out.php")
                                        .setData("g_id", wtApplication.getCurrentGroup().getId())
                                        .setUseSession(true)
                                        .postData();
                            }
                        })
                        .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
                break;
            }
        }
    }
}
