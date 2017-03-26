package com.vladimirkush.geoaction.Adapters;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.vladimirkush.geoaction.ActionCreate;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class ActionsListAdapter extends   RecyclerView.Adapter<ActionsListAdapter.ViewHolder>{
    private final String LOG_TAG = "LOGTAG";

    private List<LBAction> mActionList;
    private Context mContext;
    private DBHelper dbHelper;

    // ctor
    public ActionsListAdapter(Context mContext, ArrayList<LBAction> mActionList) {
        this.mActionList = mActionList;
        this.mContext = mContext;
        dbHelper = new DBHelper(mContext);       // init dbHelper
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageType;
        public TextView messageTv;
        public TextView titleTv;
        public TextView toTv;
        public ImageButton statusBtn;

        public View v;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            super(itemView);
            v=itemView;
            imageType = (ImageView) itemView.findViewById(R.id.image_type);
            messageTv = (TextView) itemView.findViewById(R.id.tv_message);
            titleTv = (TextView) itemView.findViewById(R.id.tv_title);
            toTv = (TextView) itemView.findViewById(R.id.tv_to);
            statusBtn = (ImageButton)itemView.findViewById(R.id.imgb_pause_activate);


        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // inflate a layout for the view
        View actionView = inflater.inflate(R.layout.main_row_item, parent, false);


        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(actionView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        LBAction lbAction = mActionList.get(position);

        switch (lbAction.getActionType()) {
            case REMINDER:
                LBReminder reminder = (LBReminder) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_reminder);
                holder.messageTv.setText(reminder.getMessage());
                holder.titleTv.setText(reminder.getTitle());
                holder.toTv.setText("");
                break;
            case SMS:
                LBSms sms = (LBSms) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_sms);
                holder.messageTv.setText(sms.getMessage());
                holder.titleTv.setText("");
                holder.toTv.setText(stringFromArrayListStrings(sms.getTo()));
                break;
            case EMAIL:
                LBEmail email = (LBEmail) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_mail);
                holder.messageTv.setText(email.getMessage());
                holder.titleTv.setText(email.getSubject());
                holder.toTv.setText(stringFromArrayListStrings(email.getTo()));
                break;
            default:
                Log.d(LOG_TAG, "IS: lbAction received from DB has an illegal type");
                break;
        }
        if(lbAction.getStatus()== LBAction.Status.ACTIVE){
            holder.statusBtn.setImageResource(R.drawable.circle_green);
        }else{
            holder.statusBtn.setImageResource(R.drawable.circle_yellow);
        }

        // click listeners
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LBAction action = mActionList.get(holder.getAdapterPosition());
                Log.d(LOG_TAG, "clicked id "+ action.getID());

                Intent intent = new Intent(getContext(), ActionCreate.class);
                intent.putExtra(Constants.EDIT_MODE_KEY, true);
                intent.putExtra(Constants.LBACTION_ID_KEY, action.getID());
                ((Activity)mContext).startActivityForResult(intent, Constants.EDIT_EXISTING_LBACTION_REQUEST);
            }
        });
        holder.v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(LOG_TAG, "long clicked id "+ mActionList.get(holder.getAdapterPosition()).getID());
                return true;
            }
        });
        holder.statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "clicked status btn of id "+ mActionList.get(holder.getAdapterPosition()).getID());
                int position = holder.getAdapterPosition();
                LBAction action = mActionList.get(position);
                LBAction.Status status = action.getStatus();
                if(status == LBAction.Status.ACTIVE){   // pause action
                    action.setStatus(LBAction.Status.PAUSED);
                    dbHelper.updateAction(action);
                    holder.statusBtn.setImageResource(R.drawable.circle_yellow);
                    Log.d(LOG_TAG, "ADAPTER: action with id "+ action.getID()+" is now PAUSED");
                }else{  // unpause action
                    action.setStatus(LBAction.Status.ACTIVE);
                    dbHelper.updateAction(action);
                    holder.statusBtn.setImageResource(R.drawable.circle_green);
                    Log.d(LOG_TAG, "ADAPTER: action with id "+ action.getID()+" is now ACTIVE");

                }
                notifyItemChanged(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mActionList.size();
    }

    private Context getContext() {
        return mContext;
    }

    private String stringFromArrayListStrings(List<String> list){
        StringBuilder listString = new StringBuilder();
        int i;
        for (i =0; i<list.size()-1; i++){
            listString.append(list.get(i)+", ");
        }
        listString.append(list.get(i));
        return listString.toString();
    }

}
