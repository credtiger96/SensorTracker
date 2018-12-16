package kr.ac.ajou.hnm.sensortracker;

import android.app.Application;
import android.content.Context;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.internal.RxBleLog;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;

public class BaseApplication extends Application {

    private RxBleClient rxBleClient;

    /**
     * In practise you will use some kind of dependency injection pattern.
     */
    public static RxBleClient getRxBleClient(Context context) {
        BaseApplication application = (BaseApplication)context.getApplicationContext();
        return application.rxBleClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        rxBleClient = RxBleClient.create(this);
        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }
}