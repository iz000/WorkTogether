package jang.worktogether.fileandimage;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import jang.worktogether.R;
import jang.worktogether.Utils.DesignUtil;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.FileUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.basic.basic_class.WTImage;
import jang.worktogether.chatting.ChattingRoomActivity;
import jang.worktogether.chatting.FilePacket;
import jang.worktogether.chatting.SocketService;

public class ImageListActivity extends AppCompatActivity{

    RecyclerView imageRecyclerView;
    Toolbar toolbar;

    private ArrayList<WTImage> wtImages;
    RecyclerView.LayoutManager layoutManager;
    ImageAdapter imageAdapter;
    HttpUtil httpUtil;

    LocalBroadcastManager broadCaster;
    BroadcastReceiver broadcastReceiver;
    String imagePath = null;

    int imageViewSize;

    String cacheDirectory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        broadCaster = LocalBroadcastManager.getInstance(this);
        cacheDirectory = this.getCacheDir().getAbsolutePath();
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        imageViewSize = dm.widthPixels/4;

        imageRecyclerView = (RecyclerView)findViewById(R.id.image_recyclerView);
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

        getSupportActionBar().setTitle(null);
        ((TextView)findViewById(R.id.title)).setTypeface(Typeface.createFromAsset(getAssets(),
                "HelveticaBold.ttf"));

        wtImages = new ArrayList<>();
        layoutManager = new GridLayoutManager(this, 4);
        imageRecyclerView.setLayoutManager(layoutManager);

        httpUtil = new HttpUtil(this);
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(ImageListActivity.this, ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT)
                            .show();
                }
                else{
                    try{
                        JSONArray jsonArray = new JSONArray(response);
                        int length = jsonArray.length();
                        for(int i=0; i<length; i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            wtImages.add(new WTImage(jsonObject.getString("i_num"),
                                    jsonObject.getString("g_id"),
                                    jsonObject.getString("c_id"),
                                    jsonObject.getString("u_id"),
                                    jsonObject.getString("i_name"), cacheDirectory));
                        }
                        imageAdapter = new ImageAdapter(wtImages);
                        imageRecyclerView.setAdapter(imageAdapter);
                        imageAdapter.setOnButtonClick(new OnButtonClick() {
                            @Override
                            public void onButtonClick(WTImage wtImage) {
                                imageDownload(wtImage);
                            }
                        });
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
        httpUtil.setUrl("image_list.php")
                .setUseSession(true)
                .postData();
    }

    private void imageDownload(WTImage wtImage){
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("path", wtImage.getPath());
        intent.putExtra("id", wtImage.getImageID());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        ArrayList<WTImage> wtImages;
        LayoutInflater layoutInflater;
        OnButtonClick onButtonClick;

        ImageAdapter(ArrayList<WTImage> wtImages){
            this.wtImages = wtImages;
            layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        void setOnButtonClick(OnButtonClick onButtonClick) {
            this.onButtonClick = onButtonClick;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = layoutInflater.inflate(R.layout.recyclerview_image_list, parent, false);
            return new ViewHolderImage(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ViewHolderImage viewHolderImage = (ViewHolderImage)holder;
            viewHolderImage.imageView.getLayoutParams().width = imageViewSize;
            viewHolderImage.imageView.getLayoutParams().height = imageViewSize;
            viewHolderImage.imageView.requestLayout();
            Glide.with(ImageListActivity.this).load(wtImages.get(position).getThumbnailPath())
                    .error(R.drawable.loadfail)
                    .centerCrop().into(viewHolderImage.imageView);
            viewHolderImage.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClick.onButtonClick(wtImages.get(position));
                }
            });

        }

        @Override
        public int getItemCount() {
            return wtImages.size();
        }

        private class ViewHolderImage extends RecyclerView.ViewHolder{

            ImageView imageView;

            ViewHolderImage(View itemView) {
                super(itemView);
                imageView = (ImageView)itemView.findViewById(R.id.image_container);
            }
        }
    }

    private interface OnButtonClick{
        void onButtonClick(WTImage wtImage);
    }

}
