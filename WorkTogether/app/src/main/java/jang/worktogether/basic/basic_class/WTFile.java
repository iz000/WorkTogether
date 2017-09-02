package jang.worktogether.basic.basic_class;

import android.os.Environment;

import java.io.File;

public class WTFile {

    private String fileID;
    private String groupID;
    private String chatRoomID;
    private String userID;
    private String fileName;
    private String path;
    private long fileLength;

    public WTFile(String fileID, String groupID, String chatRoomID, String userID, String fileName,
                  long fileLength){
        this.fileID = fileID;
        this.groupID = groupID;
        this.chatRoomID = chatRoomID;
        this.userID = userID;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.path = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS) + File.separator
                + groupID + File.separator + chatRoomID + File.separator + "wtfiles"
                + File.separator + fileName;
    }

    public String getGroupID() {
        return groupID;
    }

    public String getUserID() {
        return userID;
    }

    public String getChatRoomID() {
        return chatRoomID;
    }

    public String getFileID() {
        return fileID;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public String getPath() {
        return path;
    }
}

