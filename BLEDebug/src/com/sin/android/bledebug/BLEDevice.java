package com.sin.android.bledebug;

import android.bluetooth.BluetoothDevice;

public class BLEDevice {
	public BluetoothDevice device;
	public int rssi;
	public byte[] scanRecord;

	public String getName() {
		return device != null ? device.getName() : "未知";
	}
}
