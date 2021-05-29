package com.bluetag.inheart.SDKSample;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;

import com.bluetag.inheart.R;
import com.tnrbiofab.ThermocareSDK;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.bluetag.inheart.SDKSample.Utility.UUIDType.BLESTD;


public class BLEManager {
    private Context context;
    private final int ANDROID_SDK_VERSION = Build.VERSION.SDK_INT;
    private boolean is_connected = false;
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothAdapter.LeScanCallback le_scancallback;
    private ScanCallback scancallback;
    private byte[] model_number;
    private byte[] serial_number;
    private byte[] latest_sensor_data;
    private byte[] sensor_calibration_data;

    public BLEManager(Context context) {
        this.context = context;
        this.model_number = null;
        this.serial_number = null;
        this.latest_sensor_data = null;
        this.sensor_calibration_data = null;

        adapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    public boolean isConnected() {
        return is_connected;
    }

    public void setConnected(boolean is_connected) {
        this.is_connected = is_connected;
        if(!this.is_connected) {
            this.model_number = null;
            this.serial_number = null;
            this.latest_sensor_data = null;
            this.sensor_calibration_data = null;
        }
    }

    public void discoverAndConnect() {
        if(adapter != null && adapter.isEnabled()) {
            if(ANDROID_SDK_VERSION >= Build.VERSION_CODES.LOLLIPOP) {
                discoverAndConnectNew();
            } else {
                discoverAndConnectOld();
            }
        } else {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BT_NOT_AVAILABLE), null);
        }
    }

    private void discoverAndConnectOld() {
        le_scancallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(device.getName().equals(context.getString(R.string.DEVICE_NAME_THERMOCARE))) {
                    Utility.sendIntent(context, context.getString(R.string.FLAG_DISCOVERED_THERMOMETER), null);
                    adapter.stopLeScan(le_scancallback);
                    connect(device);
                }
            }
        };

        adapter.startLeScan(le_scancallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void discoverAndConnectNew() {
        scanner = adapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setDeviceName(context.getString(R.string.DEVICE_NAME_THERMOCARE)).build());

        scancallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                try {
                    Utility.sendIntent(context, context.getString(R.string.FLAG_DISCOVERED_THERMOMETER), null);
                    scanner.stopScan(scancallback);
                    ThermocareSDK.getInstance().setDeviceInfo(result.getScanRecord().getBytes());
                    connect(result.getDevice());
                } catch(NullPointerException e) {
                    Utility.sendIntent(context, context.getString(R.string.FLAG_SDK_NOT_AVAILABLE), null);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        scanner.startScan(filters, settings, scancallback);
    }

    private void connect(BluetoothDevice device) {
        disconnect();
        gatt = device.connectGatt(context, false, new BLEGattCallback(this, context));
    }

    public void disconnect() {
        if(gatt != null) {
            gatt.close();
            gatt = null;
        }
    }

    boolean readCharacteristic(String svcUUID, String characteristicUUID) {
        try {
            BluetoothGattService svc = gatt.getService(UUID.fromString(svcUUID));
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID.fromString(characteristicUUID));

            return gatt.readCharacteristic(ch);
        } catch(NullPointerException e) {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
            return false;
        }
    }

    boolean writeCharacteristic(String svcUUID, String characteristicUUID, byte[] value) {
        try {
            BluetoothGattService svc = gatt.getService(UUID.fromString(svcUUID));
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID.fromString(characteristicUUID));
            ch.setValue(value);

            return gatt.writeCharacteristic(ch);
        } catch(NullPointerException e) {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
            return false;
        }
    }

    boolean setNotification(String svcUUID, String characteristicUUID, boolean isEnable) {
        try {
            BluetoothGattService svc = gatt.getService(UUID.fromString(svcUUID));
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID.fromString(characteristicUUID));
            BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString(Utility.getUUIDString(context, BLESTD, "2902")));

            if(isEnable) {
                gatt.setCharacteristicNotification(ch, true);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                gatt.setCharacteristicNotification(ch, false);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            return gatt.writeDescriptor(descriptor);
        } catch(NullPointerException e) {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
            return false;
        }
    }

    boolean setIndication(String svcUUID, String characteristicUUID, boolean isEnable) {
        try {
            BluetoothGattService svc = gatt.getService(UUID.fromString(svcUUID));
            BluetoothGattCharacteristic ch = svc.getCharacteristic(UUID.fromString(characteristicUUID));
            BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString(Utility.getUUIDString(context, BLESTD, "2902")));

            if(isEnable) {
                gatt.setCharacteristicNotification(ch, true);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            } else {
                gatt.setCharacteristicNotification(ch, false);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            return gatt.writeDescriptor(descriptor);
        } catch(NullPointerException e) {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
            return false;
        }
    }

    byte[] getLatestSensorData() {
        return latest_sensor_data;
    }

    public void setLatestSensorData(byte[] value) {
        this.latest_sensor_data = value;
    }

    public byte[] getSensorCalibrationData() {
        return sensor_calibration_data;
    }

    void setSensorCalibrationData(byte[] value) {
        this.sensor_calibration_data = value;
    }

    public byte[] getModelNumber() {
        return model_number;
    }

    void setModelNumber(byte[] value) {
        this.model_number = value;
    }

    public byte[] getSerialNumber() {
        return serial_number;
    }

    void setSerialNumber(byte[] value) {
        this.serial_number = value;
    }
}
