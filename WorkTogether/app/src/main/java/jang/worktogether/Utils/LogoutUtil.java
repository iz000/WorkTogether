package jang.worktogether.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import jang.worktogether.basic.WTapplication;
import jang.worktogether.chatting.SQLIte.DBManager;


public class LogoutUtil {

    public static void logout(Context context){
        SharedPreferences pref = context.getSharedPreferences("Login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        String loginType = pref.getString("LoginType", null);
        if(loginType != null){
            switch(loginType){ //페이스북 로그인이 된 상태였다면
                case "F" :
                    FacebookSdk.sdkInitialize(context);
                    LoginManager.getInstance().logOut();
                    break;
                case "G" :
                    googleSignOut(context);
                    break;
                case "N" :
                    break;
                default:
                    break;
            }
        }
        //sqlite db도 삭제해 주어야 함
        context.deleteDatabase(((WTapplication)context.getApplicationContext())
                .getMyself().getId()+ DBManager.DB_NAME);
        editor.putBoolean("isLogin", false);
        editor.putString("LoginType", null);
        editor.putString("Session-Cookie", null);
        editor.apply();
        Log.i("chat", "로그아웃유틸");
    }

    private static void googleSignOut(final Context context){
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .enableAutoManage((FragmentActivity)context, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.i("Chat", connectionResult.getErrorMessage());
                        Toast.makeText(context, "구글 클라이언트 접속에 실패했습니다", Toast.LENGTH_SHORT).show();
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
