package jang.worktogether.chatting;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import jang.worktogether.R;
import jang.worktogether.basic.WTapplication;
import jang.worktogether.basic.basic_class.ChatRoom;

public class NotEnteredChattingListFragment extends android.support.v4.app.Fragment {

    ListView listView;
    NotEnteredAdapter notEnteredAdapter;
    WTapplication wtApplication;
    LocalBroadcastManager broadCaster;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wtApplication = (WTapplication)getActivity().getApplicationContext();
        broadCaster = LocalBroadcastManager.getInstance(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chatting_list, container, false);
        listView = (ListView)v.findViewById(R.id.chatting_list_view);
        notEnteredAdapter = new NotEnteredAdapter();
        notEnteredAdapter.setChatRooms(wtApplication.getCurrentGroupNotEnteredChatRooms());
        listView.setAdapter(notEnteredAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("채팅방에 입장하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ChatRoom chatRoom = (ChatRoom)notEnteredAdapter.getItem(position);
                                wtApplication.setCurrentChatRoom(
                                        wtApplication.getCurrentGroupNotEnteredChatRooms().get(chatRoom.getChatRoomID()));
                                wtApplication.getCurrentGroupNotEnteredChatRooms().remove(chatRoom.getChatRoomID());
                                wtApplication.getCurrentGroupEnteredChatRooms().put(chatRoom.getChatRoomID(), chatRoom);
                                Packet packet = new Packet(SocketService.CHATTING_ENTER);
                                packet.addData(wtApplication.getCurrentGroup().getId(),
                                        wtApplication.getCurrentChatRoom().getChatRoomID());
                                Intent intent = new Intent(SocketService.CHATTING_ENTER);
                                intent.putExtra("Packet", packet.toByteArray());
                                broadCaster.sendBroadcast(intent);
                                startActivity(new Intent(getActivity(), ChattingRoomActivity.class));
                            }
                        })
                        .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        });
        return v;
    }

    public void dataLoaded(){
        if(notEnteredAdapter != null){
            notEnteredAdapter.setChatRooms(wtApplication.getCurrentGroupNotEnteredChatRooms());
        }
    }

    private class NotEnteredAdapter extends BaseAdapter {

        ArrayList<ChatRoom> chatRooms;
        LayoutInflater layoutInflater;

        public NotEnteredAdapter(){
            chatRooms = new ArrayList<>();
            layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setChatRooms(HashMap<String, ChatRoom> notEntered){
            chatRooms.clear();
            for(String id : notEntered.keySet()){
                chatRooms.add(notEntered.get(id));
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
                v = layoutInflater.inflate(R.layout.listview_not_entered, null);
                viewHolder = new ViewHolder();
                viewHolder.not_topic = (TextView)v.findViewById(R.id.not_topic);
                viewHolder.not_member_count = (TextView)v.findViewById(R.id.not_member_count);
                v.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder)v.getTag();
            }

            viewHolder.not_topic.setText(chatRooms.get(position).getChatRoomTopic());
            viewHolder.not_member_count.setText(String.format("%d", chatRooms.get(position).getMemberNum()));

            return v;
        }

        private class ViewHolder {
            public TextView not_topic;
            public TextView not_member_count;
        }
    }
}
