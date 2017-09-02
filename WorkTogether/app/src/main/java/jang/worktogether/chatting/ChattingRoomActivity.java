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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jang.worktogether.R;
import jang.worktogether.Utils.DesignUtil;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.FileUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.UserProfileActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.ChatMessage;
import jang.worktogether.basic.basic_class.ChatRoom;
import jang.worktogether.basic.basic_class.FileMessage;
import jang.worktogether.basic.basic_class.ImageMessage;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;
import jang.worktogether.fileandimage.FileListActivity;
import jang.worktogether.fileandimage.ImageListActivity;
import jang.worktogether.fileandimage.ImageViewActivity;

import static android.view.View.GONE;

public class ChattingRoomActivity extends BaseActivity{


    private ChatRoom thisChatRoom;
    private LocalBroadcastManager broadCaster;
    private HttpUtil httpUtil;
    private WTapplication wtApplication;

    private BroadcastReceiver broadcastReceiver;

    public static final String USER_ENTER = "enter";
    public static final String USER_OUT = "out";
    public static final String CHAT_MESSAGE = "chat_message";
    public static final String FILE_MESSAGE = "file_message";
    public static final String IMAGE_MESSAGE = "image_message";
    public static final String FILE_DOWNLOAD_FINISHED = "file_downloaded";
    public static final String FILE_DOWNLOADING = "file_downloading";
    public static final String FILE_UPLOAD_START = "file_upload";
    public static final String FILE_UPLOADING = "file_uploading";
    public static final String FILE_UPLOAD_FINISHED = "file_uploaded";
    private static final int REQUEST_CHOOSER = 1234;
    private static final int IMAGE_GALLERY = 2345;
    private static final int IMAGE_CAMERA = 3456;
    private static final int IMAGE_CROP = 4567;
    private static final int THUMBNAIL_MAX_SIZE = 400; // 픽셀단위

    DrawerLayout mainLayout;
    LinearLayout buttons;
    EditText chatting_input;
    Toolbar toolbar;
    Button sendButton;

    RecyclerView chatting_view;
    RecyclerView.LayoutManager layoutManager;
    ChattingAdapter chattingAdapter;
    ArrayList<ChatMessage> chatMessages; // message가 다 로딩되면 임시 메세지들을 끝에 합쳐줌.
    ArrayList<ChatMessage> tempMessages; // message가 다 로딩되기 전에 임시로 여기에 저장해줌.
    boolean messageLoaded = false; // message가 다 로딩되었는지를 표현해주는 변수

    boolean keyBoardStateFlag = false; //

    //navigation view
    TextView navigationTopic;
    ListView navigationUser;
    ChattingNavigationAdapter chattingNavigationAdapter;
    private SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String directoryPath;

    //파일 다운로드 시 progressbar
    ProgressBar progressBar;
    LinearLayout progressLayout;
    int downloadPosition; // 다운로드하는 파일의 위치

    //파일 업로드 시 progressbar
    ProgressBar uploadProgressBar;

    //맨 앞에있는 chat의 번호
    int firstMessageNum;
    //더 불러오기 전 이전 메세지 리스트의 마지막 position
    int beforeMessageLastNum;

    //스크롤 중인지 아닌지
    boolean scrollFlag = false;

    //이미지를 크롭했을 시 uri
    Uri imageUri = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_room);
        wtApplication = (WTapplication)getApplicationContext();
        broadCaster = LocalBroadcastManager.getInstance(this);
        thisChatRoom = wtApplication.getCurrentChatRoom();

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {

            private float downY, upY;
            static final int MIN_DISTANCE = 100;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                chatting_input.clearFocus();
                int action = event.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    downY = event.getY();
                    return false;
                }
                else if(action == MotionEvent.ACTION_UP){
                    upY = event.getY();
                    int deltaY = Math.abs((int)downY - (int)upY);
                    if(deltaY < MIN_DISTANCE){
                        KeyboardUtil.hideKeyboard(v, ChattingRoomActivity.this);
                        keyBoardStateFlag = false;
                    }
                }
                return false;
            }
        };

        mainLayout = (DrawerLayout)findViewById(R.id.chatting_mainLayout);
        KeyboardUtil.setupHideKeyBoard(mainLayout, this, onTouchListener);

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
                        HashMap<String, User> users = new HashMap<>();
                        for(int i=0; i<jsonArray.length(); i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            User user = new User(jsonObject.getString("u_id"),
                                    jsonObject.getString("u_name"),
                                    jsonObject.getString("u_status"),
                                    jsonObject.getString("u_profile"),
                                    User.Relation.NoRelation);
                            users.put(user.getId(), user);
                        }
                        wtApplication.getCurrentChatRoom().setUsers(users);
                        loadMessages();
                        setNavigationView();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        httpUtil.setUrl("chatting_users.php")
                .setUseSession(true)
                .setData("c_id", wtApplication.getCurrentChatRoom().getChatRoomID())
                .postData();
        chatting_input = (EditText)findViewById(R.id.chatting_input);

        buttons = (LinearLayout)findViewById(R.id.buttons);
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        ((TextView)findViewById(R.id.title)).setTypeface(Typeface.createFromAsset(getAssets(),
                "HelveticaBold.ttf"));

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.back_arrow);
        int px = DesignUtil.dpToPx(this, 30);
        Drawable drawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                px, px, true));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        chatting_view = (RecyclerView)findViewById(R.id.chatting_view);
        layoutManager = new LinearLayoutManager(this);
        tempMessages = new ArrayList<>();

        sendButton = (Button)findViewById(R.id.chatting_send);
        sendButton.setOnTouchListener(null); // 온터치 리스너 제거해줌 전송버튼에서는

        chatting_view.setOnTouchListener(onTouchListener);
        final View activityRootView = mainLayout;
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > DesignUtil.dpToPx(ChattingRoomActivity.this, 200)) {
                    //키보드가 올라오면 스크롤 포지션을 맨 밑으로 내림
                    if(!keyBoardStateFlag){
                        /*다시 키보드가 내려갔다 올라왔을 때는 다시 스크롤 내려주면 됨.
                        * */
                        chatting_view.scrollToPosition(0);
                        keyBoardStateFlag = true;
                    }
                }
            }
        });


        directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + File.separator + wtApplication.getCurrentGroup().getId() +
                File.separator + wtApplication.getCurrentChatRoom().getChatRoomID() +
                File.separator + "wtfiles";

        uploadProgressBar = (ProgressBar)findViewById(R.id.chatting_upload_progress);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        wtApplication.setCurrentChatRoom(thisChatRoom);
        loadMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wtApplication.setCurrentChatRoom(null);
    }

    @Override
    public void onBackPressed() {
        if(mainLayout.isDrawerOpen(GravityCompat.END)){
            mainLayout.closeDrawer(GravityCompat.END);
        }
        else{
            super.onBackPressed();
        }
    }

    private void setNavigationView(){
        navigationTopic = (TextView)findViewById(R.id.navigation_topic);
        navigationTopic.setText(wtApplication.getCurrentChatRoom().getChatRoomTopic());
        navigationUser = (ListView)findViewById(R.id.navigation_user_list);
        chattingNavigationAdapter = new ChattingNavigationAdapter(this,
                wtApplication.getCurrentChatRoom().getUsers());
        navigationUser.setAdapter(chattingNavigationAdapter);
        navigationUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ChattingRoomActivity.this, UserProfileActivity.class);
                intent.putExtra("type", "chatting");
                intent.putExtra("id", ((User)chattingNavigationAdapter.getItem(position)).getId());
                startActivity(intent);
            }
        });
    }

    private void loadMessages(){
        chatMessages = new ArrayList<>();
        DBManager dbManager = new DBManager(this, wtApplication.getMyself().getId());
        SQLiteDatabase db = dbManager.getWritableDatabase();
        int readCount = wtApplication.getCurrentChatRoom().getChatCount();
        Cursor cursor = db.rawQuery("select * from " + FeedReaderContract.FeedEntry.TABLE_NAME +
        " where group_id = "+wtApplication.getCurrentGroup().getId()+" and chat_id = " +
                wtApplication.getCurrentChatRoom().getChatRoomID() +
                " order by " + FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM +" desc " +
                "limit "+Math.max(readCount, 100), null);
        db.execSQL("update "+FeedReaderContract.FeedEntry.TABLE_NAME+
                " set " + FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ + "= 1 " +
                "where group_id = "+wtApplication.getCurrentGroup().getId()+" and " +
                "chat_id = " + wtApplication.getCurrentChatRoom().getChatRoomID()+" " +
                "and chat_read = 0");
        //쿼리를 실행하자마자 receiver 등록
        registerReceiver();

        while(cursor.moveToNext()){
            String userId = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID));
            String type = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE));
            Long time = cursor.getLong(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME));
            String message = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT));
            ChatMessage chatMessage;
            switch (type){
                case "text" : {
                    chatMessage = new ChatMessage(userId, message, time, ChatMessage.Type.text);
                    break;
                }
                case "file" : {
                    String[] messageStr = message.split("\\|", 4);
                    chatMessage = new FileMessage(userId, messageStr[0],
                            Long.parseLong(messageStr[1]), messageStr[3]+"."+messageStr[2],
                            time, ChatMessage.Type.file);
                    break;
                }
                case "image" : {
                    String[] messageStr = message.split("\\|", 4);
                    chatMessage = new ImageMessage(userId, messageStr[0],
                            messageStr[2]+"."+messageStr[1], time,
                            wtApplication.getCurrentGroup().getId()+"/"+
                                    wtApplication.getCurrentChatRoom().getChatRoomID()+"/"+
                                    userId, ChatMessage.Type.image, messageStr[3]);
                    break;
                }
                case "enter" : {
                    chatMessage = new ChatMessage(message, ChatMessage.Type.enter);
                    break;
                }
                case "out" : {
                    chatMessage = new ChatMessage(message, ChatMessage.Type.out);
                    break;
                }
                default:
                    chatMessage = null;
            }
            if(chatMessage != null){
                //쿼리 결과가 거꾸로 나오기 때문에 맨 앞에서부터 넣어줌계속
                chatMessages.add(chatMessage);
            }
            firstMessageNum = cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM));
        }
        cursor.close();
        db.close();
        dbManager.close();
        //완료 후에 어댑터 등록
        chatMessages.addAll(0, tempMessages);
        tempMessages.clear();
        chattingAdapter = new ChattingAdapter(chatMessages);
        messageLoaded = true;
        chatting_view.setLayoutManager(layoutManager);
        chatting_view.setAdapter(chattingAdapter);
        ((LinearLayoutManager)layoutManager).setReverseLayout(true);
        chattingAdapter.setOnMessageClickCallback(new OnMessageClickCallback() {
            @Override
            public void onFileClick(final int position, final RecyclerView.ViewHolder holder) {
                final FileMessage fileMessage = (FileMessage)chattingAdapter.getItem(position);
                final File file = new File(directoryPath + File.separator + fileMessage.getMessage());
                if(file.exists() && fileMessage.getFileLength() == file.length()){
                    //파일이 존재해서 열기를 할 때
                    FileUtil.viewFile(ChattingRoomActivity.this, file);
                    return;
                }
                if(file.exists()){ // 파일이 이미 존재할 때
                    if(fileMessage.getFileLength() != file.length()){
                        //파일이 존재하지만 파일의 길이가 다를 때
                        final AlertDialog.Builder innerBuilder = new AlertDialog.Builder(ChattingRoomActivity.this);
                        innerBuilder.setMessage("불완전한 파일이 존재합니다. 이어서 다운로드 하시겠습니까?")
                                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setProgressLayout(holder, position);
                                        progressBar.setMax((int)(fileMessage.getFileLength()));
                                        progressBar.setProgress((int)file.length());
                                        FilePacket filePacket =
                                                new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                                        filePacket.addData(fileMessage.getFileID(),
                                                Long.toString(file.length()));
                                        Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                                        intent.putExtra("Packet", filePacket.toByteArray());
                                        broadCaster.sendBroadcast(intent);
                                        downloadPosition = position;
                                    }
                                })
                                .setNegativeButton("새로 다운로드", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setProgressLayout(holder, position);
                                        progressBar.setMax((int)fileMessage.getFileLength());
                                        FilePacket filePacket =
                                                new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                                        filePacket.addData(fileMessage.getFileID(), "0");
                                        Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                                        intent.putExtra("Packet", filePacket.toByteArray());
                                        broadCaster.sendBroadcast(intent);
                                        downloadPosition = position;
                                    }
                                });
                        innerBuilder.create().show();
                    }
                }
                else { // 파일이 존재하지 않아 아예처음 다운로드 할 때
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChattingRoomActivity.this);
                    builder.setMessage("파일을 다운로드 하시겠습니까?")
                            .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setProgressLayout(holder, position);
                                    progressBar.setMax((int)fileMessage.getFileLength());
                                    FilePacket filePacket =
                                            new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                                    filePacket.addData(fileMessage.getFileID(), "0");
                                    Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                                    intent.putExtra("Packet", filePacket.toByteArray());
                                    broadCaster.sendBroadcast(intent);
                                    downloadPosition = position;
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                }
            }

            private void setProgressLayout(RecyclerView.ViewHolder holder, int position){
                if(progressBar != null || progressLayout != null){
                    return;
                }
                if(chattingAdapter.getItem(position).getUserId().equals(wtApplication.getMyself().getId())){
                    progressBar = ((ChattingAdapter.ViewHolderMe)holder).file_download_progress_bar;
                    progressBar.setProgress(0);
                    progressBar.setLayoutParams(
                            new LinearLayout.LayoutParams(((ChattingAdapter.ViewHolderMe)holder)
                                    .message_text.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
                    progressLayout = ((ChattingAdapter.ViewHolderMe)holder).file_download_layout;
                }
                else {
                    progressBar = ((ChattingAdapter.ViewHolderYou)holder).your_file_download_progress_bar;
                    progressBar.setProgress(0);
                    progressBar.setLayoutParams(
                            new LinearLayout.LayoutParams(((ChattingAdapter.ViewHolderYou)holder)
                                    .yourMessage_text.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
                    progressLayout = ((ChattingAdapter.ViewHolderYou)holder).your_file_download_layout;
                }
            }

            @Override
            public void onCancelClick(int position, RecyclerView.ViewHolder viewHolder) {
                if(progressLayout != null){
                    progressLayout.setVisibility(GONE);
                }
                progressBar = null;
                progressLayout = null;
                FilePacket filePacket = new FilePacket(SocketService.CANCEL_FILE_DOWNLOAD);
                Intent intent = new Intent(SocketService.CANCEL_FILE_DOWNLOAD);
                intent.putExtra("Packet", filePacket.toByteArray());
                broadCaster.sendBroadcast(intent);
            }

            @Override
            public void onImageClick(int position) {
                ImageMessage imageMessage = (ImageMessage)chattingAdapter.getItem(position);
                Intent intent = new Intent(ChattingRoomActivity.this, ImageViewActivity.class);
                String path = ChattingRoomActivity.this.getCacheDir() + File.separator
                        + wtApplication.getCurrentGroup().getId() +
                        File.separator + wtApplication.getCurrentChatRoom().getChatRoomID() +
                        File.separator + "wtimages"
                        + File.separator + imageMessage.getImageName();
                intent.putExtra("path", path);
                intent.putExtra("id", imageMessage.getImageID());
                startActivity(intent);
            }
        });

        chatting_view.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int firstVisibleItemPosition = ((LinearLayoutManager)recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
                if(newState == RecyclerView.SCROLL_STATE_IDLE && firstVisibleItemPosition == 0){
                    loadMoreMessages();
                }
                if(newState == RecyclerView.SCROLL_STATE_IDLE){
                    scrollFlag = false;
                }
                else{
                    scrollFlag = true;
                }
            }
        });
    }

    private void loadMoreMessages(){
        beforeMessageLastNum = chatMessages.size();
        DBManager dbManager = new DBManager(this, wtApplication.getMyself().getId());
        SQLiteDatabase db = dbManager.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from " + FeedReaderContract.FeedEntry.TABLE_NAME +
                " where group_id = "+wtApplication.getCurrentGroup().getId()+" and chat_id = " +
                wtApplication.getCurrentChatRoom().getChatRoomID() +
                " and chat_num < " + firstMessageNum +
                " order by " + FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM +" desc " +
                "limit 100", null);
        while(cursor.moveToNext()){
            String userId = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID));
            String type = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE));
            Long time = cursor.getLong(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME));
            String message = cursor.getString(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT));
            ChatMessage chatMessage;
            switch (type){
                case "text" : {
                    chatMessage = new ChatMessage(userId, message, time, ChatMessage.Type.text);
                    break;
                }
                case "file" : {
                    String[] messageStr = message.split("\\|", 4);
                    chatMessage = new FileMessage(userId, messageStr[0],
                            Long.parseLong(messageStr[1]), messageStr[3]+"."+messageStr[2],
                            time, ChatMessage.Type.file);
                    break;
                }
                case "image" : {
                    String[] messageStr = message.split("\\|", 4);
                    chatMessage = new ImageMessage(userId, messageStr[0],
                            messageStr[2]+"."+messageStr[1], time,
                            wtApplication.getCurrentGroup().getId()+"/"+
                                    wtApplication.getCurrentChatRoom().getChatRoomID()+"/"+
                                    userId, ChatMessage.Type.image, messageStr[3]);
                    break;
                }
                case "enter" : {
                    chatMessage = new ChatMessage(message, ChatMessage.Type.enter);
                    break;
                }
                case "out" : {
                    chatMessage = new ChatMessage(message, ChatMessage.Type.out);
                    break;
                }
                default:
                    chatMessage = null;
            }
            if(chatMessage != null){
                //쿼리 결과가 거꾸로 나오기 때문에 맨 앞에서부터 넣어줌계속
                chatMessages.add(chatMessage);
            }
            firstMessageNum = cursor.getInt(cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM));
        }
        chattingAdapter.notifyDataSetChanged();
        if(cursor.getCount() != 0){
            chatting_view.smoothScrollToPosition(beforeMessageLastNum);
        }
        cursor.close();
        db.close();
        dbManager.close();
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()){
                    case CHAT_MESSAGE : {
                        String[] bodyStr = intent.getStringArrayExtra(CHAT_MESSAGE);
                        if(!messageLoaded){
                            ChatMessage chatMessage = new ChatMessage(bodyStr[0], bodyStr[4],
                                    Long.parseLong(bodyStr[3]), ChatMessage.Type.text);
                            tempMessages.add(0, chatMessage);
                            break;
                        }
                        ChatMessage chatMessage = new ChatMessage(bodyStr[0], bodyStr[4],
                                Long.parseLong(bodyStr[3]), ChatMessage.Type.text);
                        chatMessages.add(0, chatMessage);
                        chattingAdapter.notifyItemInserted(0);
                        if(!scrollFlag){
                            chatting_view.scrollToPosition(0);
                        }
                        break;
                    }
                    case FILE_MESSAGE : {
                        String[] bodyStr = intent.getStringArrayExtra(FILE_MESSAGE);
                        if(!messageLoaded){
                            ChatMessage chatMessage = new FileMessage(bodyStr[0], bodyStr[3],
                                    Integer.parseInt(bodyStr[4]),
                                    bodyStr[7]+"."+bodyStr[5], Long.parseLong(bodyStr[6]),
                                    ChatMessage.Type.file);
                            tempMessages.add(0, chatMessage);
                            break;
                        }
                        ChatMessage chatMessage = new FileMessage(bodyStr[0], bodyStr[3],
                                Integer.parseInt(bodyStr[4]),
                                bodyStr[7]+"."+bodyStr[5], Long.parseLong(bodyStr[6]),
                                ChatMessage.Type.file);
                        chatMessages.add(0, chatMessage);
                        chattingAdapter.notifyItemInserted(0);
                        if(!scrollFlag){
                            chatting_view.scrollToPosition(0);
                        }
                        break;
                    }
                    case IMAGE_MESSAGE : {
                        String[] bodyStr = intent.getStringArrayExtra(IMAGE_MESSAGE);
                        if(!messageLoaded){
                            ChatMessage chatMessage = new ImageMessage(bodyStr[0], bodyStr[3],
                                    bodyStr[5]+"."+bodyStr[4], Long.parseLong(bodyStr[5]),
                                    bodyStr[1]+"/"+bodyStr[2]+"/"+bodyStr[0],
                                    ChatMessage.Type.image, bodyStr[6]);
                            tempMessages.add(0, chatMessage);
                            break;
                        }
                        ChatMessage chatMessage = new ImageMessage(bodyStr[0], bodyStr[3],
                                bodyStr[5]+"."+bodyStr[4], Long.parseLong(bodyStr[5]),
                                bodyStr[1]+"/"+bodyStr[2]+"/"+bodyStr[0],
                                ChatMessage.Type.image, bodyStr[6]);
                        chatMessages.add(0, chatMessage);
                        chattingAdapter.notifyItemInserted(0);
                        if(!scrollFlag){
                            chatting_view.scrollToPosition(0);
                        }
                        break;
                    }
                    case FILE_DOWNLOADING : {
                        if(progressBar != null && progressLayout != null) {
                            if (progressLayout.getVisibility() == GONE) {
                                progressLayout.setVisibility(View.VISIBLE);
                                chatting_view.smoothScrollToPosition(downloadPosition);
                            }
                            progressBar.incrementProgressBy(intent.getIntExtra("read", 0));
                        }
                        break;
                    }
                    case FILE_DOWNLOAD_FINISHED : {
                        if(progressLayout != null){
                            progressLayout.setVisibility(GONE);
                            progressBar = null;
                            progressLayout = null;
                        }
                        break;
                    }
                    case FILE_UPLOAD_START : {
                        if(uploadProgressBar != null){
                            if (uploadProgressBar.getVisibility() == GONE) {
                                uploadProgressBar.setMax((int)intent.getLongExtra("upload", 0));
                                uploadProgressBar.setProgress(0);
                                uploadProgressBar.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    }
                    case FILE_UPLOADING : {
                        if(uploadProgressBar != null) {
                            uploadProgressBar.incrementProgressBy(intent.getIntExtra("upload", 0));
                        }
                        break;
                    }
                    case FILE_UPLOAD_FINISHED : {
                        if(uploadProgressBar != null){
                            uploadProgressBar.setVisibility(GONE);
                        }
                        Toast.makeText(ChattingRoomActivity.this, "업로드 완료", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case USER_ENTER : {
                        if(!messageLoaded){
                            String name = intent.getStringExtra(USER_ENTER);
                            ChatMessage chatMessage = new ChatMessage(name, ChatMessage.Type.enter);
                            tempMessages.add(0, chatMessage);
                            break;
                        }
                        String name = intent.getStringExtra(USER_ENTER);
                        ChatMessage chatMessage = new ChatMessage(name, ChatMessage.Type.enter);
                        chatMessages.add(0, chatMessage);
                        chattingAdapter.notifyItemInserted(0);
                        if(!scrollFlag){
                            chatting_view.scrollToPosition(0);
                        }
                        break;
                    }
                    case USER_OUT : {
                        if(!messageLoaded){
                            String name = intent.getStringExtra(USER_OUT);
                            ChatMessage chatMessage = new ChatMessage(name, ChatMessage.Type.out);
                            tempMessages.add(0, chatMessage);
                            break;
                        }
                        String name = intent.getStringExtra(USER_OUT);
                        ChatMessage chatMessage = new ChatMessage(name, ChatMessage.Type.out);
                        chatMessages.add(0, chatMessage);
                        chattingAdapter.notifyItemInserted(0);
                        if(!scrollFlag){
                            chatting_view.scrollToPosition(0);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(CHAT_MESSAGE);
        intentFilter.addAction(FILE_MESSAGE);
        intentFilter.addAction(IMAGE_MESSAGE);
        intentFilter.addAction(USER_ENTER);
        intentFilter.addAction(USER_OUT);
        intentFilter.addAction(FILE_DOWNLOAD_FINISHED);
        intentFilter.addAction(FILE_DOWNLOADING);
        intentFilter.addAction(FILE_UPLOAD_FINISHED);
        intentFilter.addAction(FILE_UPLOADING);
        intentFilter.addAction(FILE_UPLOAD_START);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.chatting_send : {
                if(chatting_input.getText().toString().equals("")){
                    break;
                }
                Packet packet = new Packet("100");
                packet.addData(wtApplication.getCurrentGroup().getId(),
                        wtApplication.getCurrentChatRoom().getChatRoomID(),
                        chatting_input.getText().toString());
                Intent intent = new Intent(SocketService.CHATTING_PACKET);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                chatting_input.setText("");
                break;
            }

            case R.id.chatting_plus : {
                chatting_input.clearFocus();
                if(buttons.getVisibility() == View.VISIBLE){
                    chatting_view.scrollToPosition(0);
                    buttons.setVisibility(GONE);
                }
                else{
                    chatting_view.scrollToPosition(0);
                    buttons.setVisibility(View.VISIBLE);
                }
                break;
            }

            case R.id.chatting_file : {
                chatting_input.clearFocus();
                Intent getContentIntent = com.ipaulpro.afilechooser.utils.FileUtils.
                        createGetContentIntent();
                Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                startActivityForResult(intent, REQUEST_CHOOSER);
                break;
            }

            case R.id.chatting_gallery : {
                chatting_input.clearFocus();
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, IMAGE_GALLERY);
                break;
            }

            case R.id.chatting_camera : {
                chatting_input.clearFocus();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, IMAGE_CAMERA);
                break;
            }

            case R.id.navigation_icon : {
                if(!mainLayout.isDrawerOpen(GravityCompat.END)) {
                    if(chattingNavigationAdapter != null){
                        chattingNavigationAdapter.notifyDataSetChanged();
                    }
                    mainLayout.openDrawer(GravityCompat.END);
                }
                break;
            }

            case R.id.navigation_file_list : {
                Intent intent = new Intent(ChattingRoomActivity.this, FileListActivity.class);
                intent.putExtra("g_id", wtApplication.getCurrentGroup().getId());
                intent.putExtra("c_id", wtApplication.getCurrentChatRoom().getChatRoomID());
                startActivity(intent);
                break;
            }

            case R.id.navigation_image_list : {
                Intent intent = new Intent(ChattingRoomActivity.this, ImageListActivity.class);
                intent.putExtra("g_id", wtApplication.getCurrentGroup().getId());
                intent.putExtra("c_id", wtApplication.getCurrentChatRoom().getChatRoomID());
                startActivity(intent);
                break;
            }

            case R.id.navigation_out_button : {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("채팅방에서 나가시겠습니까?\n(대화 내용이 모두 삭제됩니다)")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String currentChatRoomID = wtApplication.getCurrentChatRoom().getChatRoomID();
                                Packet packet = new Packet(SocketService.CHATTING_OUT);
                                packet.addData(wtApplication.getCurrentGroup().getId(),
                                        currentChatRoomID, wtApplication.getMyself().getName());
                                Intent intent = new Intent(SocketService.CHATTING_OUT);
                                intent.putExtra("Packet", packet.toByteArray());
                                broadCaster.sendBroadcast(intent);
                                wtApplication.getCurrentGroupNotEnteredChatRooms().put(currentChatRoomID,
                                        wtApplication.getCurrentGroupEnteredChatRooms()
                                                .remove(currentChatRoomID));
                                DBManager dbManager = new DBManager(ChattingRoomActivity.this,
                                        wtApplication.getMyself().getId());
                                SQLiteDatabase db = dbManager.getWritableDatabase();
                                db.execSQL("delete from "+ FeedReaderContract.FeedEntry.TABLE_NAME
                                        +" where chat_id = " +
                                        wtApplication.getCurrentChatRoom().getChatRoomID());
                                ChattingRoomActivity.this.finish();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_CHOOSER : {
                if(resultCode == RESULT_OK){
                    final Uri uri = data.getData();
                    String path = com.ipaulpro.afilechooser.utils.FileUtils.getPath(this, uri);
                    if(path == null){
                        Log.i("chat", "파일 path null");
                        return;
                    }
                    Intent intent = new Intent(SocketService.FILE_UPLOAD);
                    intent.putExtra("Path", path);
                    FilePacket packet = new FilePacket(SocketService.FILE_UPLOAD);
                    packet.setFileLength((int)(new File(path).length()));
                    packet.addData(wtApplication.getCurrentGroup().getId(),
                            wtApplication.getCurrentChatRoom().getChatRoomID(),
                            FilenameUtils.getExtension(path), FilenameUtils.getBaseName(path));
                    intent.putExtra("Packet", packet.toByteArray());
                    broadCaster.sendBroadcast(intent);
                }
                break;
            }
            case IMAGE_GALLERY : {
                if(resultCode == RESULT_OK){
                    Uri uriFromGallery = data.getData();
                    askCrop(uriFromGallery);
                }
                break;
            }
            case IMAGE_CAMERA : {
                if(resultCode == RESULT_OK){
                    Uri uriFromCamera = data.getData();
                    askCrop(uriFromCamera);
                }
                break;
            }
            case IMAGE_CROP : {
                if(imageUri != null) {
                    Intent intent = new Intent(SocketService.IMAGE_UPLOAD);
                    intent.putExtra("Path", imageUri.getPath());
                    FilePacket packet = new FilePacket(SocketService.IMAGE_UPLOAD);
                    packet.setFileLength((int)(new File(imageUri.getPath()).length()));
                    packet.addData(wtApplication.getCurrentGroup().getId(),
                            wtApplication.getCurrentChatRoom().getChatRoomID(),
                            FilenameUtils.getExtension(imageUri.getPath()), getExif(imageUri.getPath()));
                    intent.putExtra("Packet", packet.toByteArray());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    imageUri = null;
                }
                break;
            }
        }
    }

    private void askCrop(final Uri uri){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("이미지 편집")
                .setPositiveButton("자르기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = com.ipaulpro.afilechooser.utils.FileUtils
                                .getPath(ChattingRoomActivity.this, uri);
                        Log.i("chat", "path:" + path);
                        Log.i("chat", "uriPath:"+uri.getPath());
                        Intent intent = new Intent("com.android.camera.action.CROP");
                        intent.setDataAndType(uri, "image/*");
                        imageUri = getFileUri();
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent, IMAGE_CROP);
                    }
                })
                .setNegativeButton("보내기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = com.ipaulpro.afilechooser.utils.FileUtils
                                .getPath(ChattingRoomActivity.this, uri);
                        if(path == null){
                            Log.i("chat", "이미지 path null");
                            return;
                        }
                        Intent intent = new Intent(SocketService.IMAGE_UPLOAD);
                        intent.putExtra("Path", path);
                        FilePacket packet = new FilePacket(SocketService.IMAGE_UPLOAD);
                        packet.setFileLength((int)(new File(path).length()));
                        packet.addData(wtApplication.getCurrentGroup().getId(),
                                wtApplication.getCurrentChatRoom().getChatRoomID(),
                                FilenameUtils.getExtension(path), getExif(path));
                        intent.putExtra("Packet", packet.toByteArray());
                        broadCaster.sendBroadcast(intent);
                    }
                }).create().show();
    }

    public class ChattingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        final float density = getResources().getDisplayMetrics().density;
        ArrayList<ChatMessage> chatMessages;
        LayoutInflater layoutInflater;
        OnMessageClickCallback onMessageClickCallback;

        ChattingAdapter(ArrayList<ChatMessage> chatMessages){
            this.chatMessages = chatMessages;
            this.layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        void setOnMessageClickCallback(OnMessageClickCallback onMessageClickCallback) {
            this.onMessageClickCallback = onMessageClickCallback;
        }

        public ChatMessage getItem(int position){
            return this.chatMessages.get(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case 0: case 2: {
                    View v = layoutInflater.inflate(R.layout.recyclerview_my_message, parent, false);
                    return new ViewHolderMe(v);
                }
                case 1: case 3: {
                    View v = layoutInflater.inflate(R.layout.recyclerview_your_message, parent, false);
                    return new ViewHolderYou(v);
                }
                case 4 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_my_image, parent, false);
                    return new ViewHolderMyImage(v);
                }
                case 5 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_your_image, parent, false);
                    return new ViewHolderYourImage(v);
                }
                case 6 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_enter, parent, false);
                    return new ViewHolderEnter(v);
                }
                case 7 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_enter, parent, false);
                    return new ViewHolderEnter(v);
                }
                case 9 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_my_image, parent, false);
                    return new ViewHolderMyImage(v);
                }
                case 10 : {
                    View v = layoutInflater.inflate(R.layout.recyclerview_your_image, parent, false);
                    return new ViewHolderYourImage(v);
                }
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            ChatMessage chatMessage = chatMessages.get(position);
            if(chatMessage.getType() == ChatMessage.Type.text){
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    ViewHolderMe viewHolderMe = (ViewHolderMe)holder;
                    viewHolderMe.message_text.setText(chatMessage.getMessage());
                    viewHolderMe.message_time.setText(longTodate(chatMessage.getTime()));
                }
                else{
                    ViewHolderYou viewHolderYou = (ViewHolderYou)holder;
                    if(wtApplication.getCurrentChatRoom().getUsers()
                            .get(chatMessage.getUserId()) == null){
                        viewHolderYou.yourMessage_name.setText("알 수 없음");
                    }
                    else{
                        viewHolderYou.yourMessage_name.setText(
                                wtApplication.getCurrentChatRoom().
                                        getUsers().get(chatMessage.getUserId()).getName());
                    }
                    viewHolderYou.yourMessage_text.setText(chatMessage.getMessage());
                    viewHolderYou.yourMessage_time.setText(longTodate(chatMessage.getTime()));
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.file){
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    ViewHolderMe viewHolderMe = (ViewHolderMe)holder;
                    viewHolderMe.message_text.setText(Html.
                            fromHtml("<u>"+chatMessage.getMessage()+"</u>"));
                    viewHolderMe.message_text.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onMessageClickCallback.onFileClick(position, holder);
                        }
                    });
                    viewHolderMe.message_time.setText(longTodate(chatMessage.getTime()));
                    viewHolderMe.file_download_cancel_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onMessageClickCallback.onCancelClick(position, holder);
                        }
                    });
                }
                else{
                    ViewHolderYou viewHolderYou = (ViewHolderYou)holder;
                    if(wtApplication.getCurrentChatRoom().getUsers()
                            .get(chatMessage.getUserId()) == null){
                        viewHolderYou.yourMessage_name.setText("알 수 없음");
                    }
                    else{
                        viewHolderYou.yourMessage_name.setText(
                                wtApplication.getCurrentChatRoom().
                                        getUsers().get(chatMessage.getUserId()).getName());
                    }
                    viewHolderYou.yourMessage_text.setText(Html.
                            fromHtml("<u>"+chatMessage.getMessage()+"</u>"));
                    viewHolderYou.yourMessage_text.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            onMessageClickCallback.onFileClick(position, holder);
                            return false;
                        }
                    });
                    viewHolderYou.your_file_download_progress_bar.setLayoutParams(
                            new LinearLayout.LayoutParams(viewHolderYou.yourMessage_text.getWidth(),
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                    viewHolderYou.yourMessage_time.setText(longTodate(chatMessage.getTime()));
                    viewHolderYou.your_file_download_cancel_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onMessageClickCallback.onCancelClick(position, holder);
                        }
                    });
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.image) {
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    final ViewHolderMyImage viewHolderMyImage = (ViewHolderMyImage) holder;
                    viewHolderMyImage.message_time.setText(longTodate(chatMessage.getTime()));
                    viewHolderMyImage.message_image.requestLayout();
                    SimpleTarget<Bitmap> simpleTarget = new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            viewHolderMyImage.message_image.getLayoutParams().width =
                                    (int)(resource.getWidth()*density);
                            viewHolderMyImage.message_image.getLayoutParams().height =
                                    (int)(resource.getHeight()*density);
                            viewHolderMyImage.message_image.requestLayout();
                            viewHolderMyImage.message_image.setImageBitmap(resource);
                        }
                    };
                    Glide.with(ChattingRoomActivity.this).load(((ImageMessage)chatMessage).getThumbPath())
                            .asBitmap()
                            .error(R.drawable.loadfail)
                            .into(simpleTarget);
                    viewHolderMyImage.message_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onMessageClickCallback.onImageClick(position);
                        }
                    });
                }
                else{
                    final ViewHolderYourImage viewHolderYourImage = (ViewHolderYourImage) holder;
                    if(wtApplication.getCurrentChatRoom().getUsers()
                            .get(chatMessage.getUserId()) == null){
                        viewHolderYourImage.yourMessage_name.setText("알 수 없음");
                    }
                    else {
                        viewHolderYourImage.yourMessage_name.setText(
                                wtApplication.getCurrentChatRoom().
                                        getUsers().get(chatMessage.getUserId()).getName());
                    }
                    if(((ImageMessage)chatMessage).getOrientation().equals("h")){
                        viewHolderYourImage.yourMessage_image.getLayoutParams().width = THUMBNAIL_MAX_SIZE;
                    }
                    else{
                        viewHolderYourImage.yourMessage_image.getLayoutParams().height = THUMBNAIL_MAX_SIZE;
                    }
                    viewHolderYourImage.yourMessage_image.requestLayout();
                    viewHolderYourImage.yourMessage_time.setText(longTodate(chatMessage.getTime()));
                    SimpleTarget<Bitmap> simpleTarget = new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            viewHolderYourImage.yourMessage_image.getLayoutParams().width =
                                    (int)(resource.getWidth()*density);
                            viewHolderYourImage.yourMessage_image.getLayoutParams().height =
                                    (int)(resource.getHeight()*density);
                            viewHolderYourImage.yourMessage_image.requestLayout();
                            viewHolderYourImage.yourMessage_image.setImageBitmap(resource);
                        }
                    };
                    Glide.with(ChattingRoomActivity.this).load(((ImageMessage)chatMessage).getThumbPath())
                            .asBitmap()
                            .error(R.drawable.loadfail)
                            .into(simpleTarget);
                    viewHolderYourImage.yourMessage_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onMessageClickCallback.onImageClick(position);
                        }
                    });
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.enter){
                ViewHolderEnter viewHolderEnter = (ViewHolderEnter)holder;
                viewHolderEnter.message_enter.setGravity(Gravity.CENTER);
                viewHolderEnter.message_enter.setText(chatMessage.getMessage()+
                "님이 채팅방에 들어왔습니다");
            }
            else if(chatMessage.getType() == ChatMessage.Type.out){
                ViewHolderEnter viewHolderEnter = (ViewHolderEnter)holder;
                viewHolderEnter.message_enter.setGravity(Gravity.CENTER);
                viewHolderEnter.message_enter.setText(chatMessage.getMessage()+
                        "님이 채팅방에서 나갔습니다");
            }
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage chatMessage = chatMessages.get(position);
            if(chatMessage.getType() == ChatMessage.Type.text){
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    return 0; // 0이면 내 메세지
                }
                else{
                    return 1; // 1이면 상대 메세지
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.file){
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    return 2; // 내 파일 메세지
                }
                else{
                    return 3; // 상대 파일 메세지
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.image) {
                if(chatMessage.getUserId().equals(wtApplication.getMyself().getId())){
                    if(((ImageMessage)chatMessage).getOrientation().equals("h")){
                        return 4; // 내 이미지 가로 메세지
                    }
                    else{
                        return 9; // 내 이미지 세로 메세지
                    }
                }
                else{
                    if(((ImageMessage)chatMessage).getOrientation().equals("h")) {
                        return 5; // 상대 이미지 가로 메세지
                    }
                    else{
                        return 10; // 상대 이미지 세로 메세지
                    }
                }
            }
            else if(chatMessage.getType() == ChatMessage.Type.enter){
                return 6; // 들어오는 메세지
            }
            else if(chatMessage.getType() == ChatMessage.Type.out){
                return 7; // 나가는 메세지
            }
            return 8; // 에러
        }

        class ViewHolderMe extends RecyclerView.ViewHolder {

            TextView message_text;
            TextView message_time;
            ProgressBar file_download_progress_bar;
            LinearLayout file_download_layout;
            TextView file_download_cancel_btn;

            ViewHolderMe(View itemView) {
                super(itemView);
                message_text = (TextView)itemView.findViewById(R.id.message_text);
                message_time = (TextView)itemView.findViewById(R.id.message_time);
                file_download_progress_bar = (ProgressBar)itemView.
                        findViewById(R.id.file_progress_bar);
                file_download_layout = (LinearLayout)itemView.findViewById(R.id.file_download_layout);
                file_download_cancel_btn = (TextView)itemView.findViewById(R.id.file_download_cancel);
            }
        }

        class ViewHolderYou extends RecyclerView.ViewHolder {

            TextView yourMessage_name;
            TextView yourMessage_text;
            TextView yourMessage_time;
            ProgressBar your_file_download_progress_bar;
            LinearLayout your_file_download_layout;
            TextView your_file_download_cancel_btn;

            ViewHolderYou(View itemView) {
                super(itemView);
                yourMessage_name = (TextView)itemView.findViewById(R.id.yourMessage_name);
                yourMessage_text = (TextView)itemView.findViewById(R.id.yourMessage_text);
                yourMessage_time = (TextView)itemView.findViewById(R.id.yourMessage_time);
                your_file_download_progress_bar = (ProgressBar)itemView.
                        findViewById(R.id.your_file_progress_bar);
                your_file_download_layout = (LinearLayout)itemView.
                        findViewById(R.id.your_file_download_layout);
                your_file_download_cancel_btn = (TextView)itemView.
                        findViewById(R.id.your_file_download_cancel);
            }
        }

        class ViewHolderEnter extends RecyclerView.ViewHolder {

            TextView message_enter;

            ViewHolderEnter(View itemView) {
                super(itemView);
                message_enter = (TextView)itemView.findViewById(R.id.message_enter);
            }
        }

        class ViewHolderMyImage extends RecyclerView.ViewHolder {

            TextView message_time;
            ImageView message_image;

            ViewHolderMyImage(View itemView) {
                super(itemView);
                message_time = (TextView)itemView.findViewById(R.id.message_time);
                message_image = (ImageView)itemView.findViewById(R.id.message_image);
            }
        }

        class ViewHolderYourImage extends RecyclerView.ViewHolder{

            TextView yourMessage_time;
            TextView yourMessage_name;
            ImageView yourMessage_image;

            ViewHolderYourImage(View itemView) {
                super(itemView);
                yourMessage_name = (TextView)itemView.findViewById(R.id.yourMessage_name);
                yourMessage_time = (TextView)itemView.findViewById(R.id.yourMessage_time);
                yourMessage_image = (ImageView)itemView.findViewById(R.id.yourMessage_image);
            }
        }
    }

    public interface OnMessageClickCallback {
        void onFileClick(int position, RecyclerView.ViewHolder viewHolder);
        void onCancelClick(int position, RecyclerView.ViewHolder viewHolder);
        void onImageClick(int position);
    }

    private Uri getFileUri(){
        return Uri.fromFile(this.getFile());
    }

    private File getFile(){
        File dir = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                +"/WorkTogehter/image/"+wtApplication.getMyself().getId()+"/");
        if(!dir.exists()){
            if(!dir.mkdirs()){
                return null;
            }
        }
        File f = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                +"/WorkTogehter/image/"+wtApplication.getMyself().getId()+
                "/wt_"+System.currentTimeMillis()+"_"
                + wtApplication.getMyself().getId()+".jpg");
        try{
            if(!f.createNewFile()){
                return null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return f;
    }

    private String getExif(String path){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        if(orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270){
            return "v";
        }
        else{
            return "h";
        }
    }

    private String longTodate(Long time){
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time);
    }
}
