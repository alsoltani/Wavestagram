package io.github.alsoltani.wavestagram;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by alsoltani on 05.11.14.
 */
public class WavestagramApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
