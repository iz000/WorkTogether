package jang.worktogether.basic.basic_class;

public class FileMessage extends ChatMessage {

    private String fileID;
    private long fileLength;

    public FileMessage(String userId, String fileID, long fileLength,
                       String message, Long time, Type type) {
        super(userId, message, time, type);
        this.fileID = fileID;
        this.fileLength = fileLength;
    }

    public String getFileID() {
        return fileID;
    }

    public long getFileLength() {
        return fileLength;
    }
}
