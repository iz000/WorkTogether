package jang.worktogether.chatting;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;

import static jang.worktogether.chatting.SocketService.NEW_CHATTING;
import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class MakeChattingActivity extends BaseActivity{

    WTapplication wtApplication;
    EditText chatting_topic_et;
    GridView memberGridView;

    String topic;
    ArrayList<User> selectedUsers;
    String selectedUserIDs;
    GridViewAdapter gridViewAdapter;
    final int SELECT_USERS_REQUEST_CODE = 5678;

    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;

    public static final String CHATTING_MAKE_COMPLETED = "completed";
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_chatting);
        wtApplication = (WTapplication)getApplicationContext();
        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.chatting_make_mainLayout), this);

        chatting_topic_et = (EditText)findViewById(R.id.chatting_topic_input);
        memberGridView = (GridView)findViewById(R.id.chatting_member_grid_view);
        broadCaster = LocalBroadcastManager.getInstance(this);
        selectedUsers = new ArrayList<>();

        gridViewAdapter = new GridViewAdapter(selectedUsers);
        memberGridView.setAdapter(gridViewAdapter);
        memberGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedUsers.remove(position);
                gridViewAdapter.notifyDataSetChanged();
                return false;
            }
        });

        registerReceiver();
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(CHATTING_MAKE_COMPLETED)){
                    wtApplication.setCurrentChatRoom(wtApplication.getCurrentGroupEnteredChatRooms()
                            .get(intent.getStringExtra("c_id")));
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    startActivity(new Intent(MakeChattingActivity.this, ChattingRoomActivity.class));
                    finish();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHATTING_MAKE_COMPLETED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_USERS_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                ArrayList<User> getdata = (ArrayList<User>)data.getSerializableExtra("selectedUser");
                if(getdata != null){
                    selectedUsers.addAll(getdata);
                }
                gridViewAdapter.setAddedFriends(selectedUsers);
            }
        }
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.chatting_users_add_button : {
                Intent intent = new Intent(this, SelectUsersActivity.class);
                if(selectedUsers.size() != 0){
                    intent.putExtra("alreadySelected", (ArrayList<User>)selectedUsers.clone());
                }
                startActivityForResult(intent, SELECT_USERS_REQUEST_CODE);
                break;
            }
            case R.id.chatting_make_button : {
                topic = chatting_topic_et.getText().toString();
                if(topic.equals("")){
                    Toast.makeText(this, "채팅 주제를 입력해주세요", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (selectedUsers.size() == 0) {
                    Toast.makeText(this, "채팅에 초대할 그룹원을 선택해주세요", Toast.LENGTH_SHORT).show();
                    break;
                }
                selectedUserIDs = "";
                StringBuilder stringBuilder = new StringBuilder();
                for(User user : selectedUsers){
                    stringBuilder.append(user.getId())
                            .append(",");
                }
                stringBuilder.deleteCharAt(stringBuilder.length()-1);
                Packet packet = new Packet(NEW_CHATTING);
                packet.addData(wtApplication.getCurrentGroup().getId(),
                        stringBuilder.toString(), topic);
                Intent intent = new Intent(NEW_CHATTING);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                progressDialog = ProgressDialog.show(this, "WorkTogether", "채팅방을 생성하는 중입니다");
                break;
            }
            case R.id.chatting_make_cancel_button : {
                finish();
                break;
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter {

        ArrayList<User> addedFriends;
        LayoutInflater layoutInflater;

        private GridViewAdapter(ArrayList<User> addedFriends){
            this.addedFriends = addedFriends;
            layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        private void setAddedFriends(ArrayList<User> addedFriends) {
            this.addedFriends = addedFriends;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return addedFriends.size();
        }

        @Override
        public Object getItem(int position) {
            return addedFriends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null){
                v = layoutInflater.inflate(R.layout.gridview_selected_friend, parent, false);
            }
            if(addedFriends.get(position).getProfile().length() != 0) {
                CircleImageView profileImageView = (CircleImageView) v.findViewById(R.id.selected_profile);
                Glide.with(MakeChattingActivity.this).load(thumbnailPath + addedFriends.get(position).getId()
                        + "/thumb/" + addedFriends.get(position).getProfile())
                        .error(R.drawable.user)
                        .into(profileImageView);
            }
            TextView nameTv = (TextView)v.findViewById(R.id.selected_name);
            nameTv.setText(addedFriends.get(position).getName());
            return v;
        }
    }
}
