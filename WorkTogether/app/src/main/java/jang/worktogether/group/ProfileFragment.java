package jang.worktogether.group;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.LoginActivity;
import jang.worktogether.basic.SplashActivity;
import jang.worktogether.basic.UserProfileActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.Group;
import jang.worktogether.basic.basic_class.Myself;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.Packet;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SocketService;
import jang.worktogether.fileandimage.ImageViewActivity;

import static android.app.Activity.RESULT_OK;
import static jang.worktogether.chatting.SocketService.LOGOUT;

public class ProfileFragment extends android.support.v4.app.Fragment implements View.OnClickListener {

    WTapplication wtApplication;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    CircleImageView profileImageView;
    TextView nameTv;
    TextView statusTv;
    HttpUtil httpUtil;

    final static int MODIFY_REQUEST = 1111;
    public final static String thumbnailPath = "http://45.32.109.86/profile/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wtApplication = (WTapplication)getActivity().getApplicationContext();
        pref = getActivity().getSharedPreferences("Login", Context.MODE_PRIVATE);
        editor = pref.edit();
        httpUtil = new HttpUtil(getActivity());
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(getActivity(), ErrorcodeUtil.errorMessage(response),
                            Toast.LENGTH_SHORT).show();
                    if(response.equals("105")){ // 세션이 만료되었으면 저장된 쿠키를 삭제
                        editor.putString("Session-Cookie", null);
                        editor.apply();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        getActivity().finish();
                    }
                }
                else {
                    try {
                        JSONObject responseToJson = new JSONObject(response);
                        Myself myself = wtApplication.getMyself();
                        myself.setProfile(responseToJson.getString("u_profile"));
                        myself.setStatus(responseToJson.getString("u_status"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(wtApplication.getMyself().getProfile().length() != 0){
                        Glide.with(getActivity()).load(thumbnailPath+wtApplication.getMyself().getId()
                                +"/thumb/"+wtApplication.getMyself()
                                .getProfile()).into(profileImageView);
                        profileImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(wtApplication.getMyself().getProfile().length() != 0) {
                                    Intent intent = new Intent(getActivity(), ImageViewActivity.class);
                                    String path = getActivity().getCacheDir() + File.separator +
                                            "profile" + File.separator +
                                            wtApplication.getMyself().getId() + File.separator +
                                            wtApplication.getMyself().getProfile();
                                    intent.putExtra("path", path);
                                    intent.putExtra("profile", wtApplication.getMyself().getProfile());
                                    intent.putExtra("userid", wtApplication.getMyself().getId());
                                    startActivity(intent);
                                }
                            }
                        });
                    }
                    else{
                       profileImageView.setImageDrawable(
                               ContextCompat.getDrawable(getActivity(), R.drawable.user));
                    }
                    nameTv.setText(wtApplication.getMyself().getName());
                    statusTv.setText(wtApplication.getMyself().getStatus());
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        v.findViewById(R.id.profile_modify_button).setOnClickListener(this);
        v.findViewById(R.id.logout_button).setOnClickListener(this);
        nameTv = (TextView)v.findViewById(R.id.name_tv);
        statusTv = (TextView)v.findViewById(R.id.status_tv);
        nameTv.setText(wtApplication.getMyself().getName());
        statusTv.setText(wtApplication.getMyself().getStatus());
        profileImageView = (CircleImageView)v.findViewById(R.id.profile_image);
        if(wtApplication.getMyself().getProfile().length() != 0){
            Glide.with(getActivity()).load(thumbnailPath+wtApplication.getMyself().getId()
                    +"/thumb/"+wtApplication.getMyself()
                    .getProfile()).error(R.drawable.user).into(profileImageView);
        }
        return v;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.profile_modify_button : {
                startActivityForResult(new Intent(getActivity(), ModifyProfileActivity.class), MODIFY_REQUEST);
                break;
            }
            case R.id.logout_button : {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("정말 로그아웃 하시겠습니까?(기기에 저장된 대화 내용은 모두 사라집니다)")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                logOut();
                            }
                        })
                        .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                break;
            }
        }
    }

    private void logOut(){
        String loginType = pref.getString("LoginType", null);
        if(loginType != null){
            switch(loginType){ //페이스북 로그인이 된 상태였다면
                case "F" :
                    FacebookSdk.sdkInitialize(getActivity());
                    LoginManager.getInstance().logOut();
                    break;
                case "G" :
                    googleSignOut();
                    break;
                case "N" :
                    break;
                default:
                    break;
            }
        }
        //로그아웃 할 때는 로그아웃 패킷을 보냄
        Packet packet = new Packet(LOGOUT);
        packet.addData("logout");
        Intent intent = new Intent(LOGOUT);
        intent.putExtra("Packet", packet.toByteArray());
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        //sqlite db도 삭제해 주어야 함
        getActivity().deleteDatabase(wtApplication.getMyself().getId()+DBManager.DB_NAME);
        editor.putBoolean("isLogin", false);
        editor.putString("LoginType", null);
        editor.putString("Session-Cookie", null);
        editor.apply();
        getActivity().startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MODIFY_REQUEST){
            if(resultCode == RESULT_OK){
                httpUtil.setUrl("login_check.php")
                        .setUseSession(true)
                        .postData();
            }
        }
    }

    private void googleSignOut(){
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.i("Chat", connectionResult.getErrorMessage());
                        Toast.makeText(getActivity(), "구글 클라이언트 접속에 실패했습니다", Toast.LENGTH_SHORT).show();
                    }
                }).addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {

                            }
                        });            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
    }

}
