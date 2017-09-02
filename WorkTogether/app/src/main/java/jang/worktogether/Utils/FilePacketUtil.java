package jang.worktogether.Utils;


import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.IOException;

import jang.worktogether.chatting.FileSocketClient;
import jang.worktogether.chatting.SQLIte.DBManager;

public class FilePacketUtil extends PacketUtil {

    private FileSocketClient fileSocketClient;
    private int fileLength;

    public FilePacketUtil(String protocol, byte[] data, int fileLength, Context context) {
        super(protocol, data, context);
        this.fileLength = fileLength;
    }

    public void setFileSocketClient(FileSocketClient fileSocketClient) {
        this.fileSocketClient = fileSocketClient;
        dbManager = new DBManager(context, this.fileSocketClient.clientID);
    }

    public String getPath(){ // 패킷을 받았을 때 해야하는 일
        switch (protocol){
            case "203" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    String directoryPath = Environment.getExternalStoragePublicDirectory
                            (Environment.DIRECTORY_DOWNLOADS)
                            + File.separator + bodyStr[0] + File.separator + bodyStr[1] +
                            File.separator + "wtfiles";
                    File directory = new File(directoryPath);
                    if(!directory.exists()){
                        if(!directory.mkdirs()){
                            Log.i("chat", "디렉토리 생성 실패");
                            return null;
                        }
                        Log.i("chat", "디렉토리생성");
                    }
                    String filePath = directoryPath + File.separator + bodyStr[4] + "." + bodyStr[3];
                    File file = new File(filePath);
                    if(file.exists()){
                        if(Long.parseLong(bodyStr[2]) != fileLength){ // 이어받기
                            return filePath;
                        }
                        else{ // 이어받기 아니고 중복 다운로드
                            if(file.delete()){ // 파일을 지우고 다시 다운로드 함
                                return filePath;
                            }
                            else{
                                return null; // 파일을 지우지 못하면 error발생
                            }
                        }
                    }
                    else{ // 이어 받기 아닐때는 그냥 저장
                        return filePath;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "204" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 5);
                    String directoryPath = context.getCacheDir()
                            + File.separator + bodyStr[0] + File.separator + bodyStr[1] +
                            File.separator + "wtimages";
                    File directory = new File(directoryPath);
                    if(!directory.isDirectory()){
                        if(!directory.mkdirs()){
                            Log.i("chat", "디렉토리 생성 실패");
                            return null;
                        }
                    }
                    String imagePath = directoryPath + File.separator + bodyStr[4] + "." + bodyStr[3];
                    File file = new File(imagePath);
                    if(file.exists()){
                        int i = 1;
                        while(true){
                            file = new File(directoryPath + File.separator + bodyStr[4]
                                    + "(" + i + ")." + bodyStr[3]);
                            if(!file.exists()){
                                break;
                            }
                            i++;
                        }
                        return imagePath;
                    }
                    else{
                        return imagePath;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "207" : {
                try {
                    String[] bodyStr = (new String(data, "utf-8")).split("\\|", 2);
                    String directoryPath = context.getCacheDir()
                            + File.separator + "profile" + File.separator + bodyStr[1];
                    File directory = new File(directoryPath);
                    if(!directory.isDirectory()){
                        if(!directory.mkdirs()){
                            Log.i("chat", "디렉토리 생성 실패");
                            return null;
                        }
                    }
                    String profilePath = directoryPath + File.separator + bodyStr[0];
                    File file = new File(profilePath);
                    if(file.exists()){
                        int i = 1;
                        while(true){
                            file = new File(directoryPath + File.separator + bodyStr[0]
                                    + "(" + i + ")." + bodyStr[3]);
                            if(!file.exists()){
                                break;
                            }
                            i++;
                        }
                        return profilePath;
                    }
                    else{
                        return profilePath;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

}
