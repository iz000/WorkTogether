package jang.worktogether.basic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.Packet;
import jang.worktogether.fileandimage.ImageViewActivity;
import jang.worktogether.group.ProfileFragment;

import static jang.worktogether.chatting.SocketService.REQUEST_FRIEND;
import static jang.worktogether.chatting.SocketService.REQUEST_RELATION;

public class UserProfileActivity extends AppCompatActivity {

    CircleImageView user_profile_iv;
    TextView user_name_tv;
    TextView user_status_tv;
    Button user_add_friend_btn;

    WTapplication wtApplication;
    User user; //선택된 유저
    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;

    public static String USER_RELATION_CHECK = "relationCheck";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        wtApplication = (WTapplication)getApplicationContext();
        broadCaster = LocalBroadcastManager.getInstance(this);
        registerReceiver();

        user_profile_iv = (CircleImageView)findViewById(R.id.user_profile_image);
        user_name_tv = (TextView)findViewById(R.id.user_name_tv);
        user_status_tv = (TextView)findViewById(R.id.user_status_tv);
        user_add_friend_btn = (Button)findViewById(R.id.user_add_friend_btn);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String id = intent.getStringExtra("id");
        if(id.equals(wtApplication.getMyself().getId())){
            user = wtApplication.getMyself();
            user_add_friend_btn.setText("나");
            user_add_friend_btn.setEnabled(false);
            user_add_friend_btn.setVisibility(View.VISIBLE);
        }
        else{
            switch (type){
                case "friend" : {
                    user = wtApplication.getMyself().getFriends().get(id);
                    break;
                }
                case "group" : {
                    user = wtApplication.getCurrentGroup().getGroupUsers().get(id);
                    break;
                }
                case "chatting" : {
                    user = wtApplication.getCurrentChatRoom().getUsers().get(id);
                    break;
                }
            }
            Packet packet = new Packet(REQUEST_RELATION);
            packet.addData(user.getId());
            Intent packetIntent = new Intent(REQUEST_RELATION);
            packetIntent.putExtra("Packet", packet.toByteArray());
            broadCaster.sendBroadcast(packetIntent);
        }

        if(!user.getProfile().equals("")){
            Glide.with(this).load(ProfileFragment.thumbnailPath+
                    user.getId()+"/thumb/"+user.getProfile())
                    .error(R.drawable.user)
                    .into(user_profile_iv);
        }
        else{
            user_profile_iv
                    .setImageDrawable(ContextCompat.getDrawable(this, R.drawable.user));
        }
        user_name_tv.setText(user.getName());
        user_status_tv.setText(user.getStatus());
        user_profile_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(user.getProfile().length() != 0) {
                    Intent intent = new Intent(UserProfileActivity.this, ImageViewActivity.class);
                    String path = UserProfileActivity.this.getCacheDir() + File.separator +
                            "profile" + File.separator + user.getId() + File.separator +
                            user.getProfile();
                    intent.putExtra("path", path);
                    intent.putExtra("profile", user.getProfile());
                    intent.putExtra("userid", user.getId());
                    startActivity(intent);
                }
            }
        });
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(USER_RELATION_CHECK)){
                    if(user.getId().equals(intent.getStringExtra("id"))){
                        String relation = intent.getStringExtra("relation");
                        switch (relation){
                            case "F" :
                                user.setRelation(User.Relation.Friend);
                                user_add_friend_btn.setText("친구");
                                user_add_friend_btn.setEnabled(false);
                                break;
                            case "R" :
                                user.setRelation(User.Relation.Requestee);
                                user_add_friend_btn.setText("친구 요청 중");
                                user_add_friend_btn.setEnabled(false);
                                break;
                            case "NO" :
                                user.setRelation(User.Relation.NoRelation);
                                user_add_friend_btn.setText("친구 요청");
                                user_add_friend_btn.setEnabled(true);
                                break;
                        }
                        user_add_friend_btn.setVisibility(View.VISIBLE);
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(USER_RELATION_CHECK);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.user_add_friend_btn : {
                Packet packet = new Packet(REQUEST_FRIEND);
                packet.addData(user.getId());
                Intent intent = new Intent(REQUEST_FRIEND);
                intent.putExtra("Packet", packet.toByteArray());
                broadCaster.sendBroadcast(intent);
                Toast.makeText(this, user.getName()+"님에게 친구 요청을 보냈습니다", Toast.LENGTH_SHORT).show();
                user_add_friend_btn.setText("친구 요청 중");
                user_add_friend_btn.setEnabled(false);
                break;
            }
            case R.id.user_out : {
                finish();
                break;
            }
        }
    }
}
