package com.aviadmini.nogamenolife;

import android.app.Application;

import timber.log.Timber;

public class AppOverride
        extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

    }

}
