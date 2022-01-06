package com.wac.myusbportdemo;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * description :
 * author : Andy.Guo
 * email : Andy.Guo@waclightiong.com.cn
 * data : 2022/1/6
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    UsbSerialPort port = null;

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x11:
                    TextView tv_display = (TextView) findViewById(R.id.tv_display);
                    Bundle bundle = msg.getData();
                    tv_display.setText(tv_display.getText() + bundle.getString("text"));
                    break;
                default:
                    break;
            }
        }
    }


    private MyHandler myHandler = new MyHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mThread.start();

            }
        });

        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (port != null) {
                        port.write(hexToBytes("010A"), 5000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private Thread mThread = new Thread(new Runnable() {
        @Override
        public void run() {

            // Find all available drivers from attached devices.
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            Log.d(TAG, "availableDrivers = " + new Gson().toJson(availableDrivers));
            if (availableDrivers.isEmpty()) {
                return;
            }

            // Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            Log.d(TAG, "connection = " + connection);
            if (connection == null) {
                // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
                return;
            }

            // Read some data! Most have just one port (port 0).
            port = driver.getPorts().get(0);
            try {
                port.open(connection);
                port.setParameters(57600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                Log.d(TAG, "port = " + new Gson().toJson(port));
                while (true) {
                    byte buffer[] = new byte[16];
                    int numBytesRead = port.read(buffer, 5000);
                    Log.d(TAG, "numBytesRead = " + numBytesRead);
                    if (numBytesRead > 0) {
                        Message message = new Message();
                        message.what = 0x11;
                        Bundle bundle = new Bundle();
                        String string = bytesToHexString(buffer, ":");
                        Log.d(TAG,"port result ="+string);
                        bundle.putString("text", new String(buffer, "UTF-8"));
                        message.setData(bundle);
                        myHandler.sendMessage(message);
                        break;
                    }
                }

            } catch (IOException e) {
                // Deal with error.
            } finally {
                try {
                    port.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public static byte[] hexToBytes(String hexStr) {
        if (hexStr == null) return null;
        if (hexStr.length() == 1) {
            hexStr = "0" + hexStr;
        }
        int length = hexStr.length() / 2;
        byte[] result = new byte[length];

        for (int i = 0; i < length; i++) {
            result[i] = (byte) Integer.parseInt(hexStr.substring(i * 2, i * 2 + 2), 16);
        }

        return result;
    }
    public static final char[] HEX_BASIC = "0123456789ABCDEF".toCharArray();
    public static String bytesToHexString(byte[] array, String separator) {
        if (array == null || array.length == 0)
            return "";

//        final boolean sepNul = TextUtils.isEmpty(separator);
        final boolean sepNul = separator == null || separator.length() == 0;
        StringBuilder hexResult = new StringBuilder();
        int ai;
        for (int i = 0; i < array.length; i++) {
            ai = array[i] & 0xFF;
            if (i != 0 && !sepNul) {
                hexResult.append(separator);
            }
            hexResult.append(HEX_BASIC[ai >>> 4]).append(HEX_BASIC[ai & 0x0F]);
        }
        return hexResult.toString();
    }
}
