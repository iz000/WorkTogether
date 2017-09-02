package jang.worktogether.basic.basic_class;


import java.util.ArrayList;
import java.util.HashMap;

public class Myself extends User {

    private String type;
    private HashMap<String, User> friends;
    private HashMap<String, MyGroup> groups;

    public Myself(String id, String name, String status, String profile, Relation relation, String type) {
        super(id, name, status, profile, relation);
        this.type = type;
        friends = new HashMap<>();
        groups = new HashMap<>();
    }

    public void addGroup(MyGroup group){
        if(groups != null){
            groups.put(group.getId(), group);
        }
    }

    public void removeGroup(String groupID){
        if(groups != null){
            groups.remove(groupID);
        }
    }

    public void setGroups(HashMap<String, MyGroup> groups) {
        this.groups = groups;
    }

    public HashMap<String, MyGroup> getGroups() {
        return groups;
    }

    public void addFriend(User user) {
        this.friends.put(user.getId(), user);
    }

    public void removeFriend(User user){
        this.friends.remove(user.getId());
    }

    public void setFriends(HashMap<String, User> friends) {
        this.friends = friends;
    }

    public HashMap<String, User> getFriends() {
        return friends;
    }

    public ArrayList<User> getFriendsAsList(){
        ArrayList<User> friendsList = new ArrayList<>();
        for(String id : friends.keySet()){
            friendsList.add(friends.get(id));
        }
        return friendsList;
    }
    public String getType() {
        return type;
    }

}
