package jang.worktogether.group;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.JoinActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.Group;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.Packet;
import jang.worktogether.chatting.SocketService;

public class RequestFragment extends Fragment {

    HttpUtil httpUtil;
    WTapplication wtApplication;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    ListView friendRequestListview;
    FriendRequestAdapter friendRequestAdapter;
    ListView groupRequestListview;
    GroupRequestAdapter groupRequestAdapter;
    ImageView friendAddBtn;

    LocalBroadcastManager broadCaster;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wtApplication = (WTapplication)getActivity().getApplicationContext();
        pref = getActivity().getSharedPreferences("Login", Context.MODE_PRIVATE);
        editor = pref.edit();
        broadCaster = LocalBroadcastManager.getInstance(getActivity());

        httpUtil = new HttpUtil(getActivity());
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(getActivity(), ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT)
                            .show();
                }
                else{
                    try{
                        JSONArray jsonArray = new JSONArray(response);

                        JSONArray friendRequest = jsonArray.getJSONArray(0);
                        int friendRequestLength = friendRequest.length();
                        ArrayList<User> friendRequests = new ArrayList<>();
                        for(int i=0; i<friendRequestLength; i++){
                            JSONObject jsonObject = friendRequest.getJSONObject(i);
                            friendRequests.add(new User(jsonObject.getString("u_id"),
                                    jsonObject.getString("u_name"), jsonObject.getString("u_status"),
                                    jsonObject.getString("u_profile"), User.Relation.Requester));
                        } // 친구요청 데이터 받아옴
                        wtApplication.setFriendRequest(friendRequests);
                        friendRequestAdapter.setFriendRequests(wtApplication.getFriendRequest());

                        JSONArray groupRequest = jsonArray.getJSONArray(1);
                        int groupRequestLength = groupRequest.length();
                        ArrayList<Group> groupRequests = new ArrayList<>();
                        for(int i=0; i<groupRequestLength; i++){
                            JSONObject jsonObject = groupRequest.getJSONObject(i);
                            groupRequests.add(new Group(jsonObject.getString("g_id"),
                                    jsonObject.getString("g_name"), jsonObject.getString("g_content"),
                                    jsonObject.getString("g_chief")));
                        } // 그룹요청 데이터 받아옴
                        wtApplication.setGroupRequest(groupRequests);
                        groupRequestAdapter.setGroupRequests(wtApplication.getGroupRequest());
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        httpUtil.setUrl("request.php")
                .setUseSession(true)
                .postData();

    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_request, container, false); // 이부분 중요
        friendRequestListview = (ListView)v.findViewById(R.id.friend_request_listview);
        groupRequestListview = (ListView)v.findViewById(R.id.group_request_listview);
        groupRequestAdapter = new GroupRequestAdapter(wtApplication.getGroupRequest(), getActivity());
        groupRequestAdapter.setOnButtonClickCallback(new OnButtonClickCallback() {
            @Override
            public void onAcceptButtonClick(String id, int position) {
                Packet packet = new Packet(SocketService.GROUP_REQUEST_ACCEPT);
                packet.addData(wtApplication.getGroupRequest().get(position).getId());
                Intent intent = new Intent(SocketService.GROUP_REQUEST_ACCEPT);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                wtApplication.getGroupRequest().remove(position);
                groupRequestAdapter.notifyDataSetChanged();
            }
            @Override
            public void onRefuseButtonClick(String id, int position) {
                Packet packet = new Packet(SocketService.GROUP_REQUEST_CANCEL);
                packet.addData(wtApplication.getGroupRequest().get(position).getId());
                Intent intent = new Intent(SocketService.GROUP_REQUEST_CANCEL);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                wtApplication.getGroupRequest().remove(position);
                groupRequestAdapter.notifyDataSetChanged();
            }
        }); // 그룹 초대 수락, 거절 버튼 눌렀을 때

        friendRequestAdapter = new FriendRequestAdapter(wtApplication.getFriendRequest(), getActivity());
        friendRequestAdapter.setOnButtonClickCallback(new OnButtonClickCallback() {
            @Override
            public void onAcceptButtonClick(String id, int position) {
                Packet packet = new Packet(SocketService.FRIEND_REQUEST_ACCEPT);
                packet.addData(wtApplication.getFriendRequest().get(position).getId());
                Intent intent = new Intent(SocketService.FRIEND_REQUEST_ACCEPT);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                wtApplication.getFriendRequest().remove(position);
                friendRequestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onRefuseButtonClick(String id, int position) {
                Packet packet = new Packet(SocketService.FRIEND_REQUEST_CANCEL);
                packet.addData(wtApplication.getFriendRequest().get(position).getId());
                Intent intent = new Intent(SocketService.FRIEND_REQUEST_CANCEL);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                wtApplication.getFriendRequest().remove(position);
                friendRequestAdapter.notifyDataSetChanged();
            }
        }); // 친구 요청 수락, 거절 눌렀을 때

        friendRequestListview.setAdapter(friendRequestAdapter);
        groupRequestListview.setAdapter(groupRequestAdapter);

        friendAddBtn = (ImageView)v.findViewById(R.id.friend_add_button);
        friendAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                View dialog = inflater.inflate(R.layout.dialog_add_friend, null);
                final TextInputLayout textInputLayout = (TextInputLayout)
                        dialog.findViewById(R.id.dialog_input_layout);
                textInputLayout.setErrorEnabled(true);
                final TextInputEditText editText = (TextInputEditText)
                        dialog.findViewById(R.id.dialog_input);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if(s.toString().equals("")) {
                            textInputLayout.setError(null);
                        }
                    }
                });
                final AlertDialog alertDialog = builder.setMessage("친구 추가를 원하는 유저의 이메일을 입력하세요")
                        .setView(dialog)
                        .setPositiveButton("요청 보내기", null)
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button b = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String input = editText.getText().toString();
                                if(input.equals("")){
                                    textInputLayout.setError("이메일을 입력해주세요");
                                }
                                else if(!JoinActivity.isEmailValid(input)){
                                    textInputLayout.setError("이메일 형식에 맞춰 입력해주세요");
                                }
                                else if(!input.equals("")){
                                    Packet packet = new Packet(SocketService.REQUEST_FRIEND_BY_EMAIL);
                                    packet.addData(input);
                                    Intent intent = new Intent(SocketService.REQUEST_FRIEND_BY_EMAIL);
                                    intent.putExtra("Packet", packet.toByteArray());
                                    broadCaster.sendBroadcast(intent);
                                    alertDialog.dismiss();
                                }
                            }
                        });
                    }
                });
                alertDialog.show();
            }
        });

        return v;
    }

    public void requestChanged(){
        if(friendRequestAdapter != null){
            friendRequestAdapter.notifyDataSetChanged();
        }
        if(groupRequestAdapter != null){
            groupRequestAdapter.notifyDataSetChanged();
        }
    }

    private class FriendRequestAdapter extends BaseAdapter{

        ArrayList<User> friendRequests;
        LayoutInflater layoutInflater;
        OnButtonClickCallback onButtonClickCallback;

        public FriendRequestAdapter(ArrayList<User> friendRequests, Context context){
            this.friendRequests = friendRequests;
            layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setOnButtonClickCallback(OnButtonClickCallback onButtonClickCallback) {
            this.onButtonClickCallback = onButtonClickCallback;
        }

        public void setFriendRequests(ArrayList<User> friendRequests) {
            this.friendRequests = friendRequests;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return friendRequests.size();
        }

        @Override
        public Object getItem(int position) {
            return friendRequests.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null){
                v = layoutInflater.inflate(R.layout.listview_friend_request, null);
            }
//            CircleImageView circleImageView = (CircleImageView)v.findViewById(R.id.request_profile_image);
//            Glide.with(getActivity()).load(friendRequests.get(position).getProfile())
//                    .into(circleImageView);
            TextView nameTv = (TextView)v.findViewById(R.id.request_user_name);
            Button accpetBtn = (Button)v.findViewById(R.id.friend_accpet_button);
            Button refuseBtn = (Button)v.findViewById(R.id.friend_refuse_button);
            nameTv.setText(friendRequests.get(position).getName());
            accpetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClickCallback.onAcceptButtonClick(friendRequests.get(position).getId(), position);
                }
            });
            refuseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClickCallback.onRefuseButtonClick(friendRequests.get(position).getId(), position);
                }
            });
            return v;
        }
    }

    private class GroupRequestAdapter extends BaseAdapter{

        ArrayList<Group> groupRequests;
        LayoutInflater layoutInflater;
        OnButtonClickCallback onButtonClickCallback;

        public GroupRequestAdapter(ArrayList<Group> groupRequests, Context context){
            this.groupRequests = groupRequests;
            layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setOnButtonClickCallback(OnButtonClickCallback onButtonClickCallback) {
            this.onButtonClickCallback = onButtonClickCallback;
        }

        public void setGroupRequests(ArrayList<Group> groupRequests) {
            this.groupRequests = groupRequests;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return groupRequests.size();
        }

        @Override
        public Object getItem(int position) {
            return groupRequests.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null){
                v = layoutInflater.inflate(R.layout.listview_group_request, null);
            }
            TextView nameTv = (TextView)v.findViewById(R.id.request_group_name);
            TextView contentTv = (TextView)v.findViewById(R.id.request_group_content);
            Button accpetBtn = (Button)v.findViewById(R.id.group_accpet_button);
            Button refuseBtn = (Button)v.findViewById(R.id.group_refuse_button);
            nameTv.setText(groupRequests.get(position).getName());
            contentTv.setText(groupRequests.get(position).getContent());
            accpetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClickCallback.onAcceptButtonClick(groupRequests.get(position).getId(), position);
                }
            });
            refuseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClickCallback.onRefuseButtonClick(groupRequests.get(position).getId(), position);
                }
            });
            return v;
        }
    }

    public interface OnButtonClickCallback{
        void onAcceptButtonClick(String id, int position);
        void onRefuseButtonClick(String id, int position);
    }
}
