package jang.worktogether.chatting;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import jang.worktogether.R;
import jang.worktogether.basic.basic_class.User;

import static jang.worktogether.group.ProfileFragment.thumbnailPath;

public class ChattingNavigationAdapter extends BaseAdapter {

    ArrayList<User> users;
    LayoutInflater layoutInflater;
    Context context;

    public ChattingNavigationAdapter(Context context, HashMap<String, User> users){
        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        this.users = new ArrayList<>();
        this.users.addAll(users.values());
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
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
            v = layoutInflater.inflate(R.layout.listview_navigation_user_list , parent, false);
            viewHolder = new ViewHolder();
            viewHolder.circleImageView = (CircleImageView)v.findViewById(R.id.navigation_user_profile);
            viewHolder.nameTextView = (TextView)v.findViewById(R.id.navigation_user_name);
            v.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)v.getTag();
        }
        if(users.get(position).getProfile().length() != 0){
            Glide.with(context).load(thumbnailPath+users.get(position).getId()
                    +"/thumb/"+users.get(position).getProfile())
                    .error(ContextCompat.getDrawable(context, R.drawable.userblack))
                    .into(viewHolder.circleImageView);
        }
        else{
            viewHolder.circleImageView
                    .setImageDrawable(ContextCompat.getDrawable(context, R.drawable.user));
        }
        viewHolder.nameTextView.setText(users.get(position).getName());

        return v;
    }

    private class ViewHolder {
        CircleImageView circleImageView;
        TextView nameTextView;
    }
}
