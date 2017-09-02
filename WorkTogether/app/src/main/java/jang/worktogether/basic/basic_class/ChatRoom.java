package jang.worktogether.basic.basic_class;

import java.util.HashMap;

public class ChatRoom {

    private String chatRoomID;
    private String chatRoomTopic;
    private int memberNum = 0;
    private HashMap<String, User> users;

    private String lastChat = null;
    private int chatCount = 0;

    public ChatRoom(String chatRoomID, String chatRoomTopic){
        this.chatRoomID = chatRoomID;
        this.chatRoomTopic = chatRoomTopic;
        users = new HashMap<>();
    }

    public synchronized void setLastChat(String lastChat) {
        this.lastChat = lastChat;
    }

    public synchronized String getLastChat() {
        return lastChat;
    }

    public synchronized void addChatCount(){
        this.chatCount++;
    }

    public synchronized void setChatCount(int chatCount) {
        this.chatCount = chatCount;
    }

    public synchronized int getChatCount() {
        return chatCount;
    }

    public String getChatRoomID() {
        return chatRoomID;
    }

    public String getChatRoomTopic() {
        return chatRoomTopic;
    }

    public void setUsers(HashMap<String, User> users) {
        this.users = users;
    }

    public HashMap<String, User> getUsers() {
        return users;
    }

    public void addUser(User user){
        users.put(user.getId(), user);
    }

    public void removeUser(String id){
        users.remove(id);
    }

    public void setMemberNum(int memberNum) {
        this.memberNum = memberNum;
    }

    public int getMemberNum(){
        if(memberNum != users.size() && users.size() == 0){
            return memberNum;
        }
        else{
            return users.size();
        }
    }
}
