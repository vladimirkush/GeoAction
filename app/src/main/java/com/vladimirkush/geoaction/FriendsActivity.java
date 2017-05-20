package com.vladimirkush.geoaction;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.vladimirkush.geoaction.Adapters.FriendListAdapter;
import com.vladimirkush.geoaction.Models.Friend;
import com.vladimirkush.geoaction.Utils.DBHelper;

import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity {
    private Toolbar             mToolbar;
    private DBHelper            dbHelper;
    private RecyclerView        rvFriendList;
    private ArrayList<Friend>   mfriendList;
    private FriendListAdapter   friendListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mToolbar= (Toolbar) findViewById(R.id.friends_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Friend tracker");


        dbHelper = new DBHelper(this);
        rvFriendList = (RecyclerView) findViewById(R.id.rvFriendsList);
        mfriendList = dbHelper.getAllFriends();
        friendListAdapter = new FriendListAdapter(this, mfriendList);
        rvFriendList.setAdapter(friendListAdapter);
        rvFriendList.setLayoutManager(new LinearLayoutManager(this));
        // decorate RecyclerView
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvFriendList.addItemDecoration(itemDecoration);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
