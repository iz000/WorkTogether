package jang.worktogether.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.telecom.Call;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class HttpUtil {

    private HashMap<String, String> postData;
    private String url;
    private Callback callback;
    private Post post = null;
    private String cookie = null;
    private HttpURLConnection conn;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private boolean saveCookie = false;
    private boolean useSession = false;
    private final String fixedUrl = "http://45.32.109.86/GroupWork/";

    public HttpUtil(Context context){
        postData = new HashMap<>();
        pref = context.getSharedPreferences("Login", MODE_PRIVATE);
        editor = pref.edit();
    }

    public HttpUtil setUrl(String url){
        this.url = fixedUrl + url;
        return this;
    }

    public HttpUtil setData(String key, String value){
        postData.put(key, value);
        return this;
    }

    public HttpUtil setCallback(Callback callback){
        this.callback = callback;
        return this;
    }

    public HttpUtil setSaveCookie(boolean saveCookie){
        this.saveCookie = saveCookie;
        return this;
    }

    public HttpUtil setUseSession(boolean useSession){
        cookie = pref.getString("Session-Cookie", null);
        this.useSession = useSession;
        return this;
    }

    public void clearData(){
        cookie = null;
        saveCookie = false;
        useSession = false;
        postData.clear();
    }

    public void postData(){
        post = null;
        post = new Post();
        post.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void saveCookie(){
        Map<String, List<String>> imap = conn.getHeaderFields();
        if(imap.containsKey("Set-Cookie")){
            List<String> iString = imap.get("Set-Cookie");
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<iString.size(); i++){
                builder.append(iString.get(i));
            }
            cookie = builder.toString();
            editor.putString("Session-Cookie", cookie);
            editor.apply();
        }
    }

    private class Post extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            return executePost(url, postData);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(saveCookie){
                if(s.length() > 3){ // 에러코드는 3자리이므로 3자리 이상이면 쿠키 저장
                    saveCookie();
                }
            }
            callback.callback(s);
        }
    }

    private String executePost(String requestURL, HashMap<String, String> postData){
        URL url;
        String response ="";
        try{
            url = new URL(requestURL);
            conn = (HttpURLConnection)url.openConnection();
            if(useSession){
                if(cookie != null){
                    conn.setRequestProperty("Cookie", cookie);
                }
            }
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            if(postData.size() != 0){
                writer.write(getPostDataString(postData));
            }
            writer.flush();
            writer.close();
            os.close();

            int responseCode=conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response = "";
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        Uri.Builder builder = new Uri.Builder();
        for(Map.Entry<String, String> entry : params.entrySet()){
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        return builder.build().getEncodedQuery();
    }

    public interface Callback {
        void callback(String response);
    }

}

