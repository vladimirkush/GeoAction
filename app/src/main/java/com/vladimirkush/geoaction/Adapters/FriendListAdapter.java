package com.vladimirkush.geoaction.Adapters;


import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.login.widget.ProfilePictureView;
import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.DataAccess.DBHelper;

import java.util.ArrayList;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    private final String        LOG_TAG = "LOGTAG";

    private Context             mContext;
    private DBHelper            dbHelper;
    private ArrayList<Friend>   mFriendList;


    public FriendListAdapter(Context mContext, ArrayList<Friend> mFriendList) {
        this.mContext = mContext;
        this.mFriendList = mFriendList;
        dbHelper = new DBHelper(mContext);
    }

    static class ViewHolder extends RecyclerView.ViewHolder   {
        ImageView fbIconView;
        TextView tvName;
        TextView tvIsNear;
        Switch swichTracing;
        ProfilePictureView mProfPic;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
         ViewHolder(View itemView) {
             super(itemView);
             fbIconView = (ImageView) itemView.findViewById(R.id.image_profile_fb);
             tvName = (TextView) itemView.findViewById(R.id.tv_friend_name);
             tvIsNear = (TextView) itemView.findViewById(R.id.tv_isnear);
             swichTracing = (Switch) itemView.findViewById(R.id.switch_tracing);
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // inflate a layout for the view
        View friendsView = inflater.inflate(R.layout.friends_row_item, parent, false);

        return  new ViewHolder(friendsView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Friend friend = mFriendList.get(position);

        holder.fbIconView.setImageBitmap(friend.getUserIcon());
        holder.tvName.setText(friend.getName());
        if(friend.isNear()) {
            holder.tvName.setTextColor(Color.parseColor("green"));
            holder.tvIsNear.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "green");
        }else{
            holder.tvName.setTextColor(Color.parseColor("black"));
            holder.tvIsNear.setVisibility(View.INVISIBLE);
            Log.d(LOG_TAG, "black");
        }
        boolean traced = (friend.getStatus() == Friend.Status.TRACED);
        holder.swichTracing.setChecked(traced);


        holder.swichTracing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int position = holder.getAdapterPosition();
                Friend fr = mFriendList.get(position);
                Log.d(LOG_TAG, "clicked status btn of id "+ fr.getID());
                if(isChecked){
                    fr.setStatus(Friend.Status.TRACED);
                }else{
                    fr.setStatus(Friend.Status.UNTRACED);
                }
                dbHelper.updateFriend(fr);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFriendList.size();
    }
}
