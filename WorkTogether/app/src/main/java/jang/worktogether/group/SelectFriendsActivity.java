package jang.worktogether.group;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;

public class SelectFriendsActivity extends AppCompatActivity {

    WTapplication wtApplication;
    ListView friendsListView;
    SelectFriendListAdapter selectFriendListAdapter;

    ArrayList<User> selectedUserList;
    ArrayList<User> friendList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friends);
        wtApplication = (WTapplication)getApplicationContext();

        friendsListView = (ListView)findViewById(R.id.select_friend_listview);
        friendList = (ArrayList<User>)wtApplication.getMyself().getFriendsAsList().clone();
        Intent intent = getIntent();
        if(intent != null){
            ArrayList<User> alreadySelected = (ArrayList<User>)intent.getSerializableExtra("alreadySelected");
            if(alreadySelected != null){
                for(int i=0; i<alreadySelected.size(); i++){
                    for(int j=friendList.size()-1; j>=0; j--){
                        if(friendList.get(j).getId().equals(alreadySelected.get(i).getId())){
                            friendList.remove(j);
                        }
                    }
                }
            }
        }
        selectFriendListAdapter = new SelectFriendListAdapter(friendList, this);
        friendsListView.setAdapter(selectFriendListAdapter);
        selectedUserList = new ArrayList<>();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.select_complete_button : {
                SparseBooleanArray selectedUsers = friendsListView.getCheckedItemPositions();
                for(int i=0; i<selectedUsers.size(); i++){
                    if(selectedUsers.valueAt(i)){
                        selectedUserList.add(friendList.get(selectedUsers.keyAt(i)));
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
