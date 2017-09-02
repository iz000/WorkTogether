package jang.worktogether.chatting;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import jang.worktogether.Utils.PacketUtil;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;

public class SocketClient {

    SocketChannel socketChannel;
    int port;
    protected Context context;

    protected LocalBroadcastManager broadCaster;
    public String clientID;
    DBManager dbManager;
    SQLiteDatabase db;
    Thread thread;

    final String serverAddress = "45.32.109.86";
    private final static int SERVERPORT = 9000;

    private Queue<byte[]> packetQueue;

    SocketClient(Context context, String clientID){
        this.context = context;
        this.port = SERVERPORT;
        broadCaster = LocalBroadcastManager.getInstance(context);
        this.clientID = clientID;
        dbManager = new DBManager(context, clientID);
        db = dbManager.getReadableDatabase();
        packetQueue = new LinkedList<>();
    }

    boolean isSocketOpen(){
        return socketChannel.isOpen();
    }

    void socketClose(){
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startClient(){
        if(socketChannel != null) {
            if (socketChannel.isOpen()) {
                return;
            }
        }
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress(serverAddress, port));
                } catch (Exception e) {
                    Log.i("Chat", port + "서버 통신 안됨");
                    this.interrupt();
                    return;
                }
                Log.i("Chat", port + " 채팅 서버 연결 성공");
                Cursor cursor = db.rawQuery("select "+ FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME
                        +" from "+ FeedReaderContract.FeedEntry.TABLE_NAME +" order by "
                        + FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME + " desc limit 1", null);
                Long lastChatTime;
                if(cursor.moveToNext()){
                     lastChatTime = cursor.getLong(cursor.getColumnIndex(
                            FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME));
                }
                else{
                    lastChatTime = 0L;
                }
                Packet firstPacket = new Packet(SocketService.SOCKET_CONNECTED);
                firstPacket.addData(clientID, Long.toString(lastChatTime));
                send(firstPacket.toByteArray());
                broadCaster.sendBroadcast(new Intent(SocketService.SOCKET_CONNECT_SUCCESS));
                receive();
            }
        };
        thread.start();
    }

    void stopClient(){
        if(thread != null) {
            thread.interrupt();
            thread = null;
        }
        try{
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
                db.close();
                dbManager.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void receive(){
        while (true) {
            try {
                if(socketChannel.isOpen()){
                    ByteBuffer headerByteBuffer = ByteBuffer.allocate(7);
                    int byteCount = socketChannel.read(headerByteBuffer);
                    if (byteCount == -1) {
                        throw new IOException();
                    }
                    byte[] data = headerByteBuffer.array();
                    String protocol = new String(Arrays.copyOfRange(data, 0, 3),"utf-8");
                    int length = PacketUtil.byteArrayToInt(Arrays.copyOfRange(data, 3, 7));
                    ByteBuffer bodyByteBuffer = ByteBuffer.allocate(length);
                    while(bodyByteBuffer.hasRemaining()){
                        socketChannel.read(bodyByteBuffer);
                    }
                    PacketUtil packetUtil = new PacketUtil(protocol, bodyByteBuffer.array(), context);
                    packetUtil.setSocketClient(SocketClient.this);
                    packetUtil.execute();
                }
            }
            catch (Exception e) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "서버와의 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
                stopClient();
                break;
            }
        }
    }

    public void send(final byte[] data) {
        if(!this.isSocketOpen()){
            return;
        }
        final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        new Thread() {
            @Override
            public void run() {
                try {
                    if(!socketChannel.isOpen()){
                        throw new Exception("socket is closed");
                    }
                    socketChannel.write(byteBuffer);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    stopClient();
                    this.interrupt();
                }
            }

        }.start();
    }
}
