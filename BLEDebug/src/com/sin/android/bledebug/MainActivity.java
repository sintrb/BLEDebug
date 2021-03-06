package com.sin.android.bledebug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sin.android.sinlibs.activities.BaseActivity;
import com.sin.android.sinlibs.adapter.SimpleListAdapter;
import com.sin.android.sinlibs.adapter.SimpleViewInitor;
import com.sin.android.sinlibs.base.Callable;
import com.sin.android.sinlibs.tagtemplate.ViewRender;

public class MainActivity extends BaseActivity {
	static String TAG = "BT";

	private List<BLEDevice> devices = new ArrayList<BLEDevice>();
	private SimpleListAdapter deviceAdapter = null;

	private List<BLEUUID> uuids = new ArrayList<BLEUUID>();
	private SimpleListAdapter uuidsAdapter = null;

	private TextView tv_log = null;
	int logct = 0;
	private ViewRender render = new ViewRender();

	BLEDevice curBleDevice = null;
	BLEUUID curUUID = null;

	BluetoothGatt bluetoothGatt = null;

	public static String bytes2String(byte[] bts) {
		if (bts == null)
			return "null";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bts.length; ++i) {
			if (i > 0)
				sb.append(" ");
			sb.append(String.format(Locale.getDefault(), "%02x", bts[i]));
		}
		return sb.toString();
	}

	public static String bytesDecode(byte[] bts) {
		return bts != null ? new String(bts) : "null";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_log = (TextView) findViewById(R.id.tv_log);
		tv_log.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				logct = 0;
				tv_log.setText("");
				return false;
			}
		});

		deviceAdapter = new SimpleListAdapter(this, devices, new SimpleViewInitor() {
			@Override
			public View initView(Context context, int position, View convertView, ViewGroup parent, Object data) {
				if (convertView == null) {
					convertView = LinearLayout.inflate(MainActivity.this, R.layout.item_device, null);
				}
				if (data.equals(curBleDevice)) {
					convertView.setBackgroundColor(0x80808080);
				} else {
					convertView.setBackgroundColor(0x00000000);
				}
				render.renderView(convertView, data);
				return convertView;
			}
		});
		final float scale = this.getResources().getDisplayMetrics().density;
		uuidsAdapter = new SimpleListAdapter(this, uuids, new SimpleViewInitor() {
			@Override
			public View initView(Context context, int position, View convertView, ViewGroup parent, Object data) {
				if (convertView == null) {
					convertView = LinearLayout.inflate(MainActivity.this, R.layout.item_node, null);
				}
				if (data == curUUID) {
					convertView.setBackgroundColor(0x80808080);
				} else {
					convertView.setBackgroundColor(0x00000000);
				}
				BLEUUID u = (BLEUUID) data;
				int lv = u.getLevel();
				convertView.setPadding((int) (lv * 15 * scale), 0, 0, 0);
				((TextView) convertView.findViewById(R.id.tv_value)).setTextColor((System.currentTimeMillis() - u.lasttime) < 2000 ? Color.RED : Color.BLACK);
				convertView.findViewById(R.id.tv_value).setVisibility(u.value == null ? View.GONE : View.VISIBLE);
				convertView.findViewById(R.id.v_left).setVisibility(lv == 0 ? View.GONE : View.VISIBLE);

				render.renderView(convertView, data);
				return convertView;
			}
		});
		((ListView) findViewById(R.id.lv_devices)).setAdapter(deviceAdapter);
		((ListView) findViewById(R.id.lv_devices)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos, long arg3) {
				if (pos < devices.size()) {
					curBleDevice = devices.get(pos);
					uuids.clear();

					deviceAdapter.notifyDataSetChanged();
					uuidsAdapter.notifyDataSetChanged();

					connectDevice();
				}
			}
		});
		((ListView) findViewById(R.id.lv_uuids)).setAdapter(uuidsAdapter);
		((ListView) findViewById(R.id.lv_uuids)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos, long arg3) {
				if (pos < uuids.size()) {
					curUUID = uuids.get(pos);
					uuidsAdapter.notifyDataSetChanged();
				}
			}
		});

		findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (curUUID == null || bluetoothGatt == null) {
					AddLog("Must select a Characteristic!");
					return;
				}
				if (curUUID.tag instanceof BluetoothGattService) {
					AddLog("Service can't write or read");
					return;
				}
				String txt = ((EditText) findViewById(R.id.et_send)).getText().toString();// "AA 01";
				byte[] buf = new byte[1024];
				String[] ss = txt.split(" ");
				int bufix = 0;
				for (String s : ss) {
					if (s.length() > 0) {
						try {
							byte b = (byte) Integer.parseInt(s, 16);
							buf[bufix] = b;
							++bufix;
						} catch (Exception e) {
							e.printStackTrace();
							AddLog("%s not hex", s);
							return;
						}
					}
				}
				if (bufix > 0) {
					byte[] bts = new byte[bufix];
					System.arraycopy(buf, 0, bts, 0, bufix);
					boolean ok = false;
					if (curUUID.tag instanceof BluetoothGattCharacteristic) {
						BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic) curUUID.tag;
						ok = gattCharacteristic.setValue(bts);
						ok = bluetoothGatt.writeCharacteristic(gattCharacteristic) && ok;
						AddLog("Send C %s: %s", ok ? "ok" : "fail", bytes2String(bts));
					} else if (curUUID.tag instanceof BluetoothGattDescriptor) {
						BluetoothGattDescriptor gattDescriptor = (BluetoothGattDescriptor) curUUID.tag;
						ok = gattDescriptor.setValue(bts);
						ok = bluetoothGatt.writeDescriptor(gattDescriptor) && ok;
						AddLog("Send D %s: %s", ok ? "ok" : "fail", bytes2String(bts));
					}
					// if (!ok)
					// bluetoothGatt.abortReliableWrite();
					// else
					// bluetoothGatt.beginReliableWrite();
				}
			}
		});

		findViewById(R.id.btn_read).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (curUUID == null || bluetoothGatt == null) {
					AddLog("Must select a item!");
					return;
				}
				if (curUUID.tag instanceof BluetoothGattService) {
					AddLog("Service can't write or read");
					return;
				}

				if (curUUID.tag instanceof BluetoothGattCharacteristic) {
					BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic) curUUID.tag;
					bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
					boolean ok = bluetoothGatt.readCharacteristic(gattCharacteristic);
					AddLog("Read Request C %s", ok ? "ok" : "fail");
				} else if (curUUID.tag instanceof BluetoothGattDescriptor) {
					BluetoothGattDescriptor gattDescriptor = (BluetoothGattDescriptor) curUUID.tag;
					boolean ok = bluetoothGatt.readDescriptor(gattDescriptor);
					AddLog("Read Reauest D %s", ok ? "ok" : "fail");
				}
			}
		});

		findViewById(R.id.btn_notify).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				enableNotify(curUUID.tag);
			}
		});

		searchDevice();

		new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

			}
		};

		View.OnClickListener tagclk = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View v = (View) view.getTag();
				TextView tv = (TextView) view;
				if (v.getVisibility() == View.VISIBLE) {
					v.setVisibility(View.GONE);
					tv.setText(tv.getText() + ">>");
				} else {
					v.setVisibility(View.VISIBLE);
					tv.setText(tv.getText().toString().replace(">", ""));
				}
			}
		};

		findViewById(R.id.tv_devices).setTag(findViewById(R.id.lv_devices));
		findViewById(R.id.tv_devices).setOnClickListener(tagclk);

		findViewById(R.id.tv_uuids).setTag(findViewById(R.id.lv_uuids));
		findViewById(R.id.tv_uuids).setOnClickListener(tagclk);

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				uuidsAdapter.notifyDataSetChanged();
				handler.postDelayed(this, 2000);

				if (bluetoothGatt != null)
					bluetoothGatt.readRemoteRssi();
			}
		}, 2000);
	}

	private void AddLog(String ftm, Object... args) {
		String log = String.format(ftm, args);
		safeCall(new Callable() {
			@Override
			public void call(Object... args) {
				++logct;
				tv_log.append(String.format("%03d ", logct));
				tv_log.append(args[0].toString());
				tv_log.append("\n");

				((ScrollView) tv_log.getParent()).post(new Runnable() {
					@Override
					public void run() {
						((ScrollView) tv_log.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		}, log);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {

			connectDevice();

			return true;
		}
		if (id == R.id.action_searchdevice) {
			searchDevice();
		}
		return super.onOptionsItemSelected(item);
	}

	BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			uuids.clear();
			AddLog("Services discovered!");
			for (BluetoothGattService ser : gatt.getServices()) {
				uuids.add(new BLEUUID(ser.getUuid(), ser));
				for (BluetoothGattCharacteristic cs : ser.getCharacteristics()) {
					uuids.add(new BLEUUID(cs.getUuid(), cs));
					for (BluetoothGattDescriptor ds : cs.getDescriptors()) {
						uuids.add(new BLEUUID(cs.getUuid(), ds));
					}
				}
			}
			// bluetoothGatt.beginReliableWrite();
			safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					uuidsAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				AddLog("Gatt connected!");
				AddLog("Discovering services...");
				gatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				AddLog("Gatt disconnected!");
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			byte[] bts = characteristic.getValue();
			AddLog("onCharacteristicChange: %s", bytes2String(bts) + (bts != null ? "(" + bytesDecode(bts) + ")" : ""));
			super.onCharacteristicChanged(gatt, characteristic);

			String uuid = characteristic.getUuid().toString();
			for (BLEUUID u : uuids) {
				if (u.tag instanceof BluetoothGattCharacteristic && uuid.equals(((BluetoothGattCharacteristic) u.tag).getUuid().toString())) {
					u.value = characteristic.getValue();
					u.lasttime = System.currentTimeMillis();
				}
			}
			safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					uuidsAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			byte[] bts = characteristic.getValue();
			AddLog("onCharacteristicRead %d: %s", status, bytes2String(bts) + (bts != null ? "(" + bytesDecode(bts) + ")" : ""));

			String uuid = characteristic.getUuid().toString();
			for (BLEUUID u : uuids) {
				if (u.tag instanceof BluetoothGattCharacteristic && uuid.equals(((BluetoothGattCharacteristic) u.tag).getUuid().toString())) {
					u.value = characteristic.getValue();
					u.lasttime = System.currentTimeMillis();
				}
			}
			safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					uuidsAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			AddLog("onCharacteristicWrite" + status);
			super.onCharacteristicWrite(gatt, characteristic, status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (curBleDevice != null)
				curBleDevice.rssi = rssi;
			safeCall(new Callable() {

				@Override
				public void call(Object... args) {
					deviceAdapter.notifyDataSetChanged();
				}
			});
			super.onReadRemoteRssi(gatt, rssi, status);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			byte[] bts = descriptor.getValue();
			AddLog("onDescriptorRead %d: %s", status, bytes2String(bts) + (bts != null ? "(" + bytesDecode(bts) + ")" : ""));
			super.onDescriptorRead(gatt, descriptor, status);

			String uuid = descriptor.getUuid().toString();
			for (BLEUUID u : uuids) {
				if (u.tag instanceof BluetoothGattDescriptor && uuid.equals(((BluetoothGattDescriptor) u.tag).getUuid().toString())) {
					u.value = descriptor.getValue();
					u.lasttime = System.currentTimeMillis();
				}
			}
			safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					uuidsAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			AddLog("onDescriptorWrite" + status);
			super.onDescriptorWrite(gatt, descriptor, status);
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			AddLog("onReliableWriteCompleted" + status);
			super.onReliableWriteCompleted(gatt, status);
		}

	};

	void enableNotify(Object obj) {
		if (obj instanceof BluetoothGattCharacteristic) {
			BluetoothGattCharacteristic cs = (BluetoothGattCharacteristic) obj;
			bluetoothGatt.setCharacteristicNotification(cs, true);
			AddLog("notify enable of Characteristic");
		} else if (obj instanceof BluetoothGattDescriptor) {
			BluetoothGattDescriptor ds = (BluetoothGattDescriptor) obj;
			ds.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			bluetoothGatt.writeDescriptor(ds);
		}
	}

	void disconnectDevice() {
		if (bluetoothGatt != null) {
			bluetoothGatt.disconnect();
			bluetoothGatt.close();
			bluetoothGatt = null;
			AddLog("Gatt disconnected");
		}
	}

	void connectDevice() {
		disconnectDevice();
		if (curBleDevice != null) {
			AddLog("Start connect to Gatt...");
			bluetoothGatt = curBleDevice.device.connectGatt(MainActivity.this, true, gattCallback);
		}
	}

	BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			BLEDevice bleDevice = new BLEDevice();
			bleDevice.device = device;

			Log.i(TAG, "found: " + device.getAddress());
			int ix = devices.indexOf(bleDevice);
			if (ix >= 0) {
				bleDevice = devices.get(ix);
			} else {
				AddLog("Scaned %s", device.getAddress());
			}
			bleDevice.rssi = rssi;
			bleDevice.scanRecord = scanRecord;
			safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					BLEDevice bled = (BLEDevice) args[0];
					synchronized (bled) {
						if (devices.contains(bled) == false) {
							devices.add(bled);
						}
					}
					deviceAdapter.notifyDataSetChanged();
				}
			}, bleDevice);

		}
	};
	BluetoothAdapter blueadapter = null;

	private void stopSearch() {
		disconnectDevice();
		if (blueadapter != null) {
			blueadapter.stopLeScan(leScanCallback);
			blueadapter = null;
			AddLog("Stop scan BLE device");
		}
	}

	private void searchDevice() {
		stopSearch();
		if (blueadapter == null)
			blueadapter = BluetoothAdapter.getDefaultAdapter();
		if (blueadapter != null) {
			if (!blueadapter.isEnabled()) {
				AddLog("蓝牙不可用~");
				Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
				startActivity(in);
			} else {
				curBleDevice = null;
				devices.clear();
				deviceAdapter.notifyDataSetChanged();
				uuids.clear();
				uuidsAdapter.notifyDataSetChanged();
				blueadapter.startLeScan(leScanCallback);

				ProgressDialog dlg = new ProgressDialog(this);
				dlg.setMessage("正在搜索附近的BLE设备...");
				AddLog("Scanning BLE device...");
				dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface arg0) {
						stopSearch();
					}
				});
				dlg.show();
			}
		} else {
			showToast("设备不支持蓝牙功能");
		}
	}

	@Override
	protected void onDestroy() {
		stopSearch();
		super.onDestroy();
	}

	protected ProgressDialog doingDlg = null;

	public ProgressDialog showOperating() {
		return showOperating("正在加载...");
	}

	public ProgressDialog showOperating(String title) {
		if (doingDlg == null) {
			doingDlg = new ProgressDialog(this);
			doingDlg.setTitle(null);
			// doingDlg.setCancelable(false);
		}
		doingDlg.setMessage(title);
		doingDlg.show();
		return doingDlg;
	}

	public void hideOperating() {
		if (doingDlg != null)
			doingDlg.dismiss();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && blueadapter != null) {
			blueadapter.stopLeScan(leScanCallback);
			blueadapter = null;
			hideOperating();
		}
		return super.onKeyDown(keyCode, event);
	}
}
