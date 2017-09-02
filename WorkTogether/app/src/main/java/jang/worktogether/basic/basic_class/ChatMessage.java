package jang.worktogether.basic.basic_class;

public class ChatMessage {

    private String userId;
    private String message;
    private Long time;
    public enum Type{text, file, image, enter, out}
    private Type type;

    public ChatMessage(String userId, String message, Long time, Type type){
        this.userId = userId;
        this.message = message;
        this.time = time;
        this.type = type;
    }

    public ChatMessage(String message, Type type){
        this.message = message;
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public Long getTime() {
        return time;
    }

    public Type getType() {
        return type;
    }
}
