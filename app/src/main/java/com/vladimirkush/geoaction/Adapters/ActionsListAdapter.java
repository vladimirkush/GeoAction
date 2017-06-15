package com.vladimirkush.geoaction.Adapters;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.vladimirkush.geoaction.ActionCreate;
import com.vladimirkush.geoaction.Interfaces.DeleteItemHandler;
import com.vladimirkush.geoaction.Interfaces.SendItemHandler;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.DataAccess.DBHelper;

import java.util.ArrayList;
import java.util.List;


public class ActionsListAdapter extends   RecyclerView.Adapter<ActionsListAdapter.ViewHolder>  {
    private final String        LOG_TAG = "LOGTAG";

    private List<LBAction>      mActionList;
    private Context             mContext;
    private DBHelper            dbHelper;

    private DeleteItemHandler   mDeleteItemHandler;
    private SendItemHandler     mSendItemHandler;


    // ctor
    public ActionsListAdapter(Context mContext, ArrayList<LBAction> mActionList) {
        this.mActionList = mActionList;
        this.mContext = mContext;
        dbHelper = new DBHelper(mContext);       // init dbHelper
    }



    static class ViewHolder extends RecyclerView.ViewHolder   {
         ImageView imageType;
         TextView messageTv;
         TextView titleTv;
         TextView toTv;
         ImageButton statusBtn;

         View v;

        // Create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        ViewHolder(View itemView) {
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
    public ViewHolder onCreateViewHolder (ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // inflate a layout for the view
        View actionView = inflater.inflate(R.layout.main_row_item, parent, false);


        // Return a new holder instance
        return new ViewHolder(actionView);

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
                holder.toTv.setText(sms.getToAsSingleString());
                break;
            case EMAIL:
                LBEmail email = (LBEmail) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_mail);
                holder.messageTv.setText(email.getMessage());
                holder.titleTv.setText(email.getSubject());
                holder.toTv.setText(email.getToAsSingleString());
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
                holder.v.setSelected(true);
                Log.d(LOG_TAG, "long clicked id "+ mActionList.get(holder.getAdapterPosition()).getID());
                PopupMenu popup = new PopupMenu(v.getContext(), holder.toTv, Gravity.CENTER_HORIZONTAL);
                popup.inflate(R.menu.ctx_menu_main);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int position = holder.getAdapterPosition();
                        LBAction action = mActionList.get(position);
                        int menuItemID = item.getItemId();
                        Log.d(LOG_TAG, "Menu item for id: "+action.getID() + " :" + item.getTitle());

                        switch (menuItemID){
                            case R.id.ctx_send_action:
                                if(mSendItemHandler != null) {
                                    mSendItemHandler.sendItem(position, action);
                                }else{
                                    handleSend(position, action);
                                }
                                break;

                            case R.id.ctx_delete_action:
                                if(mDeleteItemHandler != null) {
                                    mDeleteItemHandler.deleteItem(position, action);
                                }else{
                                    handleDelete(position, action);
                                }
                                break;
                        }

                        return true;
                    }
                });

                popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        holder.v.setSelected(false);
                    }
                });
                popup.show();

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

    private void handleDelete(int position, LBAction action){
        Log.d(LOG_TAG, "Handling default delete for id: " + action.getID() + "and pos: "+ position);
    }

    private void handleSend(int position, LBAction action){
        Log.d(LOG_TAG, "Handling default send for id: " + action.getID() + "and pos: "+ position);
    }

    @Override
    public int getItemCount() {
        return mActionList.size();
    }

    private Context getContext() {
        return mContext;
    }

    public DeleteItemHandler getDeleteItemHandler() {
        return mDeleteItemHandler;
    }

    public SendItemHandler getmSendItemHandler() {
        return mSendItemHandler;
    }

    public void setDeleteItemHandler(DeleteItemHandler deleteItemHandler) {
        this.mDeleteItemHandler = deleteItemHandler;
    }

    public void setSendItemHandler(SendItemHandler sendItemHandler) {
        this.mSendItemHandler = sendItemHandler;
    }


}
