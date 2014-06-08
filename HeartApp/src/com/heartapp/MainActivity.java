package com.heartapp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private BluetoothLeService mBluetoothLeService;
	private final static String TAG = MainActivity.class.getSimpleName();

	private TextView tvHeartRate, tvDanger;

	private SQLiteDatabase db;

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	private String mDeviceName;
	private String mDeviceAddress;
	private Context mContext;
	private String uuid;
	private boolean ifCon = false;
	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("received!");
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				// connected
				mHandler.sendEmptyMessage(2);

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				// disconnected
				mHandler.sendEmptyMessage(1);
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayHRData(mBluetoothLeService.getSupportedGattServices());

			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};

	private void displayHRData(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			if (SampleGattAttributes.lookup(uuid, "a").equals(
					"Heart Rate Service")) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService
						.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					uuid = gattCharacteristic.getUuid().toString();
					if (SampleGattAttributes.lookup(uuid, "a").equals(
							"Heart Rate Measurement")) {
						BluetoothGattCharacteristic characteristic = gattCharacteristic;
						int charaProp = gattCharacteristic.getProperties();
						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
							// If there is an active notification on a
							// characteristic, clear
							// it first so it doesn't update the data field on
							// the user interface.
							if (mNotifyCharacteristic != null) {
								mBluetoothLeService
								.setCharacteristicNotification(
										mNotifyCharacteristic, false);
								mNotifyCharacteristic = null;
							}
							mBluetoothLeService
							.readCharacteristic(characteristic);
						}
						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
							mNotifyCharacteristic = characteristic;
							mBluetoothLeService.setCharacteristicNotification(
									characteristic, true);
						}
					}
				}
			}
		}
	}

	private void displayData(String data) {
		Message msg = new Message();
		msg.obj = data;
		msg.what = 3;
		System.out.println(data);
		mHandler.sendMessage(msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		db = openOrCreateDatabase("HeartRateDB", MODE_WORLD_WRITEABLE, null);
		String str = "create table if not exists heartrate(date text, rate text)";
		db.execSQL(str);



		mContext = this.getApplicationContext();
		tvDanger = (TextView) findViewById(R.id.textview07);
		tvHeartRate = (TextView) findViewById(R.id.textview06);
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		Button button = (Button) findViewById(R.id.originbutton);

		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, Origin.class);
				startActivity(i);
			}
		});

		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {

				case 0:
					Context con = getApplicationContext();
					System.out.println("aaa");

					System.out.println(mDeviceName);
					System.out.println(mDeviceAddress);

					con.registerReceiver(mGattUpdateReceiver,
							makeGattUpdateIntentFilter());

					Intent gattServiceIntent = new Intent(con,
							BluetoothLeService.class);
					con.bindService(gattServiceIntent, mServiceConnection,
							BIND_AUTO_CREATE);

					// mBluetoothLeService.connect(mDeviceAddress);
					// Toast.makeText(getApplicationContext(), (String)msg.obj,
					// Toast.LENGTH_LONG);
					break;

				case 1:
					Toast.makeText(getApplicationContext(), "disconnected!!",
							Toast.LENGTH_LONG).show();

					if (ifCon) {
						Calendar calendar = Calendar.getInstance();
						Date curDate = calendar.getTime();
						String date = (curDate.getMonth()+1) + "." + curDate.getDate();
						String str = tvHeartRate.getText().toString();
						if(!str.equals("심박수")) {
							int HRate = Integer.parseInt(str);

							String sql = "insert into heartrate values ('"+date+"','"+HRate+"')";
							db.execSQL(sql);
							
							Toast.makeText(getApplicationContext(), "자동저장되었습니다", Toast.LENGTH_LONG).show();
						}
					}

					break;

				case 2:
					ifCon = true;
					Toast.makeText(getApplicationContext(), "connected!!",
							Toast.LENGTH_LONG).show();
					break;

				case 3:
					String data = msg.obj.toString();
					tvHeartRate.setText(data);
					int iData = Integer.parseInt(data);
					if (iData <= 60) {
						tvDanger.setText("정상");
						tvDanger.setTextColor(Color.GREEN);
					} else if (iData <= 90) {
						tvDanger.setText("위험");
						tvDanger.setTextColor(Color.BLUE);
					} else {
						tvDanger.setText("고위험");
						tvDanger.setTextColor(Color.RED);

					}
					break;
				}

			}

		};
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		System.out.println("resume");
		scanLeDevice(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					mDeviceName = device.getName();
					mDeviceAddress = device.getAddress();

					if (mDeviceName.equals("HRM")) {

						Message msg = new Message();
						msg.what = 0;
						msg.obj = mDeviceAddress;

						System.out.println(mDeviceName);
						System.out.println(mDeviceAddress);

						mHandler.sendMessage(msg);
						scanLeDevice(false);
					}
				}
			});
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
		.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

}
