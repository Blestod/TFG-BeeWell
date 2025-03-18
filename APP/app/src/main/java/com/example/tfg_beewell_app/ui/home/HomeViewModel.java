package com.example.tfg_beewell_app.ui.home;

import android.app.Application; // ✅ Import Application
import androidx.lifecycle.AndroidViewModel; // ✅ Use AndroidViewModel instead of ViewModel
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.tfg_beewell_app.R; // ✅ Import R for accessing string resources

public class HomeViewModel extends AndroidViewModel { // ✅ Change to AndroidViewModel

    private final MutableLiveData<String> mText;

    public HomeViewModel(Application application) {
        super(application);
        mText = new MutableLiveData<>(application.getString(R.string.info));
    }

    public LiveData<String> getText() {
        return mText;
    }
}
