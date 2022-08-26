package com.sdu.irlab.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

public class BleCallback extends BluetoothGattCallback {

    private static final String TAG = BleCallback.class.getSimpleName();

    /**
     * 连接状态改变回调
     *
     * @param gatt     gatt
     * @param status   gatt连接状态
     * @param newState 新状态
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED://连接成功
                    Log.d(TAG, "连接成功");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED://断开连接
                    Log.e(TAG, "断开连接");
                    break;
                default:
                    break;
            }
        } else {
            Log.e(TAG, "onConnectionStateChange: " + status);
        }
    }
}
