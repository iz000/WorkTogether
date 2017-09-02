package jang.worktogether.chatting;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

import jang.worktogether.R;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.group.SelectFriendListAdapter;
import jang.worktogether.group.SelectFriendsActivity;

public class SelectUsersActivity extends AppCompatActivity{

    WTapplication wtApplication;
    ListView usersListView;
    SelectFriendListAdapter selectUserListAdapter;

    ArrayList<User> selectedUserList;
    ArrayList<User> userList;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friends);
        wtApplication = (WTapplication)getApplicationContext();

        usersListView = (ListView)findViewById(R.id.select_friend_listview);
        userList = (ArrayList<User>)wtApplication.getCurrentGroup().getGroupUsersAsList().clone();
        for(int i=0; i<userList.size(); i++){
            if(userList.get(i).getId().equals(wtApplication.getMyself().getId())){
                userList.remove(i);
                break;
            }
        }
        Intent intent = getIntent();
        if(intent != null){
            ArrayList<User> alreadySelected = (ArrayList<User>)intent.getSerializableExtra("alreadySelected");
            if(alreadySelected != null){
                for(int i=0; i<alreadySelected.size(); i++){
                    for(int j=userList.size()-1; j>=0; j--){
                        if(userList.get(j).getId().equals(alreadySelected.get(i).getId())){
                            userList.remove(j);
                        }
                    }
                }
            }
        }

        selectUserListAdapter = new SelectFriendListAdapter(userList, this);
        usersListView.setAdapter(selectUserListAdapter);
        selectedUserList = new ArrayList<>();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.select_complete_button : {
                SparseBooleanArray selectedUsers = usersListView.getCheckedItemPositions();
                for(int i=0; i<selectedUsers.size(); i++){
                    if(selectedUsers.valueAt(i)){
                        selectedUserList.add(userList.get(selectedUsers.keyAt(i)));
                    }
                }
                Intent intent = getIntent();
                intent.putExtra("selectedUser", selectedUserList);
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
            case R.id.select_cancel_button : {
                setResult(RESULT_CANCELED);
                finish();
                break;
            }
        }
    }

}
