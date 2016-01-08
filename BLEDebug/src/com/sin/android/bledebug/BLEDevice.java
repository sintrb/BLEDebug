package com.sin.android.bledebug;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

public class BLEDevice {
	public BluetoothDevice device;
	public int rssi;
	public byte[] scanRecord;

	public String getName() {
		return device != null ? device.getName() : "未知";
	}

	public String getAddress() {
		return device != null ? device.getAddress() : "未知";
	}

	public String getUUIDS() {
		if (device != null) {
			ParcelUuid[] uuids = device.getUuids();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; uuids != null && i < uuids.length; ++i) {
				sb.append(uuids[i].getUuid().toString() + " ");
			}
			return sb.toString();
		} else {
			return "未知";
		}
	}

	@Override
	public boolean equals(Object o) {
		BLEDevice bled = (BLEDevice) o;
		return bled == this || (bled != null && (bled.device == this.device || (bled.device != null && this.device != null && bled.device.getAddress().equals(this.device.getAddress()))));
	}
}
