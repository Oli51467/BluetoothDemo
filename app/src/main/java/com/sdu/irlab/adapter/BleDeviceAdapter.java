package com.sdu.irlab.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.sdu.irlab.R;
import com.sdu.irlab.devices.BleDevice;

import java.util.List;

public class BleDeviceAdapter extends BaseQuickAdapter<BleDevice, BaseViewHolder> {

    public BleDeviceAdapter(int layoutResId, List<BleDevice> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder holder, BleDevice bleDevice) {
        holder.setText(R.id.tv_device_name, bleDevice.getRealName())
                .setText(R.id.tv_mac_address, bleDevice.getDevice().getAddress())
                .setText(R.id.tv_rssi, bleDevice.getRssi() + " dBm");
    }
}
