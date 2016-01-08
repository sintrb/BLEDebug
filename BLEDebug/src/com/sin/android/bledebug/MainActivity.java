package com.sin.android.bledebug;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.sin.android.sinlibs.activities.BaseActivity;
import com.sin.android.sinlibs.adapter.SimpleListAdapter;
import com.sin.android.sinlibs.adapter.SimpleViewInitor;
import com.sin.android.sinlibs.base.Callable;
import com.sin.android.sinlibs.exutils.StrUtils;
import com.sin.android.sinlibs.tagtemplate.ViewRender;
import com.sin.android.sinlibs.utils.InjectUtils;

public class MainActivity extends BaseActivity {
	static String TAG = "BT";
	private ListView lvDevices = null;

	private List<BLEDevice> devices = new ArrayList<BLEDevice>();
	private SimpleListAdapter deviceAdapter = null;

	private ViewRender render = new ViewRender();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		InjectUtils.injectViews(this, R.class);

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
		lvDevices.setAdapter(deviceAdapter);
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
			safeCall(new Callable() {

				@Override
				public void call(Object... args) {
					BLEDevice bled = (BLEDevice) args[0];
					devices.add(bled);
					deviceAdapter.notifyDataSetChanged();
				}
			}, bleDevice);
		}
	};

	private void searchDevice() {
		BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();
		if (blueadapter != null) {
			if (!blueadapter.isEnabled()) {
				showToast("不可用~");
				Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
				startActivity(in);
			} else {
				devices.clear();
				deviceAdapter.notifyDataSetChanged();
				blueadapter.startLeScan(leScanCallback);
			}
		} else {
			showToast("设备不支持蓝牙功能");
		}
	}
}
