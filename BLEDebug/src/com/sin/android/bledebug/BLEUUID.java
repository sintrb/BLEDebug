package com.sin.android.bledebug;

import java.util.Locale;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

public class BLEUUID {
	public UUID uuid;
	public Object tag;
	public byte[] value;
	public long lasttime = 0;

	@Override
	public String toString() {
		String s = (tag instanceof BluetoothGattService ? "" : (tag instanceof BluetoothGattCharacteristic ? " |_" : "  _|_")) + (uuid != null ? uuid.toString() : "");
		int p = 0;
		StringBuffer sb = new StringBuffer();
		if (tag instanceof BluetoothGattCharacteristic) {
			BluetoothGattCharacteristic c = (BluetoothGattCharacteristic) tag;
			p = c.getProperties();
			sb.append(String.format(Locale.getDefault(), "0x%02x", p));
			int[] ms = new int[] { BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, BluetoothGattCharacteristic.PROPERTY_BROADCAST, BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PROPERTY_NOTIFY };
			String[] ss = new String[] { "R", "W", "WNR", "SW", "B", "EP", "I", "N" };
			for (int i = 0; i < ms.length; ++i) {
				if ((p & ms[i]) > 0) {
					if (sb.length() > 0)
						sb.append("|");
					sb.append(ss[i]);
				}
			}
			p = c.getPermissions();
		}

		if (tag instanceof BluetoothGattDescriptor) {
			BluetoothGattDescriptor d = (BluetoothGattDescriptor) tag;
			p = d.getPermissions();
		}
		if (p != 0) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(String.format(Locale.getDefault(), "0x%02x", p));
			int[] ms = new int[] { BluetoothGattDescriptor.PERMISSION_READ, BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED, BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM, BluetoothGattDescriptor.PERMISSION_WRITE, BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED, BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM };
			String[] ss = new String[] { "R", "RE", "RM", "W", "WE", "WM", "S", "SM" };
			for (int i = 0; i < ms.length; ++i) {
				if ((p & ms[i]) > 0) {
					if (sb.length() > 0)
						sb.append("|");
					sb.append(ss[i]);
				}
			}
		}
		if (sb.length() > 0)
			s = s + "[" + sb.toString() + "]";
		return s;
	}

	public BLEUUID(UUID uuid, Object tag) {
		super();
		this.uuid = uuid;
		this.tag = tag;
	}

	public int getLevel() {
		if (tag instanceof BluetoothGattService) {
			return 0;
		} else if (tag instanceof BluetoothGattCharacteristic) {
			return 1;
		} else {// if (tag instanceof BluetoothGattDescriptor) {
			return 2;
		}
	}

	public String getUUID() {
		return this.uuid.toString();
	}

	public String getInfo() {
		int p = 0;
		StringBuffer sb = new StringBuffer();
		if (tag instanceof BluetoothGattCharacteristic) {
			BluetoothGattCharacteristic c = (BluetoothGattCharacteristic) tag;
			p = c.getProperties();
			// sb.append(String.format(Locale.getDefault(), "Pro:0x%02x", p));
			int[] ms = new int[] { BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, BluetoothGattCharacteristic.PROPERTY_BROADCAST, BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, BluetoothGattCharacteristic.PROPERTY_INDICATE, BluetoothGattCharacteristic.PROPERTY_NOTIFY };
			String[] ss = new String[] { "R", "W", "WNR", "SW", "B", "EP", "I", "N" };
			for (int i = 0; i < ms.length; ++i) {
				if ((p & ms[i]) > 0) {
					if (sb.length() > 0)
						sb.append("|");
					sb.append(ss[i]);
				}
			}
			p = c.getPermissions();
		}

		if (tag instanceof BluetoothGattDescriptor) {
			BluetoothGattDescriptor d = (BluetoothGattDescriptor) tag;
			p = d.getPermissions();
		}

		if (sb.length() > 0 && p != 0)
			sb.append(';');
		// sb.append(String.format(Locale.getDefault(), "Per:0x%02x", p));
		int[] ms = new int[] { BluetoothGattDescriptor.PERMISSION_READ, BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED, BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM, BluetoothGattDescriptor.PERMISSION_WRITE, BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED, BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED, BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM };
		String[] ss = new String[] { "R", "RE", "RM", "W", "WE", "WM", "S", "SM" };
		for (int i = 0; i < ms.length; ++i) {
			if ((p & ms[i]) > 0) {
				if (sb.length() > 0)
					sb.append("|");
				sb.append(ss[i]);
			}
		}
		return sb.length() == 0 ? "" : "[" + sb.toString() + "]";
	}

	public String getValue() {
		if (this.value == null)
			return "null";
		else
			return MainActivity.bytes2String(value) + " (" + MainActivity.bytesDecode(value) + ")";
	}
}
