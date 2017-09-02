package jang.worktogether.basic;

import android.app.Application;

import java.util.ArrayList;
import java.util.HashMap;

import jang.worktogether.Utils.FontUtil;
import jang.worktogether.basic.basic_class.ChatRoom;
import jang.worktogether.basic.basic_class.Group;
import jang.worktogether.basic.basic_class.MyGroup;
import jang.worktogether.basic.basic_class.Myself;
import jang.worktogether.basic.basic_class.User;

public class WTapplication extends Application {

    Myself myself; // 사용자를 나타내는 객체
    ArrayList<User> friendRequest; // 나에게 들어온 친구 요청
    ArrayList<Group> groupRequest; // 나에게 들어온 그룹 초대 요청
    ChatRoom currentChatRoom = null; // 현재 보고있는 채팅방.
    MyGroup currentGroup = null; // 현재 선택한 그룹
    HashMap<String, ChatRoom> currentGroupEnteredChatRooms; // 현재 그룹의 참여한 채팅방들
    HashMap<String, ChatRoom> currentGroupNotEnteredChatRooms; // 현재 그룹의 참여하지 않은 채팅방들

    @Override
    public void onCreate() {
        super.onCreate();
        FontUtil.setDefaultFont(this, "MONOSPACE", "Helvetica.ttf");
        FontUtil.setDefaultFont(this, "SANS", "HelveticaBold.ttf");
        friendRequest = new ArrayList<>();
        groupRequest = new ArrayList<>();
        currentGroupEnteredChatRooms = new HashMap<>();
        currentGroupNotEnteredChatRooms = new HashMap<>();
    }

    public ArrayList<Group> getGroupRequest() {
        return groupRequest;
    }

    public ArrayList<User> getFriendRequest() {
        return friendRequest;
    }

    public void setFriendRequest(ArrayList<User> friendRequest) {
        this.friendRequest = friendRequest;
    }

    public void setGroupRequest(ArrayList<Group> groupRequest) {
        this.groupRequest = groupRequest;
    }

    public void addFriendRequest(User user){
        friendRequest.add(user);
    }

    public void setMyself(Myself myself) {
        this.myself = myself;
    }

    public Myself getMyself(){
        return this.myself;
    }

    public void setCurrentGroup(MyGroup currentGroup) {
        this.currentGroup = currentGroup;
    }

    public MyGroup getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentChatRoom(ChatRoom currentChatRoom) {
        this.currentChatRoom = currentChatRoom;
    }

    public ChatRoom getCurrentChatRoom() {
        return currentChatRoom;
    }

    public void setCurrentGroupEnteredChatRooms(HashMap<String, ChatRoom> currentGroupEnteredChatRooms) {
        this.currentGroupEnteredChatRooms = currentGroupEnteredChatRooms;
    }

    public void setCurrentGroupNotEnteredChatRooms(HashMap<String, ChatRoom> currentGroupNotEnteredChatRooms) {
        this.currentGroupNotEnteredChatRooms = currentGroupNotEnteredChatRooms;
    }

    public HashMap<String, ChatRoom> getCurrentGroupEnteredChatRooms() {
        return currentGroupEnteredChatRooms;
    }

    public HashMap<String, ChatRoom> getCurrentGroupNotEnteredChatRooms() {
        return currentGroupNotEnteredChatRooms;
    }

    public void clearChatRooms(){
        this.currentGroupEnteredChatRooms.clear();
        this.currentGroupNotEnteredChatRooms.clear();
    }

    public void clearApplication(){
        this.myself = null;
        this.clearChatRooms();
        this.friendRequest = null;
        this.groupRequest = null;
        this.currentGroup = null;
        this.currentChatRoom = null;
    }
}
