package kr.ac.ajou.hnm.sensortracker.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import kr.ac.ajou.hnm.sensortracker.BaseApplication;
import kr.ac.ajou.hnm.sensortracker.R;
import kr.ac.ajou.hnm.sensortracker.adapter.MonitorScanResultAdapter;
import kr.ac.ajou.hnm.sensortracker.util.ScanExceptionHandler;

public class MonitorConfigureActivity extends AppCompatActivity {

    private RxBleClient mRxBleClient;
    private boolean isScanning;
    private boolean isConnected;

    @BindView(R.id.monitor_scan)
    Button mScanButton;

    @BindView(R.id.scan_results)
    RecyclerView mScanResults;

    @BindView(R.id.monitor_disconnect)
    Button mDisconnectButton;

    private Disposable mScanDisposable;
    private MonitorScanResultAdapter mMonitorScanResultAdapter;
    

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_configure);
        ButterKnife.bind(this);
        mRxBleClient = BaseApplication.getRxBleClient(this);

        configureResultList();

        isScanning = false;
        isConnected = false;

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
                    connectMonitor(); 
                })
                .negativeText(R.string.dialog_connection_negative)
                .show();
    }

    private void connectMonitor() {

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
                .doFinally(this::dispose)
                .subscribe(mMonitorScanResultAdapter::addScanResult, this::onScanFailure);
    }

    private void dispose() {
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

        if (isConnected){
            mDisconnectButton.setActivated(true);
        }
        else {
            mDisconnectButton.setActivated(false);
        }
    }

    private void onScanFailure(Throwable throwable) {
        if (throwable instanceof BleScanException) {
            ScanExceptionHandler.handleException(this, (BleScanException) throwable);
        }
    }

}