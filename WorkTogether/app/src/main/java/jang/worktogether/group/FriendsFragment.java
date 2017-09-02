package jang.worktogether.group;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.UserProfileActivity;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.User;

import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class FriendsFragment extends android.support.v4.app.Fragment {

    WTapplication wtApplication;
    EditText friend_search_et;
    ListView friendListView;
    FriendListAdapter friendListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wtApplication = (WTapplication)getActivity().getApplicationContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(friendListAdapter != null){
            friendListAdapter.notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friendlist, container, false);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friend_search_et.clearFocus();
            }
        }); // 프래그먼트의 다른 부분을 누르면 에디트텍스트 포커스 제거
        KeyboardUtil.setupHideKeyBoard(v, getActivity());
        friend_search_et = (EditText)v.findViewById(R.id.search_friend);
        friend_search_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                friendListAdapter.filter(friend_search_et.getText().toString());
            }
        }); // 친구 검색을 위한 필터사용 부분

        friendListView = (ListView)v.findViewById(R.id.friend_list_view);
        friendListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                intent.putExtra("type", "friend");
                intent.putExtra("id", ((User)friendListAdapter.getItem(position)).getId());
                startActivity(intent);
            }
        });
        friendListAdapter = new FriendListAdapter(wtApplication.getMyself().getFriends(), getActivity());
        friendListView.setAdapter(friendListAdapter);
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        friend_search_et.setText("");
    }

    public void friendListLoaded(){
        if(friendListAdapter != null){
            friendListAdapter.setFriends(wtApplication.getMyself().getFriends());
        }
    }

    private class FriendListAdapter extends BaseAdapter{

        ArrayList<User> friends;
        ArrayList<User> reserve;
        LayoutInflater layoutInflater;

        private FriendListAdapter(HashMap<String, User> friends, Context context){
            this.friends = new ArrayList<>();
            this.reserve = new ArrayList<>();
            for(String id : friends.keySet()){
                this.friends.add(friends.get(id));
                this.reserve.add(friends.get(id));
            }
            layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private void setFriends(HashMap<String, User> friends) {
            this.friends.clear();
            this.reserve.clear();
            for(String id : friends.keySet()){
                this.friends.add(friends.get(id));
                this.reserve.add(friends.get(id));
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return friends.size();
        }

        @Override
        public Object getItem(int position) {
            return friends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder viewHolder;
            if(v == null){
                v = layoutInflater.inflate(R.layout.listview_friend, null);
                viewHolder = new ViewHolder();
                viewHolder.profile_image_view = (CircleImageView)v.findViewById(R.id.user_profile_image);
                viewHolder.nameTv = (TextView)v.findViewById(R.id.user_name);
                viewHolder.statusTv = (TextView)v.findViewById(R.id.user_status);
                v.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder)v.getTag();
            }
            if(friends.get(position).getProfile().length() != 0){
                Glide.with(getActivity()).load(thumbnailPath+friends.get(position).getId()
                        +"/thumb/"+friends.get(position).getProfile())
                        .error(ContextCompat.getDrawable(getActivity(), R.drawable.user))
                        .into(viewHolder.profile_image_view);
            }
            else{
                viewHolder.profile_image_view
                        .setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.user));
            }
            viewHolder.nameTv.setText(friends.get(position).getName());
            viewHolder.statusTv.setText(friends.get(position).getStatus());
            return v;
        }

        private void filter(String charText){
            charText = charText.toLowerCase(Locale.getDefault());
            friends.clear();
            if (charText.length() == 0) {
                friends.addAll(reserve);
            }
            else{
                for (User user : reserve){
                    if (user.getName().toLowerCase(Locale.getDefault()).contains(charText)){
                        friends.add(user);
                    }
                }
            }
            notifyDataSetChanged();
        }

    }

    private class ViewHolder {
        private CircleImageView profile_image_view;
        private TextView nameTv;
        private TextView statusTv;
    }

}
