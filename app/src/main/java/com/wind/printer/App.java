package com.wind.printer;

import android.app.Application;

/**
 * Created by wind on 2018/6/11.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PrinterHelper.getInstance(this);
    }
}
