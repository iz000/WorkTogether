package jang.worktogether.group;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.basic.basic_class.User;

import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class SelectFriendListAdapter extends BaseAdapter {

    ArrayList<User> friends;
    LayoutInflater layoutInflater;
    Context context;

    public SelectFriendListAdapter(ArrayList<User> friends, Context context){
        this.friends = friends;
        this.context = context;
        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            viewHolder = new ViewHolder();
            v = layoutInflater.inflate(R.layout.listview_select_friend, null);
            viewHolder.selected = (CheckBox)v.findViewById(R.id.select_checkbox);
            viewHolder.profileImageView = (CircleImageView)v.findViewById(R.id.select_profile_image);
            viewHolder.nameTv = (TextView)v.findViewById(R.id.select_name);
            viewHolder.statusTv = (TextView)v.findViewById(R.id.select_status);
            v.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)v.getTag();
        }
        if(friends.get(position).getProfile().length() != 0){
            Glide.with(context).load(thumbnailPath+friends.get(position).getId()
                    +"/thumb/"+friends.get(position).getProfile())
                    .error(ContextCompat.getDrawable(context, R.drawable.user))
                    .into(viewHolder.profileImageView);
        }
        else{
            viewHolder.profileImageView
                    .setImageDrawable(ContextCompat.getDrawable(context, R.drawable.user));
        }
        viewHolder.selected.setFocusable(false);
        viewHolder.selected.setClickable(false);
        viewHolder.nameTv.setText(friends.get(position).getName());
        viewHolder.statusTv.setText(friends.get(position).getStatus());
        return v;
    }

    private class ViewHolder{
        private CircleImageView profileImageView;
        private CheckBox selected;
        private TextView nameTv;
        private TextView statusTv;
    }
}