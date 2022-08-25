package com.sdu.irlab;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "djnxyxy";

    // 权限请求码
    public static final int REQUEST_PERMISSION_CODE = 9527;

    private boolean isConnected = false;

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

    private LinearLayout layConnectingLoading;  // 等待连接

    private BluetoothGatt bluetoothGatt;

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
        layConnectingLoading = findViewById(R.id.lay_connecting_loading);

        startScan.setOnClickListener(this);
        stopScan.setOnClickListener(this);
        //扫描结果回调
        scanCallback = new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, @NonNull ScanResult result) {
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
        //item点击事件
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            //连接设备
            connectDevice(mList.get(position));
        });
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
        List<String> neededPermissions = new ArrayList<>();
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
        } else {
            openBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openBluetooth();
            } else {
                ToastUtil.show(this, "您没有开启权限");
            }
        }
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
        disconnectDevice();
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

    /**
     * 连接设备
     *
     * @param bleDevice 蓝牙设备
     */
    @SuppressLint("MissingPermission")
    private void connectDevice(BleDevice bleDevice) {
        // 显示连接等待布局
        layConnectingLoading.setVisibility(View.VISIBLE);

        // 停止扫描
        stopScanDevice();

        // 获取远程设备
        BluetoothDevice device = bleDevice.getDevice();
        // 连接gatt
        bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true;
                    Log.d(TAG, "连接成功");
                    runOnUiThread(() -> {
                        layConnectingLoading.setVisibility(View.GONE);
                        ToastUtil.show(mContext, "连接成功");
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false;
                    Log.d(TAG, "断开连接");
                    runOnUiThread(() -> {
                        layConnectingLoading.setVisibility(View.GONE);
                        ToastUtil.show(mContext, "断开连接");
                    });
                }
            }
        });
    }

    /**
     * 断开设备连接
     */
    @SuppressLint("MissingPermission")
    private void disconnectDevice() {
        if (isConnected && bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_start_scan) {
            startScanDevice();
            startScan.setEnabled(false);
        } else if (vid == R.id.btn_stop_scan) {
            runOnUiThread(() -> {
                mList.clear();
                deviceAdapter.notifyDataSetChanged();
            });
            stopScanDevice();
            startScan.setEnabled(true);
        }
    }


}