package jang.worktogether.group;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.BaseActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.chatting.FilePacket;
import jang.worktogether.chatting.SocketService;

public class ModifyProfileActivity extends BaseActivity {

    TextView nameTv;
    TextView statusEt;
    CircleImageView profileImageView;
    WTapplication wtApplication;
    HttpUtil httpUtil;

    Uri uri = null; // 프로필 사진 가져올 때 uri
    private final int SELECT_PROFILE = 9876;
    public final static String PROFILE_UPLOADED = "profileUploaded";
    BroadcastReceiver broadcastReceiver;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_profile);

        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.modify_mainLayout), this);
        wtApplication = (WTapplication)getApplicationContext();
        httpUtil = new HttpUtil(this);

        nameTv = (TextView)findViewById(R.id.modify_name_tv);
        statusEt = (EditText)findViewById(R.id.modify_status_et);
        profileImageView = (CircleImageView)findViewById(R.id.modify_profile_image);

        nameTv.setText(wtApplication.getMyself().getName());
        statusEt.setText(wtApplication.getMyself().getStatus());
        if(wtApplication.getMyself().getProfile().length() != 0){
            Glide.with(this).load(ProfileFragment.thumbnailPath+
                    wtApplication.getMyself().getId()+"/thumb/" +
                    wtApplication.getMyself().getProfile())
                    .error(R.drawable.user)
                    .into(profileImageView);
        }
        else{
            profileImageView.setImageDrawable(
                    ContextCompat.getDrawable(ModifyProfileActivity.this, R.drawable.user));
        }

        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                stopProgressDialog();
                if(response.length() == 3){
                    Toast.makeText(ModifyProfileActivity.this, ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT).show();
                }
                else{
                    if(response.equals("0")){
                        Toast.makeText(ModifyProfileActivity.this, "수정 성공", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            }
        });

        registerReceiver();
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(PROFILE_UPLOADED)){
                    httpUtil.setUrl("modify_profile.php")
                            .setData("status", statusEt.getText().toString())
                            .setUseSession(true)
                            .postData();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(PROFILE_UPLOADED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.modify_profile_image : {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                uri = getFileUri();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PROFILE);
                break;
            }
            case R.id.modify_button : {
                startProgressDialog();
                if(uri != null) {
                    Intent intent = new Intent(SocketService.PROFILE_UPLOAD);
                    intent.putExtra("Path", uri.getPath());
                    FilePacket packet = new FilePacket(SocketService.PROFILE_UPLOAD);
                    packet.setFileLength((int) (new File(uri.getPath()).length()));
                    intent.putExtra("Packet", packet.toByteArray());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    uri = null;
                }
                else{
                    httpUtil.setUrl("modify_profile.php")
                            .setData("status", statusEt.getText().toString())
                            .setUseSession(true)
                            .postData();
                }
                break;
            }
            case R.id.modify_cancel_button : {
                this.finish();
                break;
            }
        }
    }

    private void startProgressDialog(){
        progressDialog = ProgressDialog.show(this, "수정", "프로필 수정 중 입니다");
    }

    private void stopProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_PROFILE){
                Glide.with(this).load(uri).into(profileImageView);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private Uri getFileUri(){
        return Uri.fromFile(this.getFile());
    }

    private File getFile(){
        File dir = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                +"/WorkTogehter/Profile");
        if(!dir.exists()){
            if(!dir.mkdirs()){
                return null;
            }
        }
        File f = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                +"/WorkTogehter/Profile/profile_"+System.currentTimeMillis()+"_"
                + wtApplication.getMyself().getId()+".jpg");
        try{
            if(!f.createNewFile()){
                return null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return f;
    }
}
