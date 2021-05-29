package com.bluetag.inheart.SDKSample;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.bluetag.inheart.R;

import java.util.HashMap;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.bluetag.inheart.SDKSample.Utility.UUIDType.BLESTD;
import static com.bluetag.inheart.SDKSample.Utility.UUIDType.THERMOCARE;

public class BLEGattCallback extends BluetoothGattCallback {
    private BLEManager ble_manager;
    private Context context;

    BLEGattCallback(BLEManager ble_manager, Context context) {
        this.ble_manager = ble_manager;
        this.context = context;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        switch(newState) {
            case BluetoothProfile.STATE_CONNECTED:
                if(gatt != null) {
                    gatt.discoverServices();
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Utility.sendIntent(context, context.getString(R.string.FLAG_DISCONNECTED_GATT), null);
                break;
            default:
                break;
        }
    }

    private boolean initialize() {
        boolean ret = false;

        if(ble_manager.getModelNumber() == null) {
            ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A24"));
        } else if(ble_manager.getSerialNumber() == null){
            ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A25"));
        } else if(ble_manager.getSensorCalibrationData() == null){
            ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A74"), new byte[] { 'T', (byte)0xFF, (byte)0xFF});
        }else {
            ret = true;
        }

        return ret;
    }

    private boolean isInitialized() {
        return (ble_manager.getModelNumber() != null &&
                ble_manager.getSerialNumber() != null) &&
                ble_manager.getSensorCalibrationData() != null ;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if(status == GATT_SUCCESS) {
            ble_manager.setConnected(true);
            Utility.sendIntent(context, context.getString(R.string.FLAG_CONNECTED_GATT), null);
            initialize();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if(status == GATT_SUCCESS) {

            if(isInitialized()) {
                HashMap<String, Object> data = new HashMap<>();
                data.put("uuid", characteristic.getUuid().toString());
                data.put("ch_value", characteristic.getValue());
                Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_READ), data);
            } else {
                if (characteristic.getUuid().toString().equals(Utility.getUUIDString(context, BLESTD, "2A24"))) {
                    ble_manager.setModelNumber((byte[])characteristic.getValue());
                } else if (characteristic.getUuid().toString().equals(Utility.getUUIDString(context, BLESTD, "2A25"))) {
                    ble_manager.setSerialNumber((byte[])characteristic.getValue());
                } else if(characteristic.getUuid().toString().equals(Utility.getUUIDString(context, THERMOCARE, "2A74"))) {
                    ble_manager.setSensorCalibrationData((byte[])characteristic.getValue());
                }
                initialize();
            }
        } else {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if(status == GATT_SUCCESS) {
            if(isInitialized() && !characteristic.getUuid().toString().equals(Utility.getUUIDString(context, THERMOCARE, "2A74"))) {
                HashMap<String, Object> data = new HashMap<>();
                data.put("uuid", characteristic.getUuid().toString());
                data.put("ch_value", characteristic.getValue());
                Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_WRITE), data);
            } else {
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A74"));
            }

        } else {
            Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_RW_ERROR), null);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        HashMap<String, Object> data = new HashMap<>();
        data.put("uuid", characteristic.getUuid().toString());
        data.put("ch_value", characteristic.getValue());
        Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_CHANGED), data);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        String uuid = descriptor.getCharacteristic().getUuid().toString();
        HashMap<String, Object> data = new HashMap<>();

        if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A2B"))) {
            data.put("type", context.getString(R.string.TYPE_CTS_NOTIFICATION));

        } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A1C"))) {
            data.put("type", context.getString(R.string.TYPE_HTS_INDICATION));
        }
        Utility.sendIntent(context, context.getString(R.string.FLAG_BLE_CH_DESC_WRITE), data);
    }
}
