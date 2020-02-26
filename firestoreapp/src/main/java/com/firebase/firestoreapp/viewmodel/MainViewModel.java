package com.firebase.firestoreapp.viewmodel;

import androidx.lifecycle.ViewModel;
import com.firebase.firestoreapp.Filters;

public class MainViewModel extends ViewModel {
    private boolean mIsSigningIn;
    private Filters mFilters;

    public MainViewModel(){
        mIsSigningIn = false;
        mFilters = Filters.getDefault();
    }

    public boolean getIsSignIn(){
        return mIsSigningIn;
    }

    public void setmIsSigningIn(boolean mIsSigningIn){
        this.mIsSigningIn = mIsSigningIn;
    }

    public Filters getFilters(){
        return mFilters;
    }

    public void setFilters(Filters filters){
        this.mFilters = filters;
    }
}
