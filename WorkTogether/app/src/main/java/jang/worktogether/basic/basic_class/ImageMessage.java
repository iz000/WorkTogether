package jang.worktogether.basic.basic_class;

public class ImageMessage extends ChatMessage{

    private String imageID;
    private String thumbPath;
    private String imageName;
    private String orientation;

    private static final String thumbnailPath = "http://45.32.109.86/image/thumbs/";

    public ImageMessage(String userId, String imageID,
                        String message, Long time, String thumbMidPath, Type type, String orientation) {
        super(userId, message, time, type);
        this.imageID = imageID;
        this.imageName = "wt_"+Long.toString(time)+userId+".jpg";
        this.thumbPath = thumbnailPath+thumbMidPath+"/"+this.imageName;
        this.orientation = orientation;
    }

    public String getOrientation() {
        return orientation;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageID() {
        return imageID;
    }

    public String getThumbPath() {
        return thumbPath;
    }
}
