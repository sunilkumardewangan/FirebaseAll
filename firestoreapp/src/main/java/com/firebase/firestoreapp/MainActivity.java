package com.firebase.firestoreapp;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.firestoreapp.adapter.RestaurantAdapter;
import com.firebase.firestoreapp.model.Restaurant;
import com.firebase.firestoreapp.viewmodel.MainViewModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,RestaurantAdapter.OnRestaurantSelectedListener,FilterDialogFragment.FilterListner {

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int LIMIT = 50;
    private Toolbar mToolbar;
    private TextView mCurrentSearchView;
    private TextView mCurrentSortByView;
    private RecyclerView mRestaurantsRecycler;
    private ViewGroup mEmptyView;
    private FirebaseFirestore mFirestore;
    private Query mQuery;
    private FilterDialogFragment mFilterDialog;
    private RestaurantAdapter mAdapter;
    private MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        mCurrentSearchView = findViewById(R.id.textCurrentSearch);
        mCurrentSortByView = findViewById(R.id.textCurrentSortBy);
        mRestaurantsRecycler = findViewById(R.id.recyclerRestaurants);
        mEmptyView = findViewById(R.id.viewEmpty);
        setSupportActionBar(mToolbar);

        findViewById(R.id.filterBar).setOnClickListener(this);
        findViewById(R.id.buttonClearFilter).setOnClickListener(this);

        //initialise viewmodel
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        //enable firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        //get firebase instance
        mFirestore = FirebaseFirestore.getInstance();

        //get ${LIMIT} restaurants
        mQuery = mFirestore.collection("restaurants").orderBy("avgRating", Query.Direction.DESCENDING)
                .limit(LIMIT);

        //set recyclerview

        mAdapter = new RestaurantAdapter(mQuery,this){

            @Override
            protected void onDataChanged() {
                //show/hide content if the query return empty
               if(getItemCount() == 0){
                   mRestaurantsRecycler.setVisibility(View.GONE);
                   mEmptyView.setVisibility(View.VISIBLE);
               }else {
                   mRestaurantsRecycler.setVisibility(View.VISIBLE);
                   mEmptyView.setVisibility(View.GONE);
               }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                //show a snackbar error
                Snackbar.make(findViewById(R.id.root_layout),"Something went wrong",Snackbar.LENGTH_SHORT).show();
            }
        };

        mRestaurantsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRestaurantsRecycler.setAdapter(mAdapter);

        // Filter Dialog
        mFilterDialog = new FilterDialogFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn();
            return;
        }

        // Apply filters
        onFilter(mViewModel.getFilters());

        // Start listening for Firestore updates
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }

    private void startSignIn() {
        //Sign In with firebaseUI
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder().setLogo(R.drawable.common_google_signin_btn_icon_dark)
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(true)
                .build();

        startActivityForResult(intent,RC_SIGN_IN);
        mViewModel.setmIsSigningIn(true);
    }

    private boolean shouldStartSignIn() {
        return (!mViewModel.getIsSignIn() && FirebaseAuth.getInstance().getCurrentUser() ==  null);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.filterBar:
                onFilterClicked();
                break;
            case R.id.buttonClearFilter:
                onClearFilterClicked();
                break;
        }
    }

    private void onFilterClicked() {
        // Show the dialog containing filter options
        mFilterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
    }

    public void onClearFilterClicked() {
        mFilterDialog.resetFilters();

        onFilter(Filters.getDefault());
    }

    @Override
    public void onRestaurantSelected(DocumentSnapshot restaurant) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            mViewModel.setmIsSigningIn(false);

            if(resultCode != RESULT_OK){
                if(response ==  null){
                    //user press the back button
                    finish();
                }else if(response.getError() != null
                && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK){
                    showSignInErroDialog(R.string.message_no_network);
                }else {
                    showSignInErroDialog(R.string.message_unknown);
                }
            }
        }
    }



    private void showSignInErroDialog(@StringRes int message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_sign_in_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.option_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startSignIn();
                    }
                }).setNegativeButton(R.string.option_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create();
        alertDialog.show();
    }

    @Override
    public void onFilter(Filters filters) {
        //construct basic query
        Query query = mFirestore.collection("restaurants");

        //category
        if(filters.hasCategory()){
            query = query.whereEqualTo(Restaurant.FIELD_CATEGORY,filters.getCategory());
        }

        //city
        if(filters.hasCity()){
            query = query.whereEqualTo(Restaurant.FIELD_CITY, filters.getCity());
        }

        //price
        if(filters.hasPrice()){
            query = query.whereEqualTo(Restaurant.FIELD_PRICE, filters.getPrice());
        }

        //sort

        if(filters.hasSortBy()){
            query = query.orderBy(filters.getSortBy(),filters.getSortDirection());
        }

        //limit item
        query = query.limit(LIMIT);

        // Update the query
        mAdapter.setQuery(query);

        // Set header
        mCurrentSearchView.setText(HtmlCompat.fromHtml(filters.getSearchDescription(this),
                HtmlCompat.FROM_HTML_MODE_LEGACY));
        mCurrentSortByView.setText(filters.getOrderDescription(this));

        // Save filters
        mViewModel.setFilters(filters);

    }
}
