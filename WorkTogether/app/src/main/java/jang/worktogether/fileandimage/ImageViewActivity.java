package jang.worktogether.fileandimage;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import jang.worktogether.R;
import jang.worktogether.chatting.ChattingRoomActivity;
import jang.worktogether.chatting.FilePacket;
import jang.worktogether.chatting.SocketService;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageViewActivity extends AppCompatActivity{

    static {
        System.loadLibrary("native-lib");
    }

    private native String processBitmap(String path, String tmpPath, int width, int height);

    ImageView imageView;
    ProgressBar loadingProgressBar;

    PhotoViewAttacher photoViewAttacher;
    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;
    String imagePath;

    Thread saveThread;

    //장치의 가로 세로 길이, 이미지 처리 후 임시파일 저장 경로
    int deviceWidth;
    int deviecHeight;
    String processedPath; // 이미지 처리 후 경로
    String tmpPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        broadCaster = LocalBroadcastManager.getInstance(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceWidth = displayMetrics.widthPixels;// 가로
        deviecHeight = displayMetrics.heightPixels;// 세로

        loadingProgressBar = (ProgressBar)findViewById(R.id.imageview_loading);
        imageView = (ImageView)findViewById(R.id.imageview_image);
        imagePath = getIntent().getStringExtra("path");
        registerReceiver();

        tmpPath = this.getCacheDir() + File.separator +
                System.currentTimeMillis() + "." +
                org.apache.commons.io.FilenameUtils.getExtension(imagePath);
        Log.i("chat", tmpPath);
        File file = new File(imagePath);
        if(file.exists()) {
            processedPath = processBitmap(imagePath, tmpPath, deviceWidth, deviecHeight);
            if(!processedPath.equals("read failed")) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(processedPath));
                photoViewAttacher = new PhotoViewAttacher(imageView);
            }
            else{
                imageView.setImageDrawable(ContextCompat.getDrawable(ImageViewActivity.this,
                        R.drawable.loadfail));
            }
        }
        else{
            if(getIntent().getStringExtra("id") != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                FilePacket filePacket =
                        new FilePacket(SocketService.REQUEST_IMAGE_DOWNLOAD);
                filePacket.addData(getIntent().getStringExtra("id"));
                Intent intent = new Intent(SocketService.REQUEST_IMAGE_DOWNLOAD);
                intent.putExtra("Packet", filePacket.toByteArray());
                broadCaster.sendBroadcast(intent);
            }
            else if(getIntent().getStringExtra("profile") != null){
                loadingProgressBar.setVisibility(View.VISIBLE);
                FilePacket filePacket =
                        new FilePacket(SocketService.PROFILE_DOWNLOAD);
                filePacket.addData(getIntent().getStringExtra("profile"),
                        getIntent().getStringExtra("userid"));
                Intent intent = new Intent(SocketService.PROFILE_DOWNLOAD);
                intent.putExtra("Packet", filePacket.toByteArray());
                broadCaster.sendBroadcast(intent);
            }
        }
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(ChattingRoomActivity.FILE_DOWNLOAD_FINISHED)){
                    loadingProgressBar.setVisibility(View.GONE);
                    processedPath = processBitmap(imagePath, tmpPath, deviceWidth, deviecHeight);
                    if(!processedPath.equals("read failed")) {
                        imageView.setImageBitmap(BitmapFactory.decodeFile(processedPath));
                        photoViewAttacher = new PhotoViewAttacher(imageView);
                    }
                    else{
                        imageView.setImageDrawable(ContextCompat.getDrawable(ImageViewActivity.this,
                                R.drawable.loadfail));
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(ChattingRoomActivity.FILE_DOWNLOAD_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File file = new File(tmpPath);
        if(file.exists()){
            if(!file.delete()){
                Log.i("chat", "tmp file delete failed!");
            }
        }
        if(saveThread != null){
            saveThread.interrupt();
            saveThread = null;
        }
        if(photoViewAttacher != null) {
            photoViewAttacher.cleanup();
            photoViewAttacher = null;
        }
        imageView.setImageBitmap(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.imageview_out : {
                this.finish();
                break;
            }
            case R.id.imageview_save : {
                saveImage();
                break;
            }
        }
    }

    private void saveImage(){
        String newDirectoryPath = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DCIM) + File.separator + "wtimages";
        final String newPath =  newDirectoryPath +
                File.separator + "wt_" + System.currentTimeMillis() +
                FileUtils.getExtension(imagePath);
        File dir = new File(newDirectoryPath);
        if(!dir.exists()){
            if(!dir.mkdirs()) {
                Log.i("chat", "저장 중 디렉토리 생성 실패");
                return;
            }
        }
        final File newFile = new File(newPath);
        saveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(imagePath);
                try {
                    FileInputStream fis = new FileInputStream(file);
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int readCount;
                    byte[] buffer = new byte[4096];
                    while((readCount = fis.read(buffer, 0, 4096)) != -1){
                        fos.write(buffer, 0, readCount);
                    }
                    fos.close();
                    fis.close();
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ImageViewActivity.this, "저장완료",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    MediaScannerConnection.scanFile(ImageViewActivity.this, new String[] { newFile.getPath() },
                            new String[] { "image/*" }, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        saveThread.start();
    }
}
