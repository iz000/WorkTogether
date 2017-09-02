package jang.worktogether.chatting;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import jang.worktogether.Utils.FilePacketUtil;
import jang.worktogether.Utils.PacketUtil;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;

public class FileSocketClient extends SocketClient{

    private final static int FILESERVERPORT = 9999;
    private final static int READ_BUFFER_SIZE = 1024*16;
    private final static int NETWORK_BUFFER_SIZE = 1024*4;

    private boolean isFileDownloading = false;
    private boolean isFileUploading = false;

    private boolean isLogout = false;

    FileSocketClient(Context context, String clientID) {
        super(context, clientID);
        this.port = FILESERVERPORT;
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
                    Log.i("chat", e.toString());
                    Log.i("Chat", port + "서버 통신 안됨");
                    this.interrupt();
                    return;
                }
                Log.i("Chat", port + " 파일 서버 연결 성공");
                FilePacket firstPacket = new FilePacket(SocketService.SOCKET_CONNECTED);
                firstPacket.addData(clientID, Long.toString(0));
                send(firstPacket.toByteArray());
                broadCaster.sendBroadcast(new Intent(SocketService.FILE_SOCKET_CONNECT_SUCCESS));
                receive();
            }
        };
        thread.start();
    }

    public boolean isFileDownloading() {
        return isFileDownloading;
    }

    public void setFileIsDownloading(){
        this.isFileDownloading = true;
    }

    public boolean isFileUploading() {
        return isFileUploading;
    }

    public void setFileIsUploading(){
        this.isFileUploading = true;
    }

    @Override
    public void receive() {
        while(true) {
            try {
                ByteBuffer headerByteBuffer = ByteBuffer.allocate(11);
                int byteCount = socketChannel.read(headerByteBuffer);
                if (byteCount == -1) {
                    continue;
                }
                byte[] data = headerByteBuffer.array();
                String protocol = new String(Arrays.copyOfRange(data, 0, 3), "utf-8");
                int dataLength = PacketUtil.byteArrayToInt(Arrays.copyOfRange(data, 3, 7));
                int fileLength = PacketUtil.byteArrayToInt(Arrays.copyOfRange(data, 7, 11));
                Log.i("chat", "파일길이:"+fileLength);
                ByteBuffer bodyByteBuffer = ByteBuffer.allocate(dataLength);
                while (bodyByteBuffer.hasRemaining()) {
                    socketChannel.read(bodyByteBuffer);
                }
                FilePacketUtil filePacketUtil =
                        new FilePacketUtil(protocol, bodyByteBuffer.array(), fileLength, context);
                filePacketUtil.setFileSocketClient(this);
                String path = filePacketUtil.getPath();
                Handler handler = new Handler(Looper.getMainLooper());
                if(path == null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "파일 다운로드 중 오류가 발생했습니다",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if(fileLength != 0){
                    File file = new File(path);
                    FileOutputStream fos = new FileOutputStream(file, true);
                    int read = 0;
                    while(fileLength != 0) {
                        if (fileLength > NETWORK_BUFFER_SIZE) {
                            ByteBuffer fileByteBuffer = ByteBuffer.allocate(NETWORK_BUFFER_SIZE);
                            read = socketChannel.read(fileByteBuffer);
                            if(read == -1){
                                break;
                            }
                            fos.write(Arrays.copyOfRange(fileByteBuffer.array(), 0, read));
                            fileByteBuffer.clear();
                            fileLength -= read;
                            Intent intent = new Intent(ChattingRoomActivity.FILE_DOWNLOADING);
                            intent.putExtra("read", read);
                            broadCaster.sendBroadcast(intent);
                        }
                        else if (0 < fileLength && fileLength <= NETWORK_BUFFER_SIZE){
                            ByteBuffer fileByteBuffer = ByteBuffer.allocate(fileLength);
                            while(fileByteBuffer.hasRemaining()){
                                read = socketChannel.read(fileByteBuffer);
                                if(read == -1){
                                    break;
                                }
                            }
                            if(read == -1){
                                break;
                            }
                            Intent intent = new Intent(ChattingRoomActivity.FILE_DOWNLOADING);
                            intent.putExtra("read", read);
                            broadCaster.sendBroadcast(intent);
                            fos.write(fileByteBuffer.array());
                            fileByteBuffer.clear();
                            fileLength = 0;
                        }
                    }
                    Intent intent = new Intent(ChattingRoomActivity.FILE_DOWNLOAD_FINISHED);
                    intent.putExtra("Path", path);
                    broadCaster.sendBroadcast(intent);
                    fos.close();
                    final String fileName = file.getName();
                    if(protocol.equals("203")) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, fileName + " 다운로드를 완료했습니다",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                stopClient();
                break;
            }
            finally {
                isFileDownloading = false;
            }
        }
    }

    public void setLogout(){
        isLogout = true;
    }

    @Override
    public void send(byte[] data) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        new Thread() {
            @Override
            public void run() {
                try {
                    if(!socketChannel.isOpen()){
                        Log.i("chat", "closed");
                        throw new Exception("socket is closed");
                    }
                    while(byteBuffer.hasRemaining()){
                        socketChannel.write(byteBuffer);
                    }
                    if(isLogout){
                        //이부분이 있어야 다시 켰을 때 login activity가 먼저 나오게 됨
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    stopClient();
                    this.interrupt();
                    isFileUploading = false;
                }
            }

        }.start();
    }

    public void fileSend(byte[] packet, String path){
        send(packet);
        final File file = new File(path);
        new Thread() {
            @Override
            public void run() {
                try {
                    if(!socketChannel.isOpen()){
                        Log.i("chat", "닫힘");
                        throw new Exception("socket is closed");
                    }
                    FileInputStream fis = new FileInputStream(file);
                    int bytesAvailable = fis.available();
                    int bufferSize = Math.min(bytesAvailable, READ_BUFFER_SIZE);
                    byte[] buffer = new byte[bufferSize];
                    int read;
                    Intent startIntent = new Intent(ChattingRoomActivity.FILE_UPLOAD_START);
                    startIntent.putExtra("upload", file.length());
                    broadCaster.sendBroadcast(startIntent);
                    while((read = fis.read(buffer, 0, bufferSize)) != -1) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, read);
                        int write = socketChannel.write(byteBuffer);
                        Intent intent = new Intent(ChattingRoomActivity.FILE_UPLOADING);
                        intent.putExtra("upload", write);
                        broadCaster.sendBroadcast(intent);
                    }
                    broadCaster.sendBroadcast(new Intent(ChattingRoomActivity.FILE_UPLOAD_FINISHED));
                    fis.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    stopClient();
                    this.interrupt();
                }
                finally {
                    isFileUploading = false;
                }
            }
        }.start();
    }

}
