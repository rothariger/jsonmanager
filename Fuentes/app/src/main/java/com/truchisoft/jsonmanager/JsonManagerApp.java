package com.truchisoft.jsonmanager;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Maximiliano.Schmidt on 14/10/2015.
 */
public class JsonManagerApp extends Application {
    private static JsonManagerApp _instance;

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;

        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException (thread, e);
            }
        });
    }

    public static JsonManagerApp getInstance() {
        return _instance;
    }

    public static Context getContext() {
        return _instance.getApplicationContext();
    }

    public void handleUncaughtException (Thread thread, Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically
    }
}
