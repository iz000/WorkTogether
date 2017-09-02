package jang.worktogether.basic;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.Socket;
import java.util.Date;

import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.basic.basic_class.Myself;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.SocketService;
import jang.worktogether.group.MainActivity;

public class SplashActivity extends BaseActivity {

    boolean isLogin = false;
    HttpUtil httpUtil;
    WTapplication wtApplication;

    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cleanCache();
        wtApplication = (WTapplication) getApplicationContext();
        final int noti = getIntent().getIntExtra("noti", 0);
        if(wtApplication.getMyself() != null){
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("noti", noti);
            startActivity(intent);
            this.finish();
            return;
        }
        setContentView(R.layout.activity_splash);
        broadCaster = LocalBroadcastManager.getInstance(this);

        httpUtil = new HttpUtil(SplashActivity.this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(SplashActivity.this, ErrorcodeUtil.errorMessage(response),
                            Toast.LENGTH_SHORT).show();
                    if(response.equals("105")){ // 세션이 만료되었으면 저장된 쿠키를 삭제
                        editor.putString("Session-Cookie", null);
                        editor.apply();
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                    SplashActivity.this.finish();
                }
                else {
                    try {
                        JSONObject responseToJson = new JSONObject(response);
                        wtApplication.setMyself(new Myself(responseToJson.getString("u_id"),
                                responseToJson.getString("u_name"),
                                responseToJson.getString("u_status"),
                                responseToJson.getString("u_profile"),
                                User.Relation.Myself,
                                responseToJson.getString("u_type")));
                        if(isMyServiceRunning(SocketService.class)){
                            //서비스가 실행중이라면 바로 넘어감
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            if(noti != 0){
                                intent.putExtra("noti", noti);
                            }
                            startActivity(intent);
                            SplashActivity.this.finish();
                        }
                        else{
                            //만약에 서비스가 실행중이 아니라면 소켓이 연결되면 넘어감
                            startService(new Intent(SplashActivity.this, SocketService.class));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        isLogin = pref.getBoolean("isLogin", false);
        if(isLogin){
            httpUtil.setUrl("login_check.php")
                    .setUseSession(true)
                    .postData();
        }
        else{
            //만약에 서비스가 실행중이라면
            if(isMyServiceRunning(SocketService.class)){
                stopService(new Intent(this, SocketService.class));
            }
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            SplashActivity.this.finish();
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(SocketService.SOCKET_CONNECT_SUCCESS)){
                    if(isLogin){
                        Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                        if(noti != 0){
                            mainIntent.putExtra("noti", noti);
                        }
                        startActivity(mainIntent);
                        SplashActivity.this.finish();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(SocketService.SOCKET_CONNECT_SUCCESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void cleanCache() { // 3일 이상된 캐쉬파일은 지움
        long currentTime = new Date().getTime();
        try {
            File dir = this.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(currentTime, dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.i("chat", "cache clean error: " + e.toString());
        }
    }

    private void deleteDir(long currentTime, File dir) {
        long diff =  currentTime - dir.lastModified();
        if (dir != null && dir.isDirectory() ) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteDir(currentTime, new File(dir, children[i]));
            }
        }
        else if(diff > 3 * 24 * 60 * 60 * 1000){
            // The directory is now empty so delete it
            dir.delete();
        }
    }
}
