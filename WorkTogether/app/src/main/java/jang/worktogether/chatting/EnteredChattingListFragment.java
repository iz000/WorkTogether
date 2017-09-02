package jang.worktogether.chatting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import jang.worktogether.R;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.ChatRoom;
import jang.worktogether.chatting.SQLIte.DBManager;
import jang.worktogether.chatting.SQLIte.FeedReaderContract;

import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID;
import static jang.worktogether.chatting.SQLIte.FeedReaderContract.FeedEntry.TABLE_NAME;

public class EnteredChattingListFragment extends android.support.v4.app.Fragment {

    ListView listView;
    EnteredAdapter enteredAdapter;
    WTapplication wtApplication;
    DBManager dbManager;
    SQLiteDatabase db;

    BroadcastReceiver broadcastReceiver;
    public static String CHAT_COUNT_PLUS = "chatplus";

    FloatingActionButton chattingMakeBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wtApplication = (WTapplication)getActivity().getApplicationContext();
        dbManager = new DBManager(getActivity(), wtApplication.getMyself().getId());
        db = dbManager.getReadableDatabase();
    }

    @Override
    public void onResume() {
        super.onResume();
        chatCountUpdate();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chatting_list, container, false);
        listView = (ListView)v.findViewById(R.id.chatting_list_view);
        enteredAdapter = new EnteredAdapter();
        enteredAdapter.setChatRooms(wtApplication.getCurrentGroupEnteredChatRooms());
        listView.setAdapter(enteredAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatRoom chatRoom = (ChatRoom)enteredAdapter.getItem(position);
                wtApplication.setCurrentChatRoom(
                        wtApplication.getCurrentGroupEnteredChatRooms().get(chatRoom.getChatRoomID()));
                startActivity(new Intent(getActivity(), ChattingRoomActivity.class));
            }
        });

        chattingMakeBtn = (FloatingActionButton)v.findViewById(R.id.chatting_make_button);
        chattingMakeBtn.setVisibility(View.VISIBLE);
        chattingMakeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MakeChattingActivity.class));
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch(scrollState){
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING :{
                        chattingMakeBtn.setVisibility(View.GONE);
                        break;
                    }
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
                        chattingMakeBtn.setVisibility(View.GONE);
                        break;
                    }
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE : {
                        chattingMakeBtn.setVisibility(View.VISIBLE);
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
    public void onDestroy() {
        super.onDestroy();
        db.close();
        dbManager.close();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void chatCountUpdate(){
        if(db.isOpen()){
            for (String chatID : wtApplication.getCurrentGroupEnteredChatRooms().keySet()) {
                Cursor cursor = db.rawQuery("select chat_content, chat_type, " +
                        "(select count("+ COLUMN_NAME_CHAT_NUM+") " +
                        "from " + TABLE_NAME +
                        " where " + COLUMN_NAME_CHAT_READ +" = 0 " +
                        "and "+ COLUMN_NAME_GROUP_ID + " = " + wtApplication.getCurrentGroup().getId()
                        + " and " + COLUMN_NAME_CHAT_ID + " = " + chatID + ") as count " +
                        "from " + TABLE_NAME +
                        " where " + COLUMN_NAME_GROUP_ID + " = " + wtApplication.getCurrentGroup().getId() +
                        " and " + COLUMN_NAME_CHAT_ID + " = " + chatID +
                        " and " + COLUMN_NAME_CHAT_TYPE + " != 'enter'" +
                        " and " + COLUMN_NAME_CHAT_TYPE + " != 'out' " +
                        "order by " + COLUMN_NAME_CHAT_NUM + " desc limit 1;   ", null);
                while (cursor.moveToNext()) {
                    String type = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CHAT_TYPE));
                    switch (type) {
                        case "text":
                            wtApplication.getCurrentGroupEnteredChatRooms().get(chatID)
                                    .setLastChat(cursor.getString(
                                            cursor.getColumnIndex(COLUMN_NAME_CHAT_CONTENT)));
                            break;
                        case "file":
                            String[] strings = cursor.getString(
                                    cursor.getColumnIndex(COLUMN_NAME_CHAT_CONTENT)).split("\\|", 4);
                            wtApplication.getCurrentGroupEnteredChatRooms().get(chatID)
                                    .setLastChat(strings[3]+"."+strings[2]);
                            break;
                        case "image":
                            wtApplication.getCurrentGroupEnteredChatRooms().get(chatID)
                                    .setLastChat("이미지");
                            break;
                    }
                    wtApplication.getCurrentGroupEnteredChatRooms().get(chatID).
                            setChatCount(cursor.getInt(cursor.getColumnIndex("count")));
                }
            }
        }
        enteredAdapter.notifyDataSetChanged();
    }

    private void registerReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(CHAT_COUNT_PLUS)){
                    enteredAdapter.notifyDataSetChanged();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHAT_COUNT_PLUS);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void dataLoaded(){
        if(enteredAdapter != null){
            enteredAdapter.setChatRooms(wtApplication.getCurrentGroupEnteredChatRooms());
            chatCountUpdate();
            registerReceiver();
        }
    }

    private class EnteredAdapter extends BaseAdapter {

        ArrayList<ChatRoom> chatRooms;
        LayoutInflater layoutInflater;

        EnteredAdapter(){
            this.chatRooms = new ArrayList<>();
            layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        void setChatRooms(HashMap<String, ChatRoom> entered){
            chatRooms.clear();
            for(String id : entered.keySet()){
                chatRooms.add(entered.get(id));
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return chatRooms.size();
        }

        @Override
        public Object getItem(int position) {
            return chatRooms.get(position);
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
                v = layoutInflater.inflate(R.layout.listview_entered, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.enter_topic = (TextView)v.findViewById(R.id.enter_topic);
                viewHolder.enter_member_count = (TextView)v.findViewById(R.id.enter_member_count);
                viewHolder.enter_message = (TextView)v.findViewById(R.id.enter_message);
                viewHolder.enter_message_count = (TextView)v.findViewById(R.id.enter_message_count);
                v.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder)v.getTag();
            }

            viewHolder.enter_topic.setText(chatRooms.get(position).getChatRoomTopic());
            viewHolder.enter_member_count.setText(Integer.toString(chatRooms.get(position).getMemberNum()));
            viewHolder.enter_message.setText(chatRooms.get(position).getLastChat());
            int chatCount = chatRooms.get(position).getChatCount();
            if(chatCount != 0){
                viewHolder.enter_message_count.setVisibility(View.VISIBLE);
                viewHolder.enter_message_count.setText(Integer.toString(chatCount));
            }
            else{
                viewHolder.enter_message_count.setVisibility(View.GONE);
            }
            return v;
        }

        private class ViewHolder {
            TextView enter_topic;
            TextView enter_member_count;
            TextView enter_message;
            TextView enter_message_count;
        }
    }
}
