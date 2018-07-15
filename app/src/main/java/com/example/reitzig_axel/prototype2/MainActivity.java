package com.example.reitzig_axel.prototype2;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import java.util.HashMap;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.digi.xbee.api.connection.android.AndroidXBeeInterface;

public class MainActivity extends AppCompatActivity {

    //private static final String ACTION_USB_PERMISSION =
    //"com.android.example.USB_PERMISSION";
    private static final String TAG = "MainActivity";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication; this is a filler!!!
                            Log.d(TAG, "THIS IS A FILLER " + device);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
            */
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // To get the vendor and product ID

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        UsbDevice device = deviceList.get("deviceName");
        System.out.println("jimusb " + deviceList.toString());
        TextView devicetextView = (TextView) findViewById(R.id.mytextview);
        devicetextView.setText("device list " + deviceList.toString());

        AndroidXBeeInterface xBeeInterface = new AndroidXBeeInterface(getApplicationContext(), 9600, device);

        try
        {
            xBeeInterface.open();
            byte[] buffer = new byte[1024];
            int len;

            //msg("embedBitmap processing input stream");
            while ((len = xBeeInterface.readData(buffer)) > 0)
            {

                for (int i = 0; i < len; i++)
                {
                    // process data reading in
                    //System.out.println("Got Data");
                    TextView textView = (TextView) findViewById(R.id.mytextview);
                    textView.setText("got data");

                }
            }
            xBeeInterface.close();
        }
        catch (Exception e)
        {
            // code to debug and handle errors
            System.out.println("problem: " + e);
            TextView textView = (TextView) findViewById(R.id.mytextview);
            textView.setText("problem: " + e);
        }
        System.out.println("Done reading data");


        //mUsbReceiver.getResultCode(); // FILLER LINE TO RESOLVE AN ERROR---REPLACE LATER

/*
        // Set up behavior for app init
        DatabaseUtils db = new DatabaseUtils(getApplicationContext());
        // Will have to change this so that it accepts serial data for the following sensors
        // and assigns them as the parameters; most likely will be in a loop so that the action is
        // repeated.
        db.replaceIntoTable("Atmospheric_Data", new String[]{"psi", "humidity", "temp", "gasVOC", "barometric_psi", "altitude", "ambient_temp"},
                new String[]{"zebra", "14"}); // CHANGE THE INPUT DATA HERE... change zebra and 14
        db.close();
        Cursor query = db.selectFromTable("psi", "humidity", "psi='zebra'"); // ACCOMMODATE FOR CHANGE IN PARAMETERS; get rid of zebras...
        //String sPublicBasePath = db.getQueryResult(query, "temp", 0);
        query.close();
        db.close();*/

    }

}