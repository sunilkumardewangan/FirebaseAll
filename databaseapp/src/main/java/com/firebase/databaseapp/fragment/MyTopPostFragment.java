package com.firebase.databaseapp.fragment;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class MyTopPostFragment extends PostListFragment {

    public MyTopPostFragment() {
        // Required empty public constructor
    }



    @Override
    public Query getQuery(DatabaseReference databaseReference) {

        String myUserId = getUid();
        Query myTopPostQuery = databaseReference.child("user-post").child(myUserId).orderByChild("startCount");
        return myTopPostQuery;
    }

}
