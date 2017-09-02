package jang.worktogether.group;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.MyGroup;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.ChattingListActivity;
import jang.worktogether.chatting.MakeChattingActivity;
import jang.worktogether.chatting.Packet;
import jang.worktogether.chatting.SocketService;

import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class MakeGroupActivity extends BaseActivity {

    WTapplication wtApplication;
    EditText group_name_et;
    EditText group_content_et;
    GridView memberGridView;

    GridViewAdapter gridViewAdapter;

    HttpUtil httpUtil;
    String name;
    String content;
    String chief_id;
    ArrayList<User> selectedFriends;
    ArrayList<String> selectedFriendsIDs;
    Gson gson;

    final int SELECT_FRIENDS_REQUEST_CODE = 1234;
    LocalBroadcastManager broadCaster;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group);
        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.group_make_mainLayout), this);
        wtApplication = (WTapplication)getApplicationContext();
        group_name_et = (EditText)findViewById(R.id.group_name_input);
        group_content_et = (EditText)findViewById(R.id.group_content_input);
        memberGridView = (GridView)findViewById(R.id.group_member_grid_view);

        broadCaster = LocalBroadcastManager.getInstance(this);
        selectedFriends = new ArrayList<>();
        gson = new Gson();

        httpUtil = new HttpUtil(this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(MakeGroupActivity.this, ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT).show();
                }
                else{
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        MyGroup myGroup = new MyGroup(jsonObject.getString("g_id"),
                                jsonObject.getString("g_name"), jsonObject.getString("g_content"),
                                jsonObject.getString("g_chief"));
                        myGroup.setMemberCount(Integer.parseInt(jsonObject.getString("member_count")));
                        Packet packet = new Packet(SocketService.NEW_GROUP_MAKE);
                        packet.addData(jsonObject.getString("g_id"));
                        Intent intent = new Intent(SocketService.NEW_GROUP_MAKE);
                        intent.putExtra("Packet", packet.toByteArray());
                        broadCaster.sendBroadcast(intent);
                        Log.i("chat", "보냈다");
                        wtApplication.getMyself().addGroup(myGroup);
                        wtApplication.setCurrentGroup(myGroup);
                        groupMakeCompleted(jsonObject.getString("g_id"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    finish();
                }
            }
        });

        gridViewAdapter = new GridViewAdapter(selectedFriends);
        memberGridView.setAdapter(gridViewAdapter);
        memberGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedFriends.remove(position);
                gridViewAdapter.notifyDataSetChanged();
                return false;
            }
        });
    }

    private void groupMakeCompleted(String groupID){
        HttpUtil userHttpUtil = new HttpUtil(this);
        userHttpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Log.i("chat", ErrorcodeUtil.errorMessage(response));
                }
                else if(response.length() == 2){
                    //결과 아무것도 없을 때
                    Log.i("chat", "없음");
                }
                else{ //결과 처리
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        HashMap<String, User> users = new HashMap<>();
                        for(int i=0; i<jsonArray.length(); i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            User user = new User(jsonObject.getString("u_id"),
                                    jsonObject.getString("u_name"),
                                    jsonObject.getString("u_status"),
                                    jsonObject.getString("u_profile"),
                                    User.Relation.NoRelation);
                            users.put(user.getId(), user);
                        }
                        wtApplication.getCurrentGroup().setGroupUsers(users);
                        if(progressDialog != null){
                            progressDialog.dismiss();
                        }
                        startActivity(new Intent(MakeGroupActivity.this, ChattingListActivity.class));
                        finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        userHttpUtil.setUrl("group_users.php")
                .setData("g_id", groupID)
                .setUseSession(true)
                .postData();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.group_friend_add_button : {
                Intent intent = new Intent(this, SelectFriendsActivity.class);
                if(selectedFriends.size() != 0){
                    intent.putExtra("alreadySelected", (ArrayList<User>)selectedFriends.clone());
                }
                startActivityForResult(intent, SELECT_FRIENDS_REQUEST_CODE);
                break;
            }
            case R.id.group_make_button : {
                name = group_name_et.getText().toString();
                content = group_content_et.getText().toString();
                if(name.equals("") || content.equals("")){
                    Toast.makeText(this, "빈칸을 모두 입력해주세요", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (selectedFriends.size() == 0) {
                    Toast.makeText(this, "초대할 그룹원을 선택해주세요", Toast.LENGTH_SHORT).show();
                    break;
                }
                chief_id = wtApplication.getMyself().getId();
                selectedFriendsIDs = new ArrayList<>();
                for(User user : selectedFriends){
                    selectedFriendsIDs.add(user.getId());
                }
                httpUtil.setUrl("make_group.php")
                        .setData("name", name)
                        .setData("content", content)
                        .setData("members", gson.toJson(selectedFriendsIDs))
                        .setUseSession(true)
                        .postData();
                progressDialog = ProgressDialog.show(MakeGroupActivity.this, "WorkTogether",
                        "그룹을 생성하는 중입니다");
                break;
            }
            case R.id.group_make_cancel_button : {
                finish();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_FRIENDS_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                ArrayList<User> getdata = (ArrayList<User>)data.getSerializableExtra("selectedUser");
                if(getdata != null){
                    selectedFriends.addAll(getdata);
                }
                gridViewAdapter.setAddedFriends(selectedFriends);
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter{

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
                Glide.with(MakeGroupActivity.this).load(thumbnailPath + addedFriends.get(position).getId()
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
