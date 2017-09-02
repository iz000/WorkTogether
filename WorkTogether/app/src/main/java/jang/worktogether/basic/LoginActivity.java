package jang.worktogether.basic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.SocketService;
import jang.worktogether.group.MainActivity;
import jang.worktogether.R;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.basic_class.Myself;

public class LoginActivity extends BaseActivity {


    private HttpUtil httpUtil;
    EditText email_et;
    EditText password_et;
    WTapplication wtApplication;
    BroadcastReceiver broadcastReceiver;

    ProgressDialog progressDialog;

    //페이스북
    CallbackManager callbackManager;
    AccessToken accessToken;
    LoginButton loginButton;

    //구글 로그인
    GoogleApiClient googleApiClient;
    GoogleSignInOptions gso;
    final int RC_SIGN_IN = 1000;

    public static String RE_LOGIN_NEEDED = "need re_login";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        setContentView(R.layout.activity_login);
        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.login_mainLayout), LoginActivity.this);

        email_et = (EditText)findViewById(R.id.input_email);
        password_et = (EditText)findViewById(R.id.input_password);

        int color = Color.parseColor("#4468FC");
        email_et.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        password_et.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        wtApplication = (WTapplication)getApplicationContext();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(SocketService.SOCKET_CONNECT_SUCCESS)) {
                    stopProgressDialog();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    LoginActivity.this.finish();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(SocketService.SOCKET_CONNECT_SUCCESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        httpUtil = new HttpUtil(LoginActivity.this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    stopProgressDialog();
                    Toast.makeText(LoginActivity.this, ErrorcodeUtil.errorMessage(response),
                            Toast.LENGTH_SHORT).show();
                    if(response.equals("106")){
                        if(isFacebookLoggedIn()){
                            LoginManager.getInstance().logOut();
                        }
                        else{
                            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(Status status) {

                                        }
                                    });
                        }
                    }
                }
                else{
                    try {
                        JSONObject responseToJson = new JSONObject(response);
                        wtApplication.setMyself(new Myself(responseToJson.getString("u_id"),
                                responseToJson.getString("u_name"),
                                responseToJson.getString("u_status"),
                                responseToJson.getString("u_profile"),
                                User.Relation.Myself,
                                responseToJson.getString("u_type")));
                        editor.putBoolean("isLogin", true);
                        editor.apply();
                        startService(new Intent(LoginActivity.this, SocketService.class));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if(isFacebookLoggedIn()){ // 로그인 페이지에 들어왔는데 페이스북 로그인되어있으면 로그아웃
            LoginManager.getInstance().logOut();
        }

        //페이스북 로그인
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton)findViewById(R.id.facebook_login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String string = loginResult.getRecentlyDeniedPermissions().toString();
                if(string.equals("[email]")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setMessage("이메일은 아이디로 사용이 됩니다. 서비스를 제대로 이용하시려면 이메일 권한을 허락해 주세요.")
                            .setPositiveButton("권한 다시 요청", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    LoginManager.getInstance().logInWithReadPermissions(
                                            LoginActivity.this, Arrays.asList("email")
                                    );
                                }
                            })
                            .setNegativeButton("권한 요청 안함", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    LoginManager.getInstance().logOut();
                                }
                            })
                            .setCancelable(false).create().show();
                    return;
                }

                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject me, GraphResponse response) {
                                if (response.getError() != null) {
                                    Toast.makeText(LoginActivity.this, "페이스북 오류 : 다시 시도해주세요", Toast.LENGTH_SHORT).show();
                                } else {
                                    httpUtil.clearData();
                                    httpUtil.setUrl("login_with_facebook_google.php")
                                            .setData("email", me.optString("email"))
                                            .setData("name", me.optString("name"))
                                            .setData("type", "F")
                                            .setSaveCookie(true)
                                            .postData();
                                    editor.putString("LoginType", "F"); // 로그인 타입 지정
                                    editor.apply();
                                    startProgressDialog();
                                }
                            }
                        }
                );
                Bundle bundle = new Bundle();
                bundle.putString("fields", "id, email, name");
                graphRequest.setParameters(bundle);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {
                LoginManager.getInstance().logOut();
                stopProgressDialog();
            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        //구글 로그인
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.i("Chat", connectionResult.getErrorMessage());
                        stopProgressDialog();
                        Toast.makeText(LoginActivity.this, "구글 클라이언트 접속에 실패했습니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        email_et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP){
                    password_et.requestFocus();
                    return true;
                }
                return false;
            }
        });

        password_et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP){
                    login();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startProgressDialog(){
        progressDialog = ProgressDialog.show(this, "Login", "로그인 중 입니다");
    }

    private void stopProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.login_button : {
                login();
                break;
            }
            case R.id.join_message : {
                startActivity(new Intent(this, JoinActivity.class));
                break;
            }
        }
    }

    private void login(){
        if(email_et.getText().toString().equals("") || password_et.getText().toString().equals("")){
            Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        httpUtil.clearData();
        httpUtil.setUrl("login.php")
                .setData("email", email_et.getText().toString())
                .setData("password", password_et.getText().toString())
                .setSaveCookie(true)
                .postData();
        editor.putString("LoginType", "N"); // 로그인 타입 지정
        editor.apply();
        startProgressDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){ // 구글 로그인 성공
                GoogleSignInAccount account = result.getSignInAccount();
                httpUtil.clearData();
                httpUtil.setUrl("login_with_facebook_google.php")
                        .setData("email", account.getEmail())
                        .setData("name", account.getDisplayName())
                        .setData("type", "G")
                        .setSaveCookie(true)
                        .postData();
                startProgressDialog();
                editor.putString("LoginType", "G"); // 로그인 타입 지정
                editor.apply();
            }
        }
        else{
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Boolean isFacebookLoggedIn(){ // 페이스북 로그인 되었는지 안되었는지 확인
        accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

}
