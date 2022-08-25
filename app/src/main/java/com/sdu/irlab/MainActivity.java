package com.sdu.irlab;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.sdu.irlab.adapter.BleDeviceAdapter;
import com.sdu.irlab.devices.BleDevice;
import com.sdu.irlab.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "djnxyxy";

    // 权限请求码
    public static final int REQUEST_PERMISSION_CODE = 9527;

    private Button startScan;

    // 蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    private ActivityResultLauncher<Intent> openBluetoothLauncher;

    private Context mContext;

    // 设备列表
    private final List<BleDevice> mList = new ArrayList<>();

    // 列表适配器
    private BleDeviceAdapter deviceAdapter;

    // 扫描回调
    private ScanCallback scanCallback;

    private ContentLoadingProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();
        mContext = this;
        initLauncher();
        initView();
        requestPermission();
    }

    private void initLauncher() {
        openBluetoothLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // 此处是跳转的result回调方法
            if (result.getResultCode() == RESULT_OK) {
                if (bluetoothAdapter.isEnabled()) {
                    //蓝牙已打开
                    ToastUtil.show(this, "蓝牙已打开");
                } else {
                    ToastUtil.show(this, "请打开蓝牙");
                }
            }
        });
    }

    private void initView() {
        RecyclerView rvDevice = findViewById(R.id.rv_device);
        Button stopScan = findViewById(R.id.btn_stop_scan);
        startScan = findViewById(R.id.btn_start_scan);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);

        startScan.setOnClickListener(this);
        stopScan.setOnClickListener(this);
        //扫描结果回调
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, @NonNull ScanResult result) {
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT);
                addDeviceList(new BleDevice(result.getDevice(), result.getRssi(), result.getDevice().getName()));
            }

            @Override
            public void onScanFailed(int errorCode) {
                throw new RuntimeException("Scan error");
            }
        };
        //列表配置
        deviceAdapter = new BleDeviceAdapter(R.layout.item_device_rv, mList);
        rvDevice.setLayoutManager(new LinearLayoutManager(this));
        //启用动画
        deviceAdapter.setAnimationEnable(true);
        //设置动画方式
        deviceAdapter.setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInRight);
        rvDevice.setAdapter(deviceAdapter);
    }

    // 是否打开蓝牙
    public void openBluetooth() {
        // 获取蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                ToastUtil.show(this, "蓝牙已打开");
            } else {
                openBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
        } else {
            ToastUtil.show(this, "你的设备不支持蓝牙");
        }
    }

    /**
     * 请求权限
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_CODE)
    private void requestPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT};
        // 权限通过之后检查有没有打开蓝牙
        if (EasyPermissions.hasPermissions(this, perms)) {
            openBluetooth();
        } else {
            EasyPermissions.requestPermissions(this, "App需要定位权限", REQUEST_PERMISSION_CODE, perms);
        }
    }

    /**
     * 权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将结果转发给 EasyPermissions
        EasyPermissions.onRequestPermissionsResult(REQUEST_PERMISSION_CODE, permissions, grantResults, this);
    }

    /**
     * 开始扫描设备
     */
    @SuppressLint("NotifyDataSetChanged")
    public void startScanDevice() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        mList.clear();
        deviceAdapter.notifyDataSetChanged();
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(scanCallback);
    }

    /**
     * 停止扫描设备
     */
    public void stopScanDevice() {
        loadingProgressBar.setVisibility(View.INVISIBLE);
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(scanCallback);
    }

    /**
     * 添加到设备列表
     *
     * @param bleDevice 蓝牙设备
     */
    @SuppressLint("NotifyDataSetChanged")
    private void addDeviceList(BleDevice bleDevice) {
        if (!mList.contains(bleDevice)) {
            if (bleDevice.getRealName() != null) {
                mList.add(bleDevice);
            }
        } else {
            //更新设备信号强度值
            for (BleDevice device : mList) {
                device.setRssi(bleDevice.getRssi());
            }
        }
        //刷新列表适配器
        deviceAdapter.notifyDataSetChanged();
    }


    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_start_scan) {
            startScanDevice();
            startScan.setEnabled(false);
        } else if (vid == R.id.btn_stop_scan) {
            stopScanDevice();
            startScan.setEnabled(true);
        }
    }
}