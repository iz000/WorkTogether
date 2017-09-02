package jang.worktogether.fileandimage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import jang.worktogether.R;
import jang.worktogether.Utils.DesignUtil;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.FileUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.basic_class.WTFile;
import jang.worktogether.chatting.ChattingRoomActivity;
import jang.worktogether.chatting.FilePacket;
import jang.worktogether.chatting.SocketService;


public class FileListActivity extends AppCompatActivity {

    LinearLayout mainLayout;
    RecyclerView fileRecyclerView;
    EditText fileSearchEt;
    Toolbar toolbar;

    ArrayList<WTFile> wtFiles;
    RecyclerView.LayoutManager layoutManager;
    FileAdapter fileAdapter;
    HttpUtil httpUtil;

    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        mainLayout = (LinearLayout)findViewById(R.id.filelist_mainLayout);
        KeyboardUtil.setupHideKeyBoard(mainLayout, this);
        broadCaster = LocalBroadcastManager.getInstance(this);

        fileRecyclerView = (RecyclerView)findViewById(R.id.file_recyclerView);

        fileSearchEt = (EditText)findViewById(R.id.search_file);
        fileSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(fileSearchEt.getText().length() == 0){
                    fileSearchEt.clearFocus();
                }
                fileAdapter.filter(fileSearchEt.getText().toString());
            }
        });

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.back_arrow);
        int px = DesignUtil.dpToPx(this, 30);
        Drawable drawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                px, px, true));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        wtFiles = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this);
        fileRecyclerView.setLayoutManager(layoutManager);

        getSupportActionBar().setTitle(null);
        ((TextView)findViewById(R.id.title)).setTypeface(Typeface.createFromAsset(getAssets(),
                "HelveticaBold.ttf"));

        httpUtil = new HttpUtil(this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(FileListActivity.this, ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT)
                            .show();
                }
                else{
                    try{
                        JSONArray jsonArray = new JSONArray(response);
                        int length = jsonArray.length();
                        for(int i=0; i<length; i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            wtFiles.add(new WTFile(jsonObject.getString("f_num"),
                                    jsonObject.getString("g_id"),
                                    jsonObject.getString("c_id"),
                                    jsonObject.getString("u_id"),
                                    jsonObject.getString("f_name"),
                                    jsonObject.getLong("f_length")));
                        }
                        fileAdapter = new FileAdapter(wtFiles);
                        fileRecyclerView.setAdapter(fileAdapter);
                        fileAdapter.setOnButtonClick(new OnButtonClick() {
                            @Override
                            public void onButtonClick(WTFile wtFile) {
                                fileDownload(wtFile);
                            }
                        });
                        registerReceiver();
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        Intent intent = getIntent();
        String g_id = intent.getStringExtra("g_id");
        String c_id = intent.getStringExtra("c_id");
        Log.i("chat", "g_id" + g_id);
        Log.i("chat", "c_id" + c_id);
        if(g_id != null){
            httpUtil.setData("g_id", g_id);
        }
        if(c_id != null){
            httpUtil.setData("c_id", c_id);
        }
        httpUtil.setUrl("file_list.php")
                .setUseSession(true)
                .postData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fileAdapter = null;
        wtFiles = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(ChattingRoomActivity.FILE_DOWNLOAD_FINISHED)){
                    if(fileAdapter != null){
                        fileAdapter.notifyDataSetChanged();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(ChattingRoomActivity.FILE_DOWNLOAD_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void fileDownload(final WTFile wtFile){
        final File file = new File(wtFile.getPath());
        if(file.exists()){ // 파일이 이미 존재할 때
            if(wtFile.getFileLength() != file.length()){
                //파일이 존재하지만 파일의 길이가 다를 때
                final AlertDialog.Builder innerBuilder = new AlertDialog.Builder(FileListActivity.this);
                innerBuilder.setMessage("불완전한 파일이 존재합니다. 이어서 다운로드 하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FilePacket filePacket =
                                        new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                                filePacket.addData(wtFile.getFileID(),
                                        Long.toString(file.length()));
                                Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                                intent.putExtra("Packet", filePacket.toByteArray());
                                broadCaster.sendBroadcast(intent);
                            }
                        })
                        .setNegativeButton("새로 다운로드", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FilePacket filePacket =
                                        new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                                filePacket.addData(wtFile.getFileID(), "0");
                                Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                                intent.putExtra("Packet", filePacket.toByteArray());
                                broadCaster.sendBroadcast(intent);
                            }
                        });
                innerBuilder.create().show();
            }
            else{ // 파일을 다 다운로드 받아서 클릭했을 때 열기를 할 때
                FileUtil.viewFile(FileListActivity.this, file);
            }
        }
        else { // 파일이 존재하지 않아 아예처음 다운로드 할 때
            AlertDialog.Builder builder = new AlertDialog.Builder(FileListActivity.this);
            builder.setMessage("파일을 다운로드 하시겠습니까?")
                    .setPositiveButton("예", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FilePacket filePacket =
                                    new FilePacket(SocketService.REQUEST_FILE_DOWNLOAD);
                            filePacket.addData(wtFile.getFileID(), "0");
                            Intent intent = new Intent(SocketService.REQUEST_FILE_DOWNLOAD);
                            intent.putExtra("Packet", filePacket.toByteArray());
                            broadCaster.sendBroadcast(intent);
                        }
                    })
                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        ArrayList<WTFile> wtFiles;
        ArrayList<WTFile> reserve;
        LayoutInflater layoutInflater;
        OnButtonClick onButtonClick;

        private FileAdapter(ArrayList<WTFile> wtFiles){
            this.wtFiles = new ArrayList<>();
            this.reserve = new ArrayList<>();
            this.wtFiles.addAll(wtFiles);
            this.reserve.addAll(wtFiles);
            this.layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void setOnButtonClick(OnButtonClick onButtonClick) {
            this.onButtonClick = onButtonClick;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = layoutInflater.inflate(R.layout.recyclerview_file_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            WTFile wtFile = wtFiles.get(position);
            ViewHolder viewHolder = (ViewHolder)holder;
            viewHolder.fileNameTv.setText(wtFile.getFileName());
            File file = new File(wtFiles.get(position).getPath());
            if(file.exists() && file.length() == wtFile.getFileLength()){
                viewHolder.fileDownloadBtn.setText("열기");
            }
            viewHolder.fileDownloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClick.onButtonClick(wtFiles.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return wtFiles.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder{

            TextView fileNameTv;
            Button fileDownloadBtn;

            ViewHolder(View itemView) {
                super(itemView);
                fileNameTv = (TextView)itemView.findViewById(R.id.file_name_tv);
                fileDownloadBtn = (Button)itemView.findViewById(R.id.file_download_btn);
            }
        }

        private void filter(String charText){
            charText = charText.toLowerCase(Locale.getDefault());
            wtFiles.clear();
            if (charText.length() == 0) {
                wtFiles.addAll(reserve);
            }
            else{
                for (WTFile wtFile : reserve){
                    if (wtFile.getFileName().toLowerCase(Locale.getDefault()).contains(charText)){
                        wtFiles.add(wtFile);
                    }
                }
            }
            notifyDataSetChanged();
        }


    }

    private interface OnButtonClick{
        void onButtonClick(WTFile wtFile);
    }
}
