package jang.worktogether.group;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.Group;
import jang.worktogether.basic.basic_class.MyGroup;
import jang.worktogether.basic.basic_class.User;
import jang.worktogether.chatting.ChattingListActivity;

public class GroupListFragment extends android.support.v4.app.Fragment {

    HttpUtil httpUtil;
    WTapplication wtApplication;
    ListView groupListView;
    FloatingActionButton groupMakeBtn;
    GroupListAdapter groupListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wtApplication = (WTapplication)getActivity().getApplicationContext();

        httpUtil = new HttpUtil(getActivity());
        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(getActivity(), ErrorcodeUtil.errorMessage(response), Toast.LENGTH_SHORT)
                            .show();
                }
                else{
                    try{
                        JSONArray jsonArray = new JSONArray(response);
                        int length = jsonArray.length();
                        HashMap<String, MyGroup> myGroups = new HashMap<>();
                        for(int i=0; i<length; i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            MyGroup myGroup = new MyGroup(jsonObject.getString("g_id"),
                                    jsonObject.getString("g_name"), jsonObject.getString("g_content"),
                                    jsonObject.getString("g_chief"));
                            myGroup.setMemberCount(Integer.parseInt(jsonObject.getString("member_count")));
                            myGroups.put(myGroup.getId(), myGroup);
                        }
                        wtApplication.getMyself().setGroups(myGroups);
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                    groupListAdapter.setMyGroups(wtApplication.getMyself().getGroups());

                }
            }
        });
        httpUtil.setUrl("group_list.php")
                .setUseSession(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grouplist, container, false);
        if(wtApplication == null){
            wtApplication = (WTapplication)getActivity().getApplicationContext();
        }
        groupListView = (ListView)v.findViewById(R.id.group_list_view);
        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyGroup selectedGroup = (MyGroup)groupListAdapter.getItem(position);
                wtApplication.setCurrentGroup(selectedGroup);
                HttpUtil userHttpUtil = new HttpUtil(getActivity());
                userHttpUtil.setCallback(new HttpUtil.Callback() {
                    @Override
                    public void callback(String response) {
                        if(response.length() == 3){
                            Log.i("chat", ErrorcodeUtil.errorMessage(response));
                        }
                        else if(response.length() == 2){
                            //결과 아무것도 없을 때
                            Log.i("chat", "없음");
                        }
                        else{ //결과 처리
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                HashMap<String, User> users = new HashMap<>();
                                for(int i=0; i<jsonArray.length(); i++){
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    User user = new User(jsonObject.getString("u_id"),
                                            jsonObject.getString("u_name"),
                                            jsonObject.getString("u_status"),
                                            jsonObject.getString("u_profile"),
                                            User.Relation.NoRelation);
                                    users.put(user.getId(), user);
                                }
                                wtApplication.getCurrentGroup().setGroupUsers(users);
                                startActivity(new Intent(getActivity(), ChattingListActivity.class));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                userHttpUtil.setUrl("group_users.php")
                        .setData("g_id", selectedGroup.getId())
                        .setUseSession(true)
                        .postData();
            }
        });
        groupListAdapter = new GroupListAdapter(wtApplication.getMyself().getGroups());
        groupListView.setAdapter(groupListAdapter);

        groupMakeBtn = (FloatingActionButton)v.findViewById(R.id.group_make_button);
        groupMakeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MakeGroupActivity.class));
            }
        });
        groupListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch(scrollState){
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING :{
                        groupMakeBtn.setVisibility(View.GONE);
                        break;
                    }
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
                        groupMakeBtn.setVisibility(View.GONE);
                        break;
                    }
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE : {
                        groupMakeBtn.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        httpUtil.postData();
        if(groupListAdapter != null){
            groupListAdapter.setMyGroups(wtApplication.getMyself().getGroups());
        }
    }

    public void groupListChanged(){
        if(groupListAdapter != null){
            groupListAdapter.notifyDataSetChanged();
        }
    }
    private class GroupListAdapter extends BaseAdapter {

        ArrayList<MyGroup> myGroups;
        LayoutInflater layoutInflater;

        public GroupListAdapter(HashMap<String, MyGroup> myGroups){
            this.myGroups = new ArrayList<>();
            for(String id : myGroups.keySet()) {
                this.myGroups.add(myGroups.get(id));
            }
            layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setMyGroups(HashMap<String, MyGroup> myGroups) {
            this.myGroups.clear();
            for(String id : myGroups.keySet()) {
                this.myGroups.add(myGroups.get(id));
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return myGroups.size();
        }

        @Override
        public Object getItem(int position) {
            return myGroups.get(position);
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
                v = layoutInflater.inflate(R.layout.listview_group, null);
                viewHolder = new ViewHolder();
                viewHolder.groupName_tv = (TextView)v.findViewById(R.id.group_name_tv);
                viewHolder.groupContent_tv = (TextView)v.findViewById(R.id.group_content_tv);
                viewHolder.memberCount_tv = (TextView)v.findViewById(R.id.member_count_tv);
                v.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder)v.getTag();
            }

            viewHolder.groupName_tv.setText(myGroups.get(position).getName());
            viewHolder.groupContent_tv.setText(myGroups.get(position).getContent());
            viewHolder.memberCount_tv.setText(Integer.toString(myGroups.get(position).getMemberCount()));

            return v;
        }
    }

    private class ViewHolder {
        public TextView groupName_tv;
        public TextView groupContent_tv;
        public TextView memberCount_tv;
    }

}
