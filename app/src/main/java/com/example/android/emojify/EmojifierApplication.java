package com.example.android.emojify;

import android.app.Application;

import timber.log.Timber;

public class EmojifierApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Install a debug tree instance
        Timber.plant(new Timber.DebugTree());
    }
}
