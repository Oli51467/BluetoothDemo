package com.sdu.irlab;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.sdu.irlab.utils.ToastUtil;

import java.util.Objects;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    /**
     * 权限请求码
     */
    public static final int REQUEST_PERMISSION_CODE = 9527;

    // 蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    private ActivityResultLauncher<Intent> openBluetoothLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();
        initLauncher();
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
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        // 权限通过之后检查有没有打开蓝牙
        if (EasyPermissions.hasPermissions(this, perms)) {
            openBluetooth();
        } else {    // 没有权限
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
}