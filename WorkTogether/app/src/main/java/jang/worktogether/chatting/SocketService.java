package jang.worktogether.chatting;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import jang.worktogether.Utils.HttpUtil;

public class SocketService extends Service {

    SocketClient socketClient;
    FileSocketClient fileSocketClient;

    HttpUtil httpUtil;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    BroadcastReceiver broadcastReceiver;
    LocalBroadcastManager broadCaster;

    public final static String SOCKET_CONNECT_SUCCESS = "SOCKET_CONNECT_SUCCESS";
    public final static String FILE_SOCKET_CONNECT_SUCCESS = "FILE_SOCKET_CONNECT_SUCCESS";
    //보내야하는 패킷 메세지 종류
    public final static String CHATTING_PACKET = "100";
    public final static String FILE_UPLOAD = "101";
    public final static String IMAGE_UPLOAD = "102";
    public final static String REQUEST_FILE_DOWNLOAD = "103";
    public final static String REQUEST_IMAGE_DOWNLOAD = "104";
    public final static String CANCEL_FILE_DOWNLOAD = "105";
    public final static String PROFILE_UPLOAD = "106";
    public final static String PROFILE_DOWNLOAD = "107";
    public final static String SOCKET_CONNECTED = "111";
    public final static String NEW_GROUP_MAKE = "121";
    public final static String REQUEST_FRIEND_BY_EMAIL = "122";
    public final static String CHATTING_ENTER = "131";
    public final static String CHATTING_OUT = "132";
    public final static String REQUEST_FRIEND = "133";
    public final static String REQUEST_RELATION = "134";
    public final static String NEW_CHATTING = "141";
    public final static String GROUP_REQUEST_ACCEPT = "151";
    public final static String GROUP_REQUEST_CANCEL = "152";
    public final static String FRIEND_REQUEST_ACCEPT = "153";
    public final static String FRIEND_REQUEST_CANCEL = "154";
    public final static String LOGOUT = "400";

    byte[] tempPacket; // 패킷 전달하기 전에 fileSocket이 닫혀있을 때
    String tempPath; // 패킷 전달하기 전에 fileSocket이 닫혀있을 때

    Queue<byte[]> packetQueue;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadCaster = LocalBroadcastManager.getInstance(this);
        pref = getSharedPreferences("Login", MODE_PRIVATE);
        editor = pref.edit();
        packetQueue = new LinkedList<>();

        httpUtil = new HttpUtil(SocketService.this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    if(response.equals("105")){ // 세션이 만료되었으면 저장된 쿠키를 삭제
                        editor.putString("Session-Cookie", null);
                        editor.apply();
                    }
                }
                else {
                    try {
                        JSONObject responseToJson = new JSONObject(response);
                        String clientID = responseToJson.getString("u_id");
                        socketClient = new SocketClient(SocketService.this, clientID);
                        fileSocketClient = new FileSocketClient(SocketService.this, clientID);
                        socketClient.startClient();
                        fileSocketClient.startClient();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        boolean isLogin = pref.getBoolean("isLogin", false);
        if(isLogin){
            httpUtil.setUrl("login_check.php")
                    .setUseSession(true)
                    .postData();
        }
        else{
            stopSelf();
        }
        registerBroadCastReceiver();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        if(socketClient != null){
           socketClient.stopClient();
        }
        if(fileSocketClient != null){
            fileSocketClient.stopClient();
        }
    }

    private void flushQueue(){ // 패킷을 보내서 큐를 비워줌
        while(!packetQueue.isEmpty()){
            socketClient.send(packetQueue.poll());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerBroadCastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(SOCKET_CONNECT_SUCCESS)){
                    flushQueue();
                }
                else if(intent.getAction().equals(FILE_SOCKET_CONNECT_SUCCESS)){
                    if(tempPath != null && tempPacket != null){
                        fileSocketClient.setFileIsUploading();
                        fileSocketClient.fileSend(tempPacket, tempPath);
                        tempPacket = null;
                        tempPath = null;
                    }
                    else if(tempPath == null && tempPacket != null){
                        fileSocketClient.setFileIsDownloading();
                        fileSocketClient.send(tempPacket);
                        tempPacket = null;
                    }
                }
                else if(intent.getAction().equals(FILE_UPLOAD)
                        || intent.getAction().equals(IMAGE_UPLOAD)
                        || intent.getAction().equals(PROFILE_UPLOAD)){
                    //파일 업로드시에는 여기서 파일 바이트로 바꿔서 보내줄 것. 데이터따로 받고 파일 경로 따로 받을 것
                    if(fileSocketClient.isFileUploading()){
                        Toast.makeText(SocketService.this, "다른 파일을 업로드 하는 중입니다",
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        if(fileSocketClient.isSocketOpen()){
                            fileSocketClient.setFileIsUploading();
                            fileSocketClient.fileSend(intent.getByteArrayExtra("Packet"),
                                    intent.getStringExtra("Path"));
                        }
                        else{
                            tempPacket = intent.getByteArrayExtra("Packet");
                            tempPath = intent.getStringExtra("Path");
                            fileSocketClient.startClient();
                        }
                    }
                }
                else if(intent.getAction().equals(REQUEST_FILE_DOWNLOAD)
                        || intent.getAction().equals(REQUEST_IMAGE_DOWNLOAD)
                        || intent.getAction().equals(PROFILE_DOWNLOAD)){
                    if(fileSocketClient.isFileUploading()){
                        Toast.makeText(SocketService.this, "파일 업로드 완료 후에 다운로드가 가능합니다",
                                Toast.LENGTH_SHORT).show();
                    }
                    else if(fileSocketClient.isFileDownloading()){
                        Toast.makeText(SocketService.this, "다른 파일을 다운로드 하는 중입니다",
                                Toast.LENGTH_SHORT).show();
                    }
                    else{
                        if(fileSocketClient.isSocketOpen()){
                            fileSocketClient.setFileIsDownloading();
                            fileSocketClient.send((intent.getByteArrayExtra("Packet")));
                        }
                        else{
                            tempPacket = intent.getByteArrayExtra("Packet");
                            fileSocketClient.startClient();
                        }
                    }
                }
                else if(intent.getAction().equals(CANCEL_FILE_DOWNLOAD)){
                    Log.i("chat", "중단요청");
                    if(fileSocketClient.isFileDownloading()){
                        if(fileSocketClient.isSocketOpen()){
                            fileSocketClient.send(intent.getByteArrayExtra("Packet"));
                            fileSocketClient.socketClose();
                        }
                        else{
                            tempPacket = intent.getByteArrayExtra("Packet");
                            fileSocketClient.startClient();
                        }
                    }
                }
                else if(intent.getAction().equals(LOGOUT)){
                    //소켓이 열려있고 큐가 비었으면 바로 보내고 둘다 아니면서 소켓이 닫혀있으면
                    //소켓을 열고 큐에 추가하고 열려있으면 큐에 추가하고 큐를 비워줌.
                    if(socketClient.isSocketOpen() && packetQueue.isEmpty()){
                        socketClient.send((intent.getByteArrayExtra("Packet")));
                    }
                    else{
                        packetQueue.offer(intent.getByteArrayExtra("Packet"));
                        if(!socketClient.isSocketOpen()){
                            socketClient.startClient();
                        }
                        else{
                            flushQueue();
                        }
                    }
                    if(fileSocketClient.isSocketOpen()){
                        FilePacket filePacket = new FilePacket(LOGOUT);
                        fileSocketClient.setLogout(); // 로그아웃시에 이 flag를 세팅해줘야 kill process가 됨.
                        fileSocketClient.send(filePacket.toByteArray());
                    }
                    stopSelf();
                }
                else{
                    if(socketClient.isSocketOpen() && packetQueue.isEmpty()){
                        socketClient.send((intent.getByteArrayExtra("Packet")));
                    }
                    else{
                        packetQueue.offer(intent.getByteArrayExtra("Packet"));
                        if(!socketClient.isSocketOpen()){
                            socketClient.startClient();
                        }
                        else{
                            flushQueue();
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SOCKET_CONNECT_SUCCESS);
        intentFilter.addAction(FILE_SOCKET_CONNECT_SUCCESS);
        intentFilter.addAction(CHATTING_PACKET);
        intentFilter.addAction(FILE_UPLOAD);
        intentFilter.addAction(IMAGE_UPLOAD);
        intentFilter.addAction(REQUEST_FILE_DOWNLOAD);
        intentFilter.addAction(REQUEST_IMAGE_DOWNLOAD);
        intentFilter.addAction(CANCEL_FILE_DOWNLOAD);
        intentFilter.addAction(PROFILE_UPLOAD);
        intentFilter.addAction(PROFILE_DOWNLOAD);
        intentFilter.addAction(CHATTING_ENTER);
        intentFilter.addAction(CHATTING_OUT);
        intentFilter.addAction(REQUEST_FRIEND);
        intentFilter.addAction(SOCKET_CONNECTED);
        intentFilter.addAction(NEW_GROUP_MAKE);
        intentFilter.addAction(REQUEST_FRIEND_BY_EMAIL);
        intentFilter.addAction(REQUEST_RELATION);
        intentFilter.addAction(NEW_CHATTING);
        intentFilter.addAction(GROUP_REQUEST_ACCEPT);
        intentFilter.addAction(GROUP_REQUEST_CANCEL);
        intentFilter.addAction(FRIEND_REQUEST_ACCEPT);
        intentFilter.addAction(FRIEND_REQUEST_CANCEL);
        intentFilter.addAction(LOGOUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

}
