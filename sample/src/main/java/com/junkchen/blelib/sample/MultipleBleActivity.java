/*
 * Copyright 2016 Junk Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.junkchen.blelib.sample;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleService;
import com.junkchen.blelib.MultipleBleService;
import com.junkchen.blelib.sample.adapter.CommonAdapter;
import com.junkchen.blelib.sample.adapter.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleBleActivity extends AppCompatActivity {
    //Debugging
    private static final String TAG = MultipleBleActivity.class.getSimpleName();

    //Constant
    public static final int SERVICE_BIND = 1;
    public static final int CONNECT_CHANGE = 2;

    //Member fields
    private boolean mIsBind;
    private MultipleBleService mBleService;
    private CommonAdapter<Map<String, Object>> deviceAdapter;
    private List<Map<String, Object>> deviceList;
    private String connDeviceName;
    private String connDeviceAddress;

    //Layout view
    private Button btn_scanBle;
    private TextView txtv_connNum;
    private ListView lstv_devList;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((MultipleBleService.LocalBinder) service).getService();
            if (mBleService != null) mHandler.sendEmptyMessage(SERVICE_BIND);
            if (mBleService.initialize()) {
                if (mBleService.enableBluetooth(true)) {
                    showDialog(getResources().getString(R.string.scanning));
                    mBleService.scanLeDevice(true);
                    Toast.makeText(MultipleBleActivity.this, "Bluetooth was opened", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MultipleBleActivity.this, "not support Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            mIsBind = false;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_BIND:
                    setBleServiceListener();
                    break;
                case CONNECT_CHANGE:
                    txtv_connNum.setText(mBleService.getConnectDevices().size());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        initView();
        initAdapter();
        registerReceiver(bleReceiver, makeIntentFilter());
        doBindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnBindService();
        unregisterReceiver(bleReceiver);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onBackPressed() {
        if (mBleService.isScanning()) {
            mBleService.scanLeDevice(false);
            return;
        }
        super.onBackPressed();
    }

    private void initView() {
        btn_scanBle = (Button) findViewById(R.id.btn_scanBle);
        txtv_connNum = (TextView) findViewById(R.id.txtv_connNum);
        lstv_devList = (ListView) findViewById(R.id.lstv_devList);

        txtv_connNum.setVisibility(View.VISIBLE);

        btn_scanBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBleService.isScanning()) {
                    mBleService.scanLeDevice(true);
                }
            }
        });
    }

    private void initAdapter() {
        deviceList = new ArrayList<>();
        deviceAdapter = new CommonAdapter<Map<String, Object>>(
                this, R.layout.item_device, deviceList) {
            @Override
            public void convert(ViewHolder holder, final Map<String, Object> deviceMap) {
                holder.setText(R.id.txtv_name, deviceMap.get("name").toString());
                holder.setText(R.id.txtv_address, deviceMap.get("address").toString());
                holder.setText(R.id.txtv_connState, ((boolean) deviceMap.get("isConnect")) ?
                        getResources().getString(R.string.state_connected) :
                        getResources().getString(R.string.state_disconnected));
                holder.setText(R.id.btn_connect, ((boolean) deviceMap.get("isConnect")) ?
                        getResources().getString(R.string.disconnected) :
                        getResources().getString(R.string.connected));
                holder.getView(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ((boolean) deviceMap.get("isConnect")) {
                            mBleService.disconnect(deviceMap.get("address").toString());
                            showDialog(getString(R.string.disconnecting));
                        } else {
                            connDeviceAddress = (String) deviceMap.get("address");
                            connDeviceName = (String) deviceMap.get("name");
                            HashMap<String, Object> connDevMap = new HashMap<String, Object>();
                            connDevMap.put("name", connDeviceName);
                            connDevMap.put("address", connDeviceAddress);
                            connDevMap.put("isConnect", false);
                            mBleService.connect(connDeviceAddress);
                            showDialog(getString(R.string.connecting));
                        }
                    }
                });
            }
        };
        lstv_devList.setAdapter(deviceAdapter);
    }

    private void setBleServiceListener() {
        mBleService.setOnDataAvailableListener(new MultipleBleService.OnDataAvailableListener() {
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            }
        });
    }

    private void doBindService() {
        Intent serviceIntent = new Intent(this, MultipleBleService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnBindService() {
        if (mIsBind) {
            unbindService(serviceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.i(TAG, "name: " + tmpDevName + ", address: " + tmpDevAddress);
                HashMap<String, Object> deviceMap = new HashMap<>();
                deviceMap.put("name", tmpDevName);
                deviceMap.put("address", tmpDevAddress);
                deviceMap.put("isConnect", false);
                deviceList.add(deviceMap);
                deviceAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                deviceList.get(0).put("isConnect", true);
                deviceAdapter.notifyDataSetChanged();
                mHandler.sendEmptyMessage(CONNECT_CHANGE);
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                deviceList.get(0).put("isConnect", false);
                deviceAdapter.notifyDataSetChanged();
                mHandler.sendEmptyMessage(CONNECT_CHANGE);
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
                btn_scanBle.setEnabled(true);
                dismissDialog();
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        return intentFilter;
    }

    /**
     * Show dialog
     */
    private ProgressDialog progressDialog;

    private void showDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissDialog() {
        if (progressDialog == null) return;
        progressDialog.dismiss();
        progressDialog = null;
    }
}
