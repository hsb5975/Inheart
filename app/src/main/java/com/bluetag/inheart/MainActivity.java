package com.bluetag.inheart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bluetag.inheart.SDKSample.BLEManager;
import com.bluetag.inheart.SDKSample.Behavior;
import com.bluetag.inheart.SDKSample.ExpandableListAdapter;
import com.bluetag.inheart.SDKSample.Item;
import com.bluetag.inheart.SDKSample.ItemGroup;
import com.bluetag.inheart.SDKSample.Utility;
import com.google.android.material.snackbar.Snackbar;
import com.tnrbiofab.ThermocareSDK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import static com.bluetag.inheart.SDKSample.Utility.UUIDType.BLESTD;
import static com.bluetag.inheart.SDKSample.Utility.UUIDType.THERMOCARE;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    final Context myApp = this;

    private ArrayList<ItemGroup> item_groups;

    private ThermocareSDK sdk;
    private BLEManager ble_manager;

    private TextView tv_readable_value;
    private TextView tv_hex_value;
    private Button btn_main;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        webView = findViewById(R.id.mainAV_webView);
//
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//
//
//        webView.addJavascriptInterface(new AndroidBridge(), "BRIDGE");
//
//
//        // android 5.0부터 API수준 21이상을 타겟킹하는 경우 아래를 추가해 주시길 바랍니다
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//            CookieManager cookieManager = CookieManager.getInstance();
//            cookieManager.setAcceptCookie(true);
//            cookieManager.setAcceptThirdPartyCookies(webView, true);
//        }
//
//        webView.setWebViewClient(new WebViewClientClass());
//        webView.setWebChromeClient(new setWebChromClient());
//
//
//        webView.loadUrl("http://175.123.253.203:8083");

        try {

            ble_manager = new BLEManager(getApplicationContext());

            // register broadcast receiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(getString(R.string.INTENT_ACTION_NAME));
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            registerReceiver(broadcastReceiver, filter);

            // UI
            tv_readable_value = (TextView) findViewById(R.id.tv_readable_value);
            tv_hex_value = (TextView) findViewById(R.id.tv_hex_value);

            btn_main = (Button) findViewById(R.id.btn_main);
            btn_main.setOnClickListener(onClick_btn_main);

            setData();

            ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
            ExpandableListAdapter expandableListAdapter = new ExpandableListAdapter(this, item_groups);
            expandableListView.setAdapter(expandableListAdapter);

            // check permission
            checkPermission();

            // SDK initialize
            AssetManager am = getApplication().getAssets();
            InputStream is = am.open("license");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;

            while((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }

            // Initialize THERMOCARE SDK
            if(!ThermocareSDK.initialize(os.toByteArray())) {
                Snackbar.make(getWindow().getDecorView(), getString(R.string.msg_sdk_not_available), Snackbar.LENGTH_LONG).show();
            } else {
                Date d = new Date(ThermocareSDK.getExpireDate());
                String expired_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(d);
                Snackbar.make(getWindow().getDecorView(), String.format(Locale.getDefault(), getString(R.string.msg_sdk_available), ThermocareSDK.getVersion(), expired_at), Snackbar.LENGTH_LONG).show();
            }

            sdk = ThermocareSDK.getInstance();

            // Temperature Calculation API for Sensor module
            byte[] cal_data = new byte[20];
            Arrays.fill(cal_data, (byte) 0x00);
            float temperature = sdk.getTemperature(1,
                    0.0f, 8191.0f,
                    3203.0f, 4553.0f,
                    cal_data, false);
            Log.i("TEST_TEMPERATURE", "temperature => " + temperature);

        } catch(IOException e) {
            Snackbar.make(getWindow().getDecorView(), getString(R.string.msg_sdk_not_available), Snackbar.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public BLEManager getBLEManager() {
        return this.ble_manager;
    }

    private void setData() {
        ArrayList<Item> childrenDIS = new ArrayList<>();
        childrenDIS.add(new Item("Manufacturer Name String", Behavior.BLESTD_READ_MANUFACTURER_NAME));
        childrenDIS.add(new Item("Model Number String", Behavior.BLESTD_READ_MODEL_NUMBER));
        childrenDIS.add(new Item("Serial Number String", Behavior.BLESTD_READ_SERIAL_NUMBER));
        childrenDIS.add(new Item("Hardware Revision String", Behavior.BLESTD_READ_HW_REV));
        childrenDIS.add(new Item("Firmware Revision String", Behavior.BLESTD_READ_FW_REV));
        childrenDIS.add(new Item("Software Revision String", Behavior.BLESTD_READ_SW_REV));
        childrenDIS.add(new Item("System ID", Behavior.BLESTD_READ_SYSTEM_ID));
        childrenDIS.add(new Item("IEEE 11073-20601 Regulatory Certification Data List", Behavior.BLESTD_READ_IEEE_11073_20601));

        ArrayList<Item> childrenCTS = new ArrayList<>();
        childrenCTS.add(new Item("Current Time READ", Behavior.BLESTD_READ_CURRENT_TIME));
        childrenCTS.add(new Item("Current Time WRITE (Now)", Behavior.BLESTD_WRITE_CURRENT_TIME));
        childrenCTS.add(new Item("Current Time NOTIFICATION ON", Behavior.BLESTD_CURRENT_TIME_NOTIFICATION_ON));
        childrenCTS.add(new Item("Current Time NOTIFICATION OFF", Behavior.BLESTD_CURRENT_TIME_NOTIFICATION_OFF));

        ArrayList<Item> childrenBAS = new ArrayList<>();
        childrenBAS.add(new Item("Battery Level", Behavior.BLESTD_READ_BATTERY_LEVEL));

        ArrayList<Item> childrenHTS = new ArrayList<>();
        childrenHTS.add(new Item("Temperature Measurement INDICATION ON", Behavior.BLESTD_TEMPERATURE_MEASUREMENT_INDICATION_ON));
        childrenHTS.add(new Item("Temperature Measurement INDICATION OFF", Behavior.BLESTD_TEMPERATURE_MEASUREMENT_INDICATION_OFF));
        childrenHTS.add(new Item("Temperature Type", Behavior.BLESTD_READ_TEMPERATURE_TYPE));
        childrenHTS.add(new Item("Sensor Data Read", Behavior.THERMOCARE_READ_SENSOR_DATA));
        childrenHTS.add(new Item("Sensor Data ACK", Behavior.THERMOCARE_ACK_SENSOR_DATA));
        childrenHTS.add(new Item("Current Group/User READ", Behavior.THERMOCARE_READ_CURRENT_USER));
        childrenHTS.add(new Item("Current Group/User WRITE", Behavior.THERMOCARE_WRITE_CURRENT_USER));
        childrenHTS.add(new Item("User List READ", Behavior.THERMOCARE_READ_USER_LIST));
        childrenHTS.add(new Item("User List WRITE", Behavior.THERMOCARE_WRITE_USER_LIST));
        childrenHTS.add(new Item("Sensor Calibration Data", Behavior.THERMOCARE_READ_CALIBRATION_DATA));
        childrenHTS.add(new Item("Data Count", Behavior.THERMOCARE_READ_DATA_COUNT));
        childrenHTS.add(new Item("Erase Data", Behavior.THERMOCARE_ERASE_ALL_DATA));
        childrenHTS.add(new Item("Bluetooth Always SW ON", Behavior.THERMOCARE_BT_ALWAYS_SW_ON));
        childrenHTS.add(new Item("Bluetooth Always SW OFF", Behavior.THERMOCARE_BT_ALWAYS_SW_OFF));

        item_groups = new ArrayList<>();
        item_groups.add(new ItemGroup("Device Information Service (DIS)", childrenDIS));
        item_groups.add(new ItemGroup("Current Time Service (CTS)", childrenCTS));
        item_groups.add(new ItemGroup("Battery Service (BAS)", childrenBAS));
        item_groups.add(new ItemGroup("Health Thermometer Service (HTS)", childrenHTS));
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.CONFIRM);
            builder.setMessage(R.string.msg_confirm_exit_app);
            builder.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
            builder.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    void checkPermission() {
        boolean is_granted_all_permissions = true;
        String[] permissions = {
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };

        for(String permission: permissions) {
            if(ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                is_granted_all_permissions = false;
                break;
            }
        }

        if(!is_granted_all_permissions) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.title_info_requisite_permissions);
            dialog.setMessage(R.string.msg_info_requisite_permissions);
            dialog.setPositiveButton(R.string.CONFIRM, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    Uri uri = Uri.fromParts("package", getPackageName(), null);

                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(uri);
                    startActivity(intent);

                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            });
            dialog.show();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    Button.OnClickListener onClick_btn_main = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(ble_manager.isConnected()) {
                btn_main.setEnabled(false);
                btn_main.setText(R.string.DISCONNECTING);
                ble_manager.disconnect();
            } else {
                ble_manager.discoverAndConnect();
                btn_main.setEnabled(false);
                btn_main.setText(R.string.DISCOVERING);
            }
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(context.getString(R.string.INTENT_ACTION_NAME))) {
                HashMap<String, Object> data = (HashMap<String, Object>) intent.getSerializableExtra("data");
                String uuid;
                byte[] ch_value;
                String flag = intent.getStringExtra("flag");

                if(flag.equals(getString(R.string.FLAG_BT_NOT_AVAILABLE))) {
                    Snackbar.make(getWindow().getDecorView(), R.string.msg_bt_not_available, Snackbar.LENGTH_LONG).show();
                    btn_main.setText(R.string.CONNECT);
                    btn_main.setEnabled(true);
                } else if(flag.equals(getString(R.string.FLAG_SDK_NOT_AVAILABLE))) {
                    Snackbar.make(getWindow().getDecorView(), R.string.msg_sdk_not_available, Snackbar.LENGTH_LONG).show();
                    btn_main.setText(R.string.CONNECT);
                    btn_main.setEnabled(true);
                } else if(flag.equals(getString(R.string.FLAG_DISCOVERED_THERMOMETER))) {
                    btn_main.setText(R.string.CONNECTING);
                } else if(flag.equals(getString(R.string.FLAG_CONNECTED_GATT))) {
                    Snackbar.make(getWindow().getDecorView(), R.string.msg_connected, Snackbar.LENGTH_SHORT).show();
                    btn_main.setText(R.string.DISCONNECT);
                    btn_main.setEnabled(true);
                } else if(flag.equals(getString(R.string.FLAG_DISCONNECTED_GATT))) {
                    btn_main.setText(R.string.CONNECT);
                    btn_main.setEnabled(true);
                    ble_manager.setConnected(false);
                } else if(flag.equals(getString(R.string.FLAG_BLE_CH_RW_ERROR))) {
                    Snackbar.make(getWindow().getDecorView(), R.string.msg_characteristic_rw_error, Snackbar.LENGTH_LONG).show();
                } else if(flag.equals(getString(R.string.FLAG_BLE_CH_READ))) {
                    uuid = data.get("uuid").toString();
                    ch_value = (byte[]) data.get("ch_value");

                    if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A23"))) {
                        tv_readable_value.setText(parseSystemID(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A2A"))) {
                        tv_readable_value.setText(getString(R.string.NON_HUMAN_READABLE_DATA));
                    } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A2B"))) {
                        tv_readable_value.setText(parseCTSdata(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A19"))) {
                        tv_readable_value.setText(parseBatteryLevel(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A1D"))) {
                        tv_readable_value.setText(parseTemperatureType(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, THERMOCARE, "2A74"))) {
                        tv_readable_value.setText(getString(R.string.NON_HUMAN_READABLE_DATA));
                    } else if(uuid.equals(Utility.getUUIDString(context, THERMOCARE, "2A75"))) {
                        ble_manager.setLatestSensorData(ch_value);
                        tv_readable_value.setText(parseTemperatureMeasurementData(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, THERMOCARE, "2A78"))) {
                        tv_readable_value.setText(parseCurrentGroupUser(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, THERMOCARE, "2A79"))) {
                        tv_readable_value.setText(parseUserList(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, THERMOCARE, "2A81"))) {
                        tv_readable_value.setText(parseDataCount(ch_value));
                    } else {
                        tv_readable_value.setText(Utility.byteArrayToString(ch_value));
                    }

                    tv_hex_value.setText(Utility.byteArrayToHexString(ch_value));
                } else if(flag.equals(getString(R.string.FLAG_BLE_CH_WRITE))) {
                    ch_value = (byte[]) data.get("ch_value");

                    tv_readable_value.setText(getString(R.string.OK));
                    tv_hex_value.setText(Utility.byteArrayToHexString(ch_value));
                } else if(flag.equals(getString(R.string.FLAG_BLE_CH_CHANGED))) {
                    uuid = data.get("uuid").toString();
                    ch_value = (byte[]) data.get("ch_value");

                    if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A2B"))) {
                        tv_readable_value.setText(parseCTSdata(ch_value));
                    } else if(uuid.equals(Utility.getUUIDString(context, BLESTD, "2A1C"))) {
                        tv_readable_value.setText(parseTemperatureMeasurement(ch_value));
                    }

                    tv_hex_value.setText(Utility.byteArrayToHexString(ch_value));
                }
            } else if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Snackbar.make(getWindow().getDecorView(), R.string.msg_disconnected_with_thermometer, Snackbar.LENGTH_SHORT).show();
                btn_main.setText(R.string.CONNECT);
                btn_main.setEnabled(true);
                ble_manager.setConnected(false);
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private String parseSystemID(byte[] val) {
        return String.format("체온계 MAC address: %s:%s:%s:%s:%s:%s",
                Utility.byteArrayToHexString(new byte[] {val[0]}),
                Utility.byteArrayToHexString(new byte[] {val[1]}),
                Utility.byteArrayToHexString(new byte[] {val[2]}),
                Utility.byteArrayToHexString(new byte[] {val[5]}),
                Utility.byteArrayToHexString(new byte[] {val[6]}),
                Utility.byteArrayToHexString(new byte[] {val[7]})
        );
    }

    private String parseCTSdata(byte[] val) {
        int y = (val[1] << 8) | (val[0] & 0xff);
        int m = val[2];
        int d = val[3];
        int h = val[4];
        int i = val[5];
        int s = val[6];

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(y, (m - 1), d, h, i, s);

        return String.format(Locale.KOREA, "현재 체온계 시각: %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(cal.getTime()));
    }

    private String parseBatteryLevel(byte[] val) {
        return String.format(Locale.KOREA, "현재 배터리 잔량: %d %%", val[0]);
    }

    private String parseTemperatureMeasurement(byte[] val) {
        String ret = "";

        boolean is_included_timestamp = ((val[0] & 0x02)) > 0;
        boolean is_included_temperature_type = ((val[0] & 0x04) > 0);
        float target_temperature;
        String unit = ((val[0] & 0x01) == 0) ? "섭씨" : "화씨";
        String measured_at = "정보없음";
        Calendar cal = Calendar.getInstance();
        String temperature_type = "정보없음";

        // value
        int mantissa = ByteBuffer.wrap(val, 1, 3).order(ByteOrder.LITTLE_ENDIAN).getShort();
        int exponent = val[4];
        target_temperature = mantissa * (float)Math.pow(10, exponent);

        // time
        if(is_included_timestamp) {
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.YEAR, ByteBuffer.wrap(val, 5, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            cal.set(Calendar.MONTH, ((short) val[7]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, (short) val[8]);
            cal.set(Calendar.HOUR_OF_DAY, (short) val[9]);
            cal.set(Calendar.MINUTE, (short) val[10]);
            cal.set(Calendar.SECOND, (short) val[11]);

            measured_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(cal.getTime());
        }

        // temperature type
        if(is_included_temperature_type) {
            byte b = (is_included_timestamp ? val[12] : val[5]);

            switch(b) {
                case 0x02:
                    temperature_type = "체온";
                    break;
                case 0x09:
                    temperature_type = "고막";
                    break;
                default:
                    temperature_type = "알수없음";
                    break;
            }
        }

        ret += String.format(Locale.getDefault(), "측정 시각: %s\n", measured_at);
        ret += String.format(Locale.getDefault(), "측정 온도: %.1f (%s)\n", target_temperature, unit);
        ret += String.format(Locale.getDefault(), "온도 타입: %s", temperature_type);

        return ret;
    }

    private String parseTemperatureType(byte[] val) {
        switch(val[0]) {
            case 0x02:
                return "Body";
            case 0x09:
                return "Tympanum";
            default:
                return "UNKNOWN";
        }
    }

    private String parseTemperatureMeasurementData(byte[] val) {
        String ret = "";

        Log.e("DATA", "timestamp >>" + sdk.parseTimestamp(val));
        Log.e("DATA", "index >>" + sdk.parseIndex(val));

        Log.e("DATA", "ambient temperature >>" + sdk.parseAmbientTemperature(val));
        Log.e("DATA", "ambient humidity >>" + sdk.parseAmbientHumidity(val));
        Log.e("DATA", "group number >>" + sdk.parseGroupNumber(val));
        Log.e("DATA", "user number >>" + sdk.parseUserNumber(val));
        Log.e("DATA", "mode >>" + sdk.parseMeasurementMode(val));
        Log.e("DATA", "sound >>" + sdk.parseSoundMode(val));

        Log.e("DATA", "target temperature >>" + sdk.parseTemperature(ble_manager.getModelNumber(),
                ble_manager.getSerialNumber(), val, ble_manager.getSensorCalibrationData()));
        Date d = new Date(sdk.parseTimestamp(val));
        String measured_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(d);

        ret += String.format(Locale.getDefault(), "#%d (측정시각: %s)\n", sdk.parseIndex(val), measured_at);
        ret += String.format(Locale.getDefault(), "  측정온도: %.1f °c\n", sdk.parseTemperature(ble_manager.getModelNumber(),
                ble_manager.getSerialNumber(), val, ble_manager.getSensorCalibrationData()));
        ret += String.format(Locale.getDefault(), "  주변온도: %.1f °c\n", sdk.parseAmbientTemperature(val));
        ret += String.format(Locale.getDefault(), "  주변습도: %.1f %%\n", sdk.parseAmbientHumidity(val));
        ret += String.format(Locale.getDefault(), "  사용자: Group #%d, User #%d\n", sdk.parseGroupNumber(val), sdk.parseUserNumber(val));
        ret += String.format(Locale.getDefault(), "  측정모드: %s\n", (sdk.parseMeasurementMode(val) == ThermocareSDK.MeasurementMode.HUMAN ? "체온" : "사물온도"));
        ret += String.format(Locale.getDefault(), "  알림음: %s", (sdk.parseSoundMode(val) == ThermocareSDK.SoundMode.ON ? "켜짐" : "꺼짐"));

        return ret;
    }

    private String parseCurrentGroupUser(byte[] val) {
        return String.format(Locale.getDefault(), "Group #%d, User #%d", (int)val[0], (int)val[1]);
    }

    private String parseUserList(byte[] val) {
        StringBuilder ret = new StringBuilder();
        int[] user_numbers = sdk.parseUserList(val);

        if(user_numbers != null) {
            for(int i = 0; i < user_numbers.length; i++) {
                ret.append(user_numbers[i]);

                if(i < (user_numbers.length - 1)) {
                    ret.append(", ");
                }
            }
        }
        return ret.toString();
    }

    private String parseDataCount(byte[] val) {
        return String.format(Locale.getDefault(), "데이터 수량: %d / 1000", ByteBuffer.wrap(val).getShort());
    }

    final class AndroidBridge {
        @JavascriptInterface //이게 있어야 웹에서 실행이 가능합니다.
        public void CallAndroid() {
            Toast.makeText(getApplicationContext(), "웹에서 클릭했어요", Toast.LENGTH_SHORT).show();
        }
    }

    private class WebViewClientClass extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private class setWebChromClient extends WebChromeClient {

        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new android.app.AlertDialog.Builder(myApp)
                    .setTitle("AlertDialog")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { result.confirm();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
            return true;
        }
    }

//    @Override
//    public void onBackPressed() {
//
//        if(webView.canGoBack()){
//            webView.goBack();
//        } else {
//            super.onBackPressed();
//        }
//    }

}