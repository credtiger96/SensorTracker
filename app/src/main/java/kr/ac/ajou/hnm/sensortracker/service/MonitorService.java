package kr.ac.ajou.hnm.sensortracker.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Singleton;

import io.reactivex.disposables.Disposable;
import kr.ac.ajou.hnm.sensortracker.BaseApplication;
import kr.ac.ajou.hnm.sensortracker.R;
import kr.ac.ajou.hnm.sensortracker.model.Monitor;

import static kr.ac.ajou.hnm.sensortracker.BaseApplication.getRxBleClient;

public class MonitorService extends Service {

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MonitorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MonitorService.this;
        }
    }

    Monitor getMonitors (){
        return null;
    }

    public String monitorMAC;
    private RxBleClient mRxBleClient;
    private RxBleDevice mRxBleDevice;
    private Disposable mDisposable;

    public MonitorService() {
    }

    public void startListening(){
        Log.d("credtiger96","Trying to starting listening with " + monitorMAC);
        if (monitorMAC == null){
            return;
        }

        mRxBleClient = BaseApplication.getRxBleClient(getApplicationContext());

        mRxBleDevice = mRxBleClient.getBleDevice(monitorMAC);
        mDisposable = mRxBleDevice.establishConnection(true)
                .flatMap(rxBleConnection ->
                        rxBleConnection.setupNotification(UUID.fromString(getString(R.string.uuid_location_data))))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            /*
                            String tmp = "";
                            for (byte b : bytes){
                                tmp += String.format("0X%02X ", b);
                            }
                            Log.d("credtiger96", tmp);
*/
                            processData(bytes);
                        },
                        throwable -> {
                            Log.e("credtiger96", throwable.toString());
                            // Handle an error here.
                        }
                );
    }

    private void processData(byte[] data) {
        int count = data[1];
        for (int i = 0; i < count; i++){
            int offset = 2 + i * 7;

            long id = (long)data[offset] & 0xFF;
            id += ((long)data[offset + 1] & 0xFF);

            long distance = (long)data[offset + 2] & 0xFF;
            distance += ((long)data[offset + 3] & 0xFF) << 8;
            distance += ((long)data[offset + 4] & 0xFF) << 16;
            distance += ((long)data[offset + 5] & 0xFF) << 24;

            Log.d("credtiger96", "ID : " + id + " Distance : " + distance);
        }

    }

    public void stopListening(){
        if (mDisposable != null)
            mDisposable.dispose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
