package jang.worktogether.basic.basic_class;

import android.os.Environment;

import java.io.File;

public class WTImage {

    private String imageID;
    private String groupID;
    private String chatRoomID;
    private String userID;
    private String path;
    private String imageName;

    public WTImage(String imageID, String groupID, String chatRoomID, String userID, String imageName,
                   String cacheDirectory){
        this.imageID = imageID;
        this.groupID = groupID;
        this.chatRoomID = chatRoomID;
        this.userID = userID;
        this.imageName = imageName;
        this.path = cacheDirectory + File.separator
                + groupID + File.separator + chatRoomID + File.separator + "wtimages"
                + File.separator + imageName;
    }

    public String getThumbnailPath(){
        return "http://45.32.109.86/image/thumbs/"+groupID+"/"+chatRoomID+"/"+userID+"/"+imageName;
    }

    public String getImageID() {
        return imageID;
    }

    public String getPath() {
        return path;
    }

    public String getImageName() {
        return imageName;
    }
}
