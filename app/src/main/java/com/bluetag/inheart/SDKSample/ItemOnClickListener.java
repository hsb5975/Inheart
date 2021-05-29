package com.bluetag.inheart.SDKSample;

import android.content.Context;
import android.view.View;

import com.tnrbiofab.ThermocareSDK;

import java.util.Calendar;
import java.util.TimeZone;

import static com.bluetag.inheart.SDKSample.Utility.UUIDType.BLESTD;
import static com.bluetag.inheart.SDKSample.Utility.UUIDType.THERMOCARE;

public class ItemOnClickListener implements View.OnClickListener {
    private Context context;
    private Item item;
    private BLEManager ble_manager;
    private ThermocareSDK sdk = ThermocareSDK.getInstance();

    ItemOnClickListener(Context context, Item item, BLEManager ble_manager) {
        this.context = context;
        this.item = item;
        this.ble_manager = ble_manager;
    }

    @Override
    public void onClick(View view) {
        switch(item.getBehavior()) {
            // Device Information Service (DIS)
            case Behavior.BLESTD_READ_MANUFACTURER_NAME:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A29"));
                break;
            case Behavior.BLESTD_READ_MODEL_NUMBER:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A24"));
                break;
            case Behavior.BLESTD_READ_SERIAL_NUMBER:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A25"));
                break;
            case Behavior.BLESTD_READ_HW_REV:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A27"));
                break;
            case Behavior.BLESTD_READ_FW_REV:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A26"));
                break;
            case Behavior.BLESTD_READ_SW_REV:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A28"));
                break;
            case Behavior.BLESTD_READ_SYSTEM_ID:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A23"));
                break;
            case Behavior.BLESTD_READ_IEEE_11073_20601:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180A"), Utility.getUUIDString(context, BLESTD, "2A2A"));
                break;

            // Current Time Service (CTS)
            case Behavior.BLESTD_WRITE_CURRENT_TIME:
                Calendar cal = Calendar.getInstance();
                cal.setTimeZone(TimeZone.getTimeZone("UTC"));

                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1805"), Utility.getUUIDString(context, BLESTD, "2A2B"),
                Utility.getConvertedTimeByteArray(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND)
                ));
                break;
            case Behavior.BLESTD_READ_CURRENT_TIME:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1805"), Utility.getUUIDString(context, BLESTD, "2A2B"));
                break;
            case Behavior.BLESTD_CURRENT_TIME_NOTIFICATION_ON:
                ble_manager.setNotification(Utility.getUUIDString(context, BLESTD, "1805"), Utility.getUUIDString(context, BLESTD, "2A2B"), true);
                break;
            case Behavior.BLESTD_CURRENT_TIME_NOTIFICATION_OFF:
                ble_manager.setNotification(Utility.getUUIDString(context, BLESTD, "1805"), Utility.getUUIDString(context, BLESTD, "2A2B"), false);
                break;

            // Battery Service (BAS)
            case Behavior.BLESTD_READ_BATTERY_LEVEL:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "180F"), Utility.getUUIDString(context, BLESTD, "2A19"));
                break;

            // Health Thermometer Service (HTS)
            case Behavior.BLESTD_TEMPERATURE_MEASUREMENT_INDICATION_ON:
                ble_manager.setIndication(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, BLESTD, "2A1C"), true);
                break;
            case Behavior.BLESTD_TEMPERATURE_MEASUREMENT_INDICATION_OFF:
                ble_manager.setIndication(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, BLESTD, "2A1C"), false);
                break;
            case Behavior.BLESTD_READ_TEMPERATURE_TYPE:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, BLESTD, "2A1D"));
                break;
            case Behavior.THERMOCARE_READ_SENSOR_DATA:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A75"));
                break;
            case Behavior.THERMOCARE_ACK_SENSOR_DATA:
                if(ble_manager.getLatestSensorData() != null) {
                    ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A76"), ble_manager.getLatestSensorData());
                }
                break;
            case Behavior.THERMOCARE_READ_CURRENT_USER:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A78"));
                break;
            case Behavior.THERMOCARE_WRITE_CURRENT_USER:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A78"), new byte[] { 0x00, 0x00 });
                break;
            case Behavior.THERMOCARE_READ_USER_LIST:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A79"));
                break;
            case Behavior.THERMOCARE_WRITE_USER_LIST:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A79"), sdk.makeUserList(new int[] { 0, 1, 3, 5 }));
                break;
            case Behavior.THERMOCARE_READ_CALIBRATION_DATA:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A74"), new byte[] {'T', (byte)0xFF, (byte)0xFF});
                break;
            case Behavior.THERMOCARE_READ_DATA_COUNT:
                ble_manager.readCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A81"));
                break;
            case Behavior.THERMOCARE_ERASE_ALL_DATA:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A81"), new byte[] { 0x00, 0x01 });
                break;
            case Behavior.THERMOCARE_BT_ALWAYS_SW_ON:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A82"), new byte[] { 0x7E });
                break;
            case Behavior.THERMOCARE_BT_ALWAYS_SW_OFF:
                ble_manager.writeCharacteristic(Utility.getUUIDString(context, BLESTD, "1809"), Utility.getUUIDString(context, THERMOCARE, "2A82"), new byte[] { 0x00 });
                break;
            default:
                break;
        }
    }
}