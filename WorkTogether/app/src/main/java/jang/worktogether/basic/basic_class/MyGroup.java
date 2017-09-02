package jang.worktogether.basic.basic_class;

import java.util.ArrayList;
import java.util.HashMap;

public class MyGroup extends Group{

    private HashMap<String, User> groupUsers;
    private int memberCount = 0;

    public MyGroup(String id, String name, String content, String chief_id) {
        super(id, name, content, chief_id);
        groupUsers = new HashMap<>();
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getMemberCount() {
        if(groupUsers.size() != memberCount && groupUsers.size() == 0){
            return memberCount;
        }
        return groupUsers.size();
    }

    public MyGroup(Group group, HashMap<String, User> groupUsers){
        super(group.getId(), group.getName(), group.getContent(), group.getChief_id());
        this.groupUsers = groupUsers;
    }

    public void setGroupUsers(HashMap<String, User> groupUsers) {
        this.groupUsers = groupUsers;
    }

    public HashMap<String, User> getGroupUsers() {
        return groupUsers;
    }

    public ArrayList<User> getGroupUsersAsList(){
        ArrayList<User> userArrayList = new ArrayList<>();
        for(String id : groupUsers.keySet()){
            userArrayList.add(groupUsers.get(id));
        }
        return userArrayList;
    }

    public void addUser(User user){
        groupUsers.put(user.getId(), user);
    }

    public void removeUser(User user){
        groupUsers.remove(user.getId());
    }


}
