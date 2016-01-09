package com.sin.android.bledebug;

import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

public class BLEUUID {
	public UUID uuid;
	public boolean isService;
	public Object tag;

	@Override
	public String toString() {
		String s = (isService ? "" : " |__") + (uuid != null ? uuid.toString() : "");
		if (tag instanceof BluetoothGattCharacteristic) {
			BluetoothGattCharacteristic c = (BluetoothGattCharacteristic) tag;
			int p = c.getProperties();
			StringBuffer sb = new StringBuffer();
			int[] ms = new int[] { BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PROPERTY_NOTIFY };
			String[] ss = new String[] { "R", "W", "N" };
			for (int i = 0; i < ms.length; ++i) {
				if ((p & ms[i]) > 0) {
					if (sb.length() > 0)
						sb.append(" ");
					sb.append(ss[i]);
				}
			}
			s = s + "[" + sb.toString() + "]";
		}
		return s;
	}

	public BLEUUID(UUID uuid, boolean isService, Object tag) {
		super();
		this.uuid = uuid;
		this.isService = isService;
		this.tag = tag;
	}
}
