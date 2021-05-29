package com.bluetag.inheart.SDKSample;

import android.content.Context;
import android.content.Intent;

import com.bluetag.inheart.R;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class Utility {

    static boolean isBluetoothUUID(Context context, String uuid) {
        if(uuid.length() < 9) {
            return false;
        }

        byte[] tmp = uuid.toUpperCase().getBytes();
        tmp[4] = '*';
        tmp[5] = '*';
        tmp[6] = '*';
        tmp[7] = '*';

        return new String(tmp).equals(context.getString(R.string.UUID_FORMAT_BLESTD));
    }

    public static String getUUIDString(Context context, UUIDType type, String identifier) {
        if(type == UUIDType.BLESTD) {
            return context.getString(R.string.UUID_FORMAT_BLESTD).replace("****", identifier).toLowerCase();
        } else if(type == UUIDType.THERMOCARE) {
            return context.getString(R.string.UUID_FORMAT_THERMOCARE).replace("****", identifier).toLowerCase();
        } else {
            return null;
        }
    }

    public static String byteArrayToString(byte[] data) {
        String ret = "";

        for(byte b: ByteBuffer.wrap(data).array()) {
            ret += (char) b;
        }

        return ret;
    }

    public static String byteArrayToHexString(byte[] data) {
        String ret = "";

        for(byte b: ByteBuffer.wrap(data).array()) {
            ret += String.format("%02X ", b);
        }

        return ret.trim();
    }

    static String byteArrayToIntegerString(byte[] data) {
        String ret = "";

        for(byte b: ByteBuffer.wrap(data).array()) {
            ret += String.format("%d ", (int) b);
        }

        return ret;
    }

    static byte[] getConvertedTimeByteArray(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return new byte[] {
                (byte) (year & 0xff),
                (byte) ((year >> 8) & 0xff),
                (byte) month,
                (byte) dayOfMonth,
                (byte) hour,
                (byte) minute,
                (byte) second,
                0x00, 0x00, 0x00
        };
    }

    static void sendIntent(Context context, String flag, HashMap<String, Object> data) {
        Intent intent = new Intent();
        intent.setAction(context.getString(R.string.INTENT_ACTION_NAME));
        intent.putExtra("flag", flag);
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }

    public enum UUIDType {
        BLESTD, THERMOCARE
    }
}
