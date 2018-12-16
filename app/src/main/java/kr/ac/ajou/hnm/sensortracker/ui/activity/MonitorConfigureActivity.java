package kr.ac.ajou.hnm.sensortracker.ui.activity;

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.Arrays;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import kr.ac.ajou.hnm.sensortracker.BaseApplication;
import kr.ac.ajou.hnm.sensortracker.R;
import kr.ac.ajou.hnm.sensortracker.adapter.MonitorScanResultAdapter;
import kr.ac.ajou.hnm.sensortracker.service.MonitorService;
import kr.ac.ajou.hnm.sensortracker.util.ScanExceptionHandler;

import static com.trello.rxlifecycle2.android.ActivityEvent.DESTROY;
import static com.trello.rxlifecycle2.android.ActivityEvent.PAUSE;

public class MonitorConfigureActivity extends RxAppCompatActivity {

    private RxBleClient mRxBleClient;
    private MonitorService mService;
    private boolean mBound  =false;

    private boolean isScanning;

    @BindView(R.id.monitor_scan)
    Button mScanButton;

    @BindView(R.id.scan_results)
    RecyclerView mScanResults;

    @BindView(R.id.monitor_disconnect)
    Button mDisconnectButton;

    @BindView(R.id.monitor_connection_status)
    TextView mMonotiorConnecitonStatus;

    private Disposable mScanDisposable;
    private Disposable mConnectionDisposable;

    private MonitorScanResultAdapter mMonitorScanResultAdapter;
    private RxBleDevice mRxBleDevice;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent  = new Intent(this, MonitorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MonitorService.LocalBinder binder = (MonitorService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_configure);
        ButterKnife.bind(this);
        mRxBleClient = BaseApplication.getRxBleClient(this);
        configureResultList();

        isScanning = false;

        if(mService != null && mService.monitorMAC != null){
            mRxBleDevice = mRxBleClient.getBleDevice(mService.monitorMAC);
            final Disposable disposable = mRxBleDevice.observeConnectionStateChanges()
                    .compose(bindUntilEvent(DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onConnectionStateChange); 
        }

        updateUIState();
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
        if (rxBleConnectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED){
            updateUIState();
        }
        updateUIState();

    }

    private void configureResultList() {
        mScanResults.setHasFixedSize(true);
        mScanResults.setItemAnimator(null);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        mScanResults.setLayoutManager(recyclerLayoutManager);
        mMonitorScanResultAdapter = new MonitorScanResultAdapter();
        mScanResults.setAdapter(mMonitorScanResultAdapter);
        mMonitorScanResultAdapter.setOnAdapterItemClickListener(view -> {
            final int childAdapterPosition = mScanResults.getChildAdapterPosition(view);
            final ScanResult itemAtPosition = mMonitorScanResultAdapter.getItemAtPosition(childAdapterPosition);
            onAdapterItemClick(itemAtPosition);
        });
    }

    @OnClick(R.id.monitor_scan)
    void onclick_monitor_scan () {
        if (isScanning){
            isScanning = false;
            mScanDisposable.dispose();
            updateUIState();
        }
        else {
            isScanning = true;
            updateUIState();
            scanBleDevices();
        }
    }

    @OnClick(R.id.monitor_disconnect)
    void onclick_monitor_disconnect(){
        Log.d("credtiger96", "Onclieck : monitor disconnect");
        disposeConnection();
        mService.stopListening();
    }


    void onAdapterItemClick(ScanResult scanResults) {
        final String macAddress = scanResults.getBleDevice().getMacAddress();
       // final Intent intent = new Intent(this, DeviceActivity.class);
       // intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, macAddress);
       // startActivity(intent);
        Log.d("credtiger96", macAddress);
        new MaterialDialog.Builder(this)
                .title(scanResults.getBleDevice().getName())
                .content(String.format(getResources().getString(R.string.dialog_connection_with), scanResults.getBleDevice().getName()))
                .positiveText(R.string.dialog_connection_positive)
                .onPositive((dialog, which) -> {
                    if (mConnectionDisposable != null && !mConnectionDisposable.isDisposed()){
                        mService.stopListening();
                        mConnectionDisposable.dispose();
                    }
                    mRxBleDevice = scanResults.getBleDevice();
                    connectMonitor(scanResults);
                })
                .negativeText(R.string.dialog_connection_negative)
                .show();
    }

    private void connectMonitor(ScanResult scanResult) {
        mConnectionDisposable = scanResult.getBleDevice().establishConnection(false)
                .flatMapSingle(RxBleConnection::discoverServices) // Disconnect automatically after discovery
                .compose(bindUntilEvent(PAUSE))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::updateUIState)
                .subscribe(this::onServiceDiscovered, this::onConnectionFailure);
    }

    private void onServiceDiscovered(RxBleDeviceServices rxBleDeviceServices) {
        boolean isValid = false;
        for (BluetoothGattService service : rxBleDeviceServices.getBluetoothGattServices()){
            if(service.getUuid().toString().equals(getResources().getString(R.string.uuid_network_node))){
                isValid = true;
            }
        }
        if (isValid){
            Snackbar.make(findViewById(android.R.id.content),
                    mRxBleDevice.getName() +
                            " : " + getString(R.string.monitor_connected),Snackbar.LENGTH_SHORT).show();
            mService.monitorMAC = mRxBleDevice.getMacAddress();
            mConnectionDisposable.dispose();
            mService.startListening();

            updateUIState();
        }
        else {
            Snackbar.make(findViewById(android.R.id.content),
                    mRxBleDevice.getName() +
                            " : " + getString(R.string.monitor_invalid), Snackbar.LENGTH_LONG).show();
            mService.monitorMAC = null;
            updateUIState();
        }
    }

    private void onConnectionFailure(Throwable throwable) {
        Snackbar.make(findViewById(android.R.id.content), "Connection error: " + throwable, Snackbar.LENGTH_LONG).show();
    }

    private void disposeConnection() {
        if (mConnectionDisposable != null)
            mConnectionDisposable.dispose();
        updateUIState();
    }

    private void scanBleDevices() {
        mScanDisposable = mRxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build(),
                new ScanFilter.Builder()
                .build()
        )
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::disposeScanResult)
                .subscribe(mMonitorScanResultAdapter::addScanResult, this::onScanFailure);
    }

    private void disposeScanResult() {
        mScanDisposable = null;
        //mMonitorScanResultAdapter.clearScanResults();
        updateUIState();
    }

    private void updateUIState() {
        if (isScanning){
            mScanButton.setText(R.string.monitor_stop_scan);
        }
        else {
            mScanButton.setText(R.string.monitor_start_scan);
        }

        if (isConnected()){
            mDisconnectButton.setActivated(true);
            mMonotiorConnecitonStatus.setText(R.string.monitor_status_disconnected);

        }
        else {
            mDisconnectButton.setActivated(false);
            if (mRxBleDevice != null)
                mMonotiorConnecitonStatus.setText(
                        String.format(getString(R.string.monitor_status_connected),
                                mRxBleDevice.getName()));
        }
    }

    private boolean isConnected() {
        if (mRxBleDevice == null) return false;
        return mRxBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;

    }

    private void onScanFailure(Throwable throwable) {
        if (throwable instanceof BleScanException) {
            ScanExceptionHandler.handleException(this, (BleScanException) throwable);
        }
    }

}