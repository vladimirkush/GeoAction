package com.vladimirkush.geoaction.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vladimirkush.geoaction.ActionCreate;
import com.vladimirkush.geoaction.Interfaces.SuggestionHandler;
import com.vladimirkush.geoaction.Models.LBAction;
import com.vladimirkush.geoaction.Models.LBEmail;
import com.vladimirkush.geoaction.Models.LBReminder;
import com.vladimirkush.geoaction.Models.LBSms;
import com.vladimirkush.geoaction.R;
import com.vladimirkush.geoaction.Utils.Constants;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;
import java.util.List;


public class SuggestionsAdapter extends   RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
    private final String LOG_TAG = "LOGTAG";

    private List<LBAction> mActionList;
    private Context mContext;
    private DBHelper dbHelper;



    private SuggestionHandler mSuggestionHandler;

    // ctor
    public SuggestionsAdapter(Context mContext, ArrayList<LBAction> mActionList) {
        this.mActionList = mActionList;
        this.mContext = mContext;
        dbHelper = new DBHelper(mContext);       // init dbHelper
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageType;
        public TextView messageTv;
        public TextView titleTv;
        public View v;

        public ViewHolder(View itemView) {
            super(itemView);
            v=itemView;
            imageType = (ImageView) itemView.findViewById(R.id.sug_image_type);
            messageTv = (TextView) itemView.findViewById(R.id.sug_tv_message);
            titleTv = (TextView) itemView.findViewById(R.id.sug_tv_title);
        }



    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // inflate a layout for the view
        View actionView = inflater.inflate(R.layout.suggestion_row_item, parent, false);


        // Return a new holder instance
        return new SuggestionsAdapter.ViewHolder(actionView);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        LBAction lbAction = mActionList.get(position);

        switch (lbAction.getActionType()) {
            case REMINDER:
                LBReminder reminder = (LBReminder) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_reminder);
                holder.messageTv.setText(reminder.getMessage());
                holder.titleTv.setText(reminder.getTitle());
                break;
            case SMS:
                LBSms sms = (LBSms) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_sms);
                holder.messageTv.setText(sms.getMessage());
                holder.titleTv.setText("");
                break;
            case EMAIL:
                LBEmail email = (LBEmail) lbAction;
                holder.imageType.setImageResource(R.mipmap.ic_mail);
                holder.messageTv.setText(email.getMessage());
                holder.titleTv.setText(email.getSubject());
                break;
            default:
                Log.d(LOG_TAG, "IS: lbAction received from DB has an illegal type");
                break;
        }

        // set listener
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                LBAction action = mActionList.get(position);
                Log.d(LOG_TAG, "clicked suggestion id "+ action.getID());
                if(mSuggestionHandler != null){
                    mSuggestionHandler.onSuggestionClicked(position, action);
                }else{
                    Log.d(LOG_TAG, "no suggestion handler registered");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mActionList.size();
    }


    public void setSuggestionHandler(SuggestionHandler mSuggestionHandler) {
        this.mSuggestionHandler = mSuggestionHandler;
    }
}
