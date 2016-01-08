package com.sin.android.bledebug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

	private List<ParcelUuid> uuids = new ArrayList<ParcelUuid>();
	private SimpleListAdapter uuidsAdapter = null;

	private ViewRender render = new ViewRender();

	BLEDevice bleDevice = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		deviceAdapter = new SimpleListAdapter(this, devices, new SimpleViewInitor() {
			@Override
			public View initView(Context context, int position, View convertView, ViewGroup parent, Object data) {
				if (convertView == null) {
					convertView = LinearLayout.inflate(MainActivity.this, R.layout.item_device, null);
				}
				render.renderView(convertView, data);
				return convertView;
			}
		});

		uuidsAdapter = new SimpleListAdapter(this, uuids, new SimpleViewInitor() {
			@Override
			public View initView(Context context, int position, View convertView, ViewGroup parent, Object data) {
				if (convertView == null) {
					convertView = new TextView(MainActivity.this);
					convertView.setTag("tt:{.m:toString}");
				}
				if (data.equals(bleDevice)) {
					convertView.setBackgroundColor(0x808080);
				} else {
					convertView.setBackgroundColor(0x00000000);
				}
				render.renderView(convertView, data);
				return convertView;
			}
		});
		((ListView) findViewById(R.id.lv_devices)).setAdapter(deviceAdapter);
		((ListView) findViewById(R.id.lv_devices)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos, long arg3) {
				if (pos < devices.size()) {
					bleDevice = devices.get(pos);
					uuids.clear();
					ParcelUuid[] uids = bleDevice.device.getUuids();
					for (int i = 0; uids != null && i < uids.length; ++i) {
						uuids.add(uids[i]);
					}
					uuidsAdapter.notifyDataSetChanged();
				}
			}
		});
		((ListView) findViewById(R.id.lv_uuids)).setAdapter(uuidsAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (bleDevice != null) {
				asynCallAndShowProcessDlg("正在连接设备...", new Callable() {

					@Override
					public void call(Object... args) {
						try {
							// String uuid =
							// "00001101-0000-1000-8000-00805F9B34FB";
							String uuid = "F0080001-0451-4000-B000-000000000000";
							// String uuid = uuids.get(0).toString();
							if (bleDevice.device.getBondState() == BluetoothDevice.BOND_NONE) {
								safeToast("需要绑定");
								try {
									BluetoothDevice.class.getMethod("createBond").invoke(bleDevice.device);
								} catch (Exception e) {
									safeToast("绑定失败...");
									e.printStackTrace();
								}
							}
							// Method m =
							// BluetoothDevice.class.getMethod("createRfcommSocket",
							// new Class[] { int.class });
							// BluetoothSocket bs = (BluetoothSocket)
							// m.invoke(bleDevice.device, 1);// 这里端口为1
							BluetoothSocket bs = bleDevice.device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
							bs.connect();
							InputStream is = bs.getInputStream();
							OutputStream os = bs.getOutputStream();
							os.write(new byte[] { (byte) 0xa1, 0x00, 0x00, 0x00 });
							byte[] bts = new byte[1024];
							is.read(bts);
							safeToast(bts.length);

						} catch (Exception e) {
							e.printStackTrace();
							safeToast(e.getMessage());
						}
					}
				});

			}

			return true;
		}
		if (id == R.id.action_searchdevice) {
			searchDevice();
		}
		return super.onOptionsItemSelected(item);
	}

	BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			BLEDevice bleDevice = new BLEDevice();
			bleDevice.device = device;
			bleDevice.rssi = rssi;
			bleDevice.scanRecord = scanRecord;
			Log.i(TAG, "found: " + device.getAddress());
			if (devices.contains(bleDevice) == false) {
				safeCall(new Callable() {
					@Override
					public void call(Object... args) {
						BLEDevice bled = (BLEDevice) args[0];
						synchronized (bled) {
							if (devices.contains(bled) == false) {
								devices.add(bled);
								deviceAdapter.notifyDataSetChanged();
							}
						}
					}
				}, bleDevice);
			}
		}
	};
	BluetoothAdapter blueadapter = null;

	private void searchDevice() {
		if (blueadapter == null)
			blueadapter = BluetoothAdapter.getDefaultAdapter();
		if (blueadapter != null) {
			if (!blueadapter.isEnabled()) {
				showToast("不可用~");
				Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
				startActivity(in);
			} else {
				devices.clear();
				deviceAdapter.notifyDataSetChanged();
				uuids.clear();

				// String[] uis = new String[] {
				// "F0010001-0451-4000-B000-000000000000",
				// "F0010003-0451-4000-B000-000000000000",
				// "F0010002-0451-4000-B000-000000000000",
				// "F0020001-0451-4000-B000-000000000000",
				// "F0020003-0451-4000-B000-000000000000",
				// "F0020002-0451-4000-B000-000000000000",
				// "F0040001-0451-4000-B000-000000000000",
				// "F0040003-0451-4000-B000-000000000000",
				// "F0040002-0451-4000-B000-000000000000",
				// "F0080001-0451-4000-B000-000000000000",
				// "F0080002-0451-4000-B000-000000000000",
				// "F0080003-0451-4000-B000-000000000000",
				// "F00A0001-0451-4000-B000-000000000000",
				// "F00A0003-0451-4000-B000-000000000000",
				// "F00A0002-0451-4000-B000-000000000000",
				// "00002901-0000-1000-8000-00805f9b34fb",
				// "00002902-0000-1000-8000-00805f9b34fb", };
				// uuids.add(ParcelUuid.fromString("F0010001-0451-4000-B000-000000000000"));
				// uuids.add(new
				// ParcelUuid(UUID.fromString("F0010001-0451-4000-B000-000000000000")));
				uuidsAdapter.notifyDataSetChanged();
				blueadapter.startLeScan(leScanCallback);

				ProgressDialog dlg = new ProgressDialog(this);
				dlg.setMessage("正在搜索附近的BLE设备...");
				dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface arg0) {
						 blueadapter.stopLeScan(leScanCallback);
//						blueadapter.cancelDiscovery();
						// blueadapter = null;
					}
				});
				dlg.show();
			}
		} else {
			showToast("设备不支持蓝牙功能");
		}
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
