package com.firebase.databaseapp.fragment;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class MyPostFragment extends PostListFragment {

    public MyPostFragment() {
        // Required empty public constructor
    }

    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        // All my posts
        return databaseReference.child("user-posts")
                .child(getUid());
    }

}
