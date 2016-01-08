package com.sin.android.bledebug;

import java.util.Iterator;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.sin.android.sinlibs.activities.BaseActivity;
import com.sin.android.sinlibs.exutils.StrUtils;

public class MainActivity extends BaseActivity {
	static String TAG = "BT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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

	private void searchDevice() {
		BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();
		if (blueadapter != null) {
			if (!blueadapter.isEnabled()) {
				showToast("不可用~");
				Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
				startActivity(in);
			} else {

				blueadapter.startLeScan(new BluetoothAdapter.LeScanCallback() {

					@Override
					public void onLeScan(BluetoothDevice bd, int arg1, byte[] arg2) {
						safeToast(bd.getName() + " ");
					}
				});
			}
		} else {
			showToast("设备不支持蓝牙功能");
		}
	}
}
