package jang.worktogether.Utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import jang.worktogether.R;
import jang.worktogether.basic.LoginActivity;
import jang.worktogether.basic.SplashActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.ChatRoom;
import jang.worktogether.basic.basic_class.MyGroup;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.ChattingRoomActivity;
import jang.worktogether.chatting.EnteredChattingListFragment;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;
import jang.worktogether.chatting.SocketClient;
import jang.worktogether.chatting.SocketService;
import jang.worktogether.group.ModifyProfileActivity;

import static jang.worktogether.basic.UserProfileActivity.USER_RELATION_CHECK;
import static jang.worktogether.chatting.ChattingListActivity.NEW_ENTERED;
import static jang.worktogether.chatting.ChattingListActivity.NEW_MEMBER;
import static jang.worktogether.chatting.ChattingListActivity.NEW_NOTENTERED;
import static jang.worktogether.chatting.MakeChattingActivity.CHATTING_MAKE_COMPLETED;
import static jang.worktogether.group.MainActivity.NEW_FRIEND;
import static jang.worktogether.group.MainActivity.NEW_FRIEND_REQUEST;
import static jang.worktogether.group.MainActivity.NEW_GROUP;

public class PacketUtil  {

    private SocketClient socketClient;
    String protocol;
    protected byte[] data;
    protected Context context;
    protected WTapplication wtApplication;
    LocalBroadcastManager broadCaster;

    DBManager dbManager;
    SQLiteDatabase db;

    private NotificationManager nm;

    public PacketUtil(String protocol, byte[] data, Context context){
        this.protocol = protocol;
        this.data = data;
        this.context = context;
        wtApplication = (WTapplication)context.getApplicationContext();
        broadCaster = LocalBroadcastManager.getInstance(context);
        nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;
        dbManager = new DBManager(context, this.socketClient.clientID);
    }

    public void execute(){ // 패킷을 받았을 때 해야하는 일
        switch (protocol){
            case "200" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    db = dbManager.getWritableDatabase();
                    String group_id = bodyStr[1];
                    String chat_id = bodyStr[2];
                    ContentValues values = new ContentValues();
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID, group_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID, chat_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME, Long.parseLong(bodyStr[3]));
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID, bodyStr[0]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT, bodyStr[4]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE, "text");
                    if(wtApplication.getCurrentGroup() != null) {
                        if (wtApplication.getCurrentGroup().getId().equals(group_id)) {
                            if (wtApplication.getCurrentGroupEnteredChatRooms().containsKey(chat_id)) {
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).setLastChat(bodyStr[4]);
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).addChatCount();
                                broadCaster.sendBroadcast(
                                        new Intent(EnteredChattingListFragment.CHAT_COUNT_PLUS));
                            }
                        }
                    }
                    if(wtApplication.getCurrentChatRoom() == null){
                        //현재 채팅방이 null이면 읽지 않음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    else if(wtApplication.getCurrentChatRoom().getChatRoomID().equals(chat_id)){
                        //현재 채팅방이 null이 아니면 읽음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                        //채팅방 액티비티에 채팅 내용을 보내줌
                        Intent intent = new Intent(ChattingRoomActivity.CHAT_MESSAGE);
                        intent.putExtra(ChattingRoomActivity.CHAT_MESSAGE, bodyStr);
                        broadCaster.sendBroadcast(intent);
                    }
                    else{
                        //다른 채팅방에 온 패킷이면 읽지 않음으로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "201" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 8);
                    db = dbManager.getWritableDatabase();
                    String group_id = bodyStr[1];
                    String chat_id = bodyStr[2];
                    ContentValues values = new ContentValues();
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID, group_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID, chat_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME, Long.parseLong(bodyStr[6]));
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID, bodyStr[0]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT,
                            bodyStr[3] + "|" + bodyStr[4] + "|" + bodyStr[5] + "|" + bodyStr[7]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE, "file");
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    if(wtApplication.getCurrentGroup() != null) {
                        if (wtApplication.getCurrentGroup().getId().equals(group_id)) {
                            if (wtApplication.getCurrentGroupEnteredChatRooms().containsKey(chat_id)) {
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).setLastChat(bodyStr[7]+"."+bodyStr[5]);
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).addChatCount();
                                broadCaster.sendBroadcast(
                                        new Intent(EnteredChattingListFragment.CHAT_COUNT_PLUS));
                            }
                        }
                    }
                    if(wtApplication.getCurrentChatRoom() == null){
                        //현재 채팅방이 null이면 읽지 않음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    else if(wtApplication.getCurrentChatRoom().getChatRoomID().equals(chat_id)){
                        //현재 채팅방이 null이 아니면 읽음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                        //채팅방 액티비티에 채팅 내용을 보내줌
                        Intent intent = new Intent(ChattingRoomActivity.FILE_MESSAGE);
                        intent.putExtra(ChattingRoomActivity.FILE_MESSAGE, bodyStr);
                        broadCaster.sendBroadcast(intent);
                    }
                    else{
                        //다른 채팅방에 온 패킷이면 읽지 않음으로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "202" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 7);
                    db = dbManager.getWritableDatabase();
                    String group_id = bodyStr[1];
                    String chat_id = bodyStr[2];
                    ContentValues values = new ContentValues();
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID, bodyStr[1]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID, bodyStr[2]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME, Long.parseLong(bodyStr[5]));
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID, bodyStr[0]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT,
                            bodyStr[3] + "|" + bodyStr[4] + "|" + bodyStr[5] + "|" + bodyStr[6]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE, "image");
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    if(wtApplication.getCurrentGroup() != null) {
                        if (wtApplication.getCurrentGroup().getId().equals(group_id)) {
                            if (wtApplication.getCurrentGroupEnteredChatRooms().containsKey(chat_id)) {
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).setLastChat("사진");
                                wtApplication.getCurrentGroupEnteredChatRooms()
                                        .get(chat_id).addChatCount();
                                broadCaster.sendBroadcast(
                                        new Intent(EnteredChattingListFragment.CHAT_COUNT_PLUS));
                            }
                        }
                    }
                    if(wtApplication.getCurrentChatRoom() == null){
                        //현재 채팅방이 null이면 읽지 않음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    else if(wtApplication.getCurrentChatRoom().getChatRoomID().equals(chat_id)){
                        //현재 채팅방이 null이 아니면 읽음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                        //채팅방 액티비티에 채팅 내용을 보내줌
                        Intent intent = new Intent(ChattingRoomActivity.IMAGE_MESSAGE);
                        intent.putExtra(ChattingRoomActivity.IMAGE_MESSAGE, bodyStr);
                        broadCaster.sendBroadcast(intent);
                    }
                    else{
                        //다른 채팅방에 온 패킷이면 읽지 않음으로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 0);
                    }
                    db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "206" : { // 프로필 업로드가 완료되었을 때
                try {
                    String body = new String(data, "utf-8");
                    wtApplication.getMyself().setProfile(body);
                    broadCaster.sendBroadcast(new Intent(ModifyProfileActivity.PROFILE_UPLOADED));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "221" : { // 이메일을 통해 친구 요청을 보내고 답이 왔을 때
                try{
                    final String body = new String(data, "utf-8");
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final String[] bodyStr = body.split("\\|", 4);
                    if(wtApplication.getMyself() != null) {
                        String toastMessage;
                        if (body.equals("101")) {
                            toastMessage = ErrorcodeUtil.errorMessage(body);
                        }
                        else if(body.equals("alreadyRequested")){
                            toastMessage = "이미 친구 요청을 보냈습니다";
                        }
                        else if(body.equals("alreadyFriend")){
                            toastMessage = "이미 친구입니다";
                        }
                        else {
                            toastMessage = bodyStr[1]+"님에게 친구 요청을 보냈습니다";
                        }
                        final String finalToastMessage = toastMessage;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, finalToastMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "231" : { // 새로운 유저가 채팅방에 들어왔다는 패킷을 받았을 때
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 7);
                    db = dbManager.getWritableDatabase();
                    String group_id = bodyStr[0];
                    String chat_id = bodyStr[1];
                    Long time = Long.parseLong(bodyStr[3]);

                    ContentValues values = new ContentValues();
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID, group_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID, chat_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME, time);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID, bodyStr[2]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT, bodyStr[4]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE, "enter");
                    if(wtApplication.getCurrentChatRoom() == null){
                        //현재 채팅방이 null이어도 읽음 상태로 저장. 읽지않은 메세지 수에 포함안됨.
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                    }
                    else if(wtApplication.getCurrentChatRoom().getChatRoomID().equals(chat_id)){
                        //현재 채팅방이 null이 아니고 현재 채팅방에 온 패킷이면 읽음 상태로 저장
                        User user = new User(bodyStr[2], bodyStr[4], bodyStr[6],
                                bodyStr[5], User.Relation.NoRelation);
                        wtApplication.getCurrentChatRoom().addUser(user);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                        //채팅방 액티비티에 들어온 유저의 이름을 보내줌
                        Intent intent = new Intent(ChattingRoomActivity.USER_ENTER);
                        intent.putExtra(ChattingRoomActivity.USER_ENTER, bodyStr[4]);
                        broadCaster.sendBroadcast(intent);
                    }
                    else{
                        //다른 채팅방에 온 패킷이어도 읽음으로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                    }
                    db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "232" : { // 나와의 관계를 물어본 후 받은 패킷
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 2);
                    Intent intent = new Intent(USER_RELATION_CHECK);
                    intent.putExtra("id", bodyStr[0]);
                    intent.putExtra("relation", bodyStr[1]);
                    broadCaster.sendBroadcast(intent);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "233" : { // 채팅방에서 유저가 나갔을 때
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    db = dbManager.getWritableDatabase();
                    String group_id = bodyStr[0];
                    String chat_id = bodyStr[1];
                    Long time = Long.parseLong(bodyStr[3]);

                    ContentValues values = new ContentValues();
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID, group_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID, chat_id);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME, time);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID, bodyStr[2]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT, bodyStr[4]);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE, "out");
                    if(wtApplication.getCurrentChatRoom() == null){
                        //현재 채팅방이 null이어도 읽음 상태로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                    }
                    else if(wtApplication.getCurrentChatRoom().getChatRoomID().equals(chat_id)){
                        //현재 채팅방이 null이 아니고 현재 채팅방에 온 패킷이면 읽음 상태로 저장
                        wtApplication.getCurrentChatRoom().removeUser(bodyStr[2]);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                        //그 채팅방 액티비티로 나간 유저의 이름을 보내줌
                        Intent intent = new Intent(ChattingRoomActivity.USER_OUT);
                        intent.putExtra(ChattingRoomActivity.USER_OUT, bodyStr[4]);
                        broadCaster.sendBroadcast(intent);
                    }
                    else{
                        //다른 채팅방에 온 패킷이어도 읽음으로 저장
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ, 1);
                    }
                    db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                finally {
                    if(db != null){
                        db.close();
                    }
                }
                break;
            }

            case "241" : { // 그룹에 새로운 채팅방이 생겼을 때
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    if(wtApplication.getCurrentGroup() != null){
                        if(wtApplication.getCurrentGroup().getId().equals(bodyStr[0])){
                            ChatRoom chatRoom = new ChatRoom(bodyStr[1], bodyStr[4]);
                            chatRoom.setMemberNum(Integer.parseInt(bodyStr[2]));
                            for(String id : bodyStr[3].split(",")){
                                if(id.equals(wtApplication.getMyself().getId())){
                                    wtApplication.getCurrentGroupEnteredChatRooms().put(bodyStr[1], chatRoom);
                                    broadCaster.sendBroadcast(new Intent(NEW_ENTERED));
                                    Intent intent = new Intent(CHATTING_MAKE_COMPLETED);
                                    intent.putExtra("c_id", bodyStr[1]);
                                    broadCaster.sendBroadcast(intent);
                                    return;
                                }
                            }
                            wtApplication.getCurrentGroupNotEnteredChatRooms().put(bodyStr[1], chatRoom);
                            broadCaster.sendBroadcast(new Intent(NEW_NOTENTERED));
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "251" : { // 친구 요청이 들어왔을 때
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 4);
                    if(wtApplication.getFriendRequest() != null){
                        User user = new User(bodyStr[0], bodyStr[1], bodyStr[2], bodyStr[3], User.Relation.Requester);
                        wtApplication.addFriendRequest(user);
                        broadCaster.sendBroadcast(new Intent(NEW_FRIEND_REQUEST));
                    }
                    startNotification("251", bodyStr[1]);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "253" : { // 그룹에 참가했을 때
                try {
                    final String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    if(wtApplication.getMyself() != null) {
                        MyGroup myGroup = new MyGroup(bodyStr[0], bodyStr[1], bodyStr[2], bodyStr[4]);
                        myGroup.setMemberCount(Integer.parseInt(bodyStr[3]));
                        wtApplication.getMyself().addGroup(myGroup);
                        broadCaster.sendBroadcast(new Intent(NEW_GROUP));
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, bodyStr[1] + " 그룹에 참가가 완료되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "254" : { // 친구 수락을 했을 때
                try {
                    final String[] bodyStr = (new String(data, "utf-8")).split("\\|", 4);
                    if(wtApplication.getMyself() != null){
                        User user = new User(bodyStr[0], bodyStr[1], bodyStr[2], bodyStr[3], User.Relation.Friend);
                        wtApplication.getMyself().addFriend(user);
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, bodyStr[1]+"님이 친구로 추가되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        });
                        broadCaster.sendBroadcast(new Intent(NEW_FRIEND));
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "255" : { // 그룹의 기존 유저들이 새로운 그룹원이 추가되었다는 것을 알림받을 때
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    if(wtApplication.getMyself() != null){
                        if(wtApplication.getMyself().getId().equals(bodyStr[1])){
                            return;
                        }
                        User user = new User(bodyStr[1], bodyStr[2], bodyStr[3], bodyStr[4], User.Relation.NoRelation);
                        if(wtApplication.getCurrentGroup().getId().equals(bodyStr[0])){
                            wtApplication.getCurrentGroup().addUser(user);
                        }
                        wtApplication.getMyself().getGroups().get(bodyStr[0]).addUser(user);
                        broadCaster.sendBroadcast(new Intent(NEW_MEMBER));
                    }
                    startNotification("255", wtApplication.getMyself().getGroups().get(bodyStr[0])
                            .getName(), bodyStr[2]);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "256" : { // 친구 요청을 한 상대가 친구 요청을 수락했을 때 받는 패킷
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 4);
                    if(wtApplication.getMyself() != null){
                        User user = new User(bodyStr[0], bodyStr[1], bodyStr[2], bodyStr[3], User.Relation.Friend);
                        wtApplication.getMyself().addFriend(user);
                        broadCaster.sendBroadcast(new Intent(NEW_FRIEND));
                    }
                    startNotification("256", bodyStr[1]);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "300" : {
                try {
                    final String bodyStr = new String(data, "utf-8");
                    if(bodyStr.equals("friendRequestError")){
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "이미 친구이거나 친구 요청을 보낸 상태입니다",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else if(bodyStr.equals("111")){
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, ErrorcodeUtil.errorMessage(bodyStr),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        LogoutUtil.logout(context);
                        context.stopService(new Intent(context, SocketService.class));
                        broadCaster.sendBroadcast(new Intent(LoginActivity.RE_LOGIN_NEEDED));
                    }
                    Log.i("chat", bodyStr + " 패킷에서 에러가 발생했습니다");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private void startNotification(String protocol, String... contents){
        NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(context);
        notiBuilder.setSmallIcon(R.drawable.icon)
                .setWhen(System.currentTimeMillis());
        switch (protocol){
            case "251" : { // 노티 누르면 main activity 세번째 탭으로
                Intent intent = new Intent(context, SplashActivity.class);
                intent.putExtra("noti", 251);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                notiBuilder.setTicker(contents[0] + "님이 친구 요청을 보냈습니다")
                        .setContentTitle(contents[0] + "님의 친구 요청")
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                nm.notify(251, notiBuilder.build());
                break;
            }
            case "255" : { // 노티 누르면 main activity 첫번째 탭으로
                Intent intent = new Intent(context, SplashActivity.class);
                intent.putExtra("noti", 255);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                notiBuilder.setTicker(contents[0] + " 그룹에 " + contents[1] + "님이 참여하셨습니다")
                        .setContentTitle(contents[1] + "님의 "+contents[0]+" 그룹 참여")
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                nm.notify(255, notiBuilder.build());
                break;
            }
            case "256" : { // 노티 누르면 main activity 두번째 탭으로
                Intent intent = new Intent(context, SplashActivity.class);
                intent.putExtra("noti", 256);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                notiBuilder.setTicker(contents[0] + "님이 친구 요청을 수락했습니다")
                        .setContentTitle(contents[0] + "님의 친구 요청 수락")
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                nm.notify(256, notiBuilder.build());
                break;
            }
        }
    }

    public static int byteArrayToInt(byte[] byteArray) {
        return (byteArray[0] & 0xff) << 24 | (byteArray[1] & 0xff) << 16 | (byteArray[2] & 0xff) << 8
                | (byteArray[3] & 0xff);
    }

    public static byte[] intToByteArray(int a) {
        return ByteBuffer.allocate(4).putInt(a).array();
    }
}
