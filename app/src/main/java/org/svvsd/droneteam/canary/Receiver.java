package org.svvsd.droneteam.canary;

import android.content.ContentValues;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.widget.TextView;

import com.digi.xbee.api.connection.android.AndroidXBeeInterface;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

////////////////////////////////////
// Notes
//
// Receiver reads data from the XBee and processes the data
//   Optionally displays the received data to "monitor" ui text fields (see addMonitor())
//   Optionally records the data to a Recorder (see startRecording())
//
// Things that need customizing
//   processData(String sData) - Need to split up the XBee sData string into the correct field value pairs
//
// Constructor
//   Receiver(Context, Recorder, VendorId, ProductId)
//
// Public variables
//   vendorId - the vendor ID of the XBee interface attached
//   productId - the product ID of the XBee interface attached
//
// Public Methods
//   startListening() - begins listening and processing data
//   stopListening() - stops listening and processing data
//   isListening() - whether the receivers is currently listening and processing data
//   startRecording(Recorder) - begins recording data to the Recorder database
//   stopRecording() - stop recording data to the Recorder database
//   isRecording() - whether the receiver is recording the data to the Recorder database
//   addMonitor(String fieldName,TextView) - display processed fieldName values to a given UI textview
//   clearMonitor(String fieldName) - stop displaying processed fieldName value to a given UI
//   clearAllMonitors() - stop displaying any field values
//   updateMonitors(ContentValues) - updates monitors with latest values
//
///////////////////////////


public class Receiver
{
    // xbee device info - keeping public so you can identify new device ids for auto launching app with the usb filter
    public int vendorId = 0; //1027;
    public int productId = 0; //24597;
    public ContentValues lastProcessedData = new ContentValues();
    public int gpsProblem = 0;
    public Date date = new Date();

    private Context context;


    private AndroidXBeeInterface xBeeInterface = null; // xbee connection interface
    private Timer checkForDataTimer = new Timer(); // timer for launching thread to check for data
    private Boolean bListening = false; // whether the receiver is listening for new data coming in
    private Boolean bRecording = false; // whether the receiver is recording the new data coming in
    private HashMap<String, TextView> monitors = new HashMap<String, TextView>(); // monitoring field views to update when listening


    private Recorder recorder = null;

    public Receiver(Context context) // general constructor
    {
        this.context = context;
    }

    public Receiver(Context context, Recorder recorder, int vendorId, int productId) // constructor if you want to look for only a specific device
    {
        this.context = context;
        this.recorder = recorder;
        this.vendorId = vendorId;
        this.productId = productId;
    }




    public void startListening()
    {
        DebugUtils.msg("startListening() starting listening");
        if (xBeeInterface == null) // get an interface if we don't have it
        {
            xBeeInterface = getXBeeInterface();
        }
        if (xBeeInterface != null) // have an interface
        {
            bListening = true;
            scheduleCheckFoData();
        }
    }


    public void stopListening()
    {
        DebugUtils.msg("stopListening() stopping listening");
        bListening = false;
        if (bRecording)
        {
            stopRecording();
        }
        if (xBeeInterface != null && xBeeInterface.isOpen())
        {
            xBeeInterface.close();
        }
    }

    public Boolean isListening()
    {
        return bListening;
    }

    public void startRecording()
    {
        if (!bListening)
        {
            startListening();
        }
        if (bListening)
        {
            bRecording = true;
        }
    }

    public void stopRecording()
    {
        bRecording = false;
    }

    public Boolean isRecording()
    {
        return bRecording;
    }

    public void addMonitor(String fieldName, TextView textView)
    {
        monitors.put(fieldName, textView);
    }

    public void clearMonitor(String fieldName)
    {
        monitors.remove(fieldName);
    }

    public void clearAllMonitors()
    {
        monitors.clear();
    }

    public void updateMonitors()
    {
        // go through any added monitors if any and update their values with the last processed data if an value exists for the field
        // note you have to do this here because you usually can only update UI views on the main UI thread, whihc we are on by the time we get to onPostExecute
        TextView textView;
        for (String sKey : monitors.keySet())
        {
            if (lastProcessedData.containsKey(sKey))
            {
                textView = monitors.get(sKey);
                textView.setText(lastProcessedData.getAsString(sKey));

            }
            if(sKey.equals("Timestamp"))
            {
                textView = monitors.get(sKey);
                textView.setText(date.toString());
            }
        }

    }


    ///////////////
    // XBee routines
    ///////////////

    // processData() - here is where you take the XBee received data string and turn into usable value you can put into the recorder or use for displaying on monitors
    public ContentValues processData(String sData)
    {

        //make timestamp for the last time data was processed
        date = new Date();
        // TODO: 10/18/18 - Split the received XBee data string into field values.
        //   Field names should be the same as Recorder database column names you want to record the values to
        //   Field names should be the same as the monitor field names for monitoring
        ContentValues contentValues = new ContentValues();
        sData = sData.replaceAll("[^0-9|.,]", "");
        DebugUtils.msg("sData at processData is " + sData);
        //make sure the data starts at the right point
        String[] dataSplit = sData.split("[|]+");
        //locate start of string
        contentValues.put("testfield", ("full data" + sData));
        for(int i = 0; i<4; i++)
        {
            String[] sensorSplit = dataSplit[i].split(",");
            if(sensorSplit[0].contentEquals("1"))
            {
                //sensorID 1
                contentValues.put("temperature", Float.parseFloat(sensorSplit[1]));
                contentValues.put("pressure", Integer.parseInt(sensorSplit[2]));
                contentValues.put("humidity", Integer.parseInt(sensorSplit[3]));
            }
            else if(sensorSplit[0].contentEquals("2"))
            {
                //sensorID 2
                contentValues.put("CO", Integer.parseInt(sensorSplit[1]));
                contentValues.put("H2", Integer.parseInt(sensorSplit[2]));
                contentValues.put("NH4", Integer.parseInt(sensorSplit[3]));
                contentValues.put("CH4", Integer.parseInt(sensorSplit[4]));
                contentValues.put("O3", Integer.parseInt(sensorSplit[5]));
            }
            else if(sensorSplit[0].contentEquals("3"))
            {
                //semsorID 3
                contentValues.put("Lidar", Integer.parseInt(sensorSplit[1]));
            }
            else if(sensorSplit[0].contentEquals("4"))
            {
                if(sensorSplit.length>1) {
                    contentValues.put("Latitude", Float.parseFloat(sensorSplit[1]));
                    contentValues.put("Longitude", Float.parseFloat(sensorSplit[2]));
                    contentValues.put("Altitude", Float.parseFloat(sensorSplit[3]));
                    gpsProblem = 0;
                }
                else {
                    contentValues.put("Latitude", "-1");
                    contentValues.put("Longitude", "-1");
                    contentValues.put("Altitude", "-1");
                    gpsProblem = 1;
                }
            }
            DebugUtils.msg("contentValues() data parsed is" + contentValues.toString());
        }



        lastProcessedData = contentValues;

        return contentValues;

    }

    // getXBeeInterface() - gets the xBeeInterface if it is attached to the USB port
    private AndroidXBeeInterface getXBeeInterface()
    {
        AndroidXBeeInterface myXBeeInterface = null;
        DebugUtils.msg("getXBeeInterface() looking for XBee");

        // get the interface
        // get hashmap of all the USB devices attached

        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList != null)
        {
            DebugUtils.msg("getXBeeInterface() found USB Devices");
            int iDeviceVendorId = 0;
            int iDeviceProductId = 0;
            // go through the hashmap values and see if any of the USB devices match the xbee device we are looking for
            for (UsbDevice device : deviceList.values())
            {
                if (myXBeeInterface == null) // if we do not have an interface, keep checking
                {
                    iDeviceVendorId = device.getVendorId();
                    iDeviceProductId = device.getProductId();
                    DebugUtils.msg("getXBeeInterface found a USB device vendorId " + iDeviceVendorId + " productId " + iDeviceProductId + " " + device.toString());

                    // if the vendor and product ids match, try to get the interface.
                    // need to match because the underlying library can get an XBee interface for a non-XBEE device which then crashes
                    if (iDeviceVendorId == vendorId && iDeviceProductId == productId)
                    {
                        DebugUtils.msg("getXBeeInterface() attempting to get interface");
                        try
                        {
                            myXBeeInterface = new AndroidXBeeInterface(context, 9600, device);

                        }
                        catch (Exception e)
                        {
                            DebugUtils.msg("getXBeeInterface() did not get interface " + e.toString());

                        }
                    }
                }

            }
        }
        else
        {
            DebugUtils.msg("getXBeeInterface() did not find any USB Devices");
        }

        if (myXBeeInterface == null)
        {
            DebugUtils.msg("getXBeeInterface() could not get any XBee Interface");
        }
        else
        {
            DebugUtils.msg("getXBeeInterface() got XBee Interface " + myXBeeInterface.toString());
        }

        return myXBeeInterface;
    }

    ///////
    // Check for data routines
    //
    // There is probably much more elegant solution to it all, but this works
    // Assumes an existing XBee Interface
    // Here's how it works currently
    // 1) scheduleCheckForData() is called, which schedules a program thread checkForDataTask to launch at some small amount of time in the future like 10ms
    // 2) checkForDataTask executes asynchronous task getXBeeDataTask, which goes off on a separate thread (doInBackground()) from the main UI thread (so it does not hang / wait)
    // 3) getXBeeDataTask in the background opens the connection to the XBee if necessary, and reads any data in the XBee buffer
    //    When done reading data, getXBeeDataTask returns to main UI thread (onPostExecute()), and ends with a call to scheduleCheckForData() to set up the next check for data run before ending the task
    //
    ///////

    // scheduleCheckForData() - schedules a program thread checkForDataTask to launch at some small amount of time in the future like 10ms
    private void scheduleCheckFoData()
    {
        if (bListening) // if we are supposed to be listening, schedule the next listening
        {
            //       DebugUtils.msg("scheduleCheckForData() scheduling next check for data");
            checkForDataTimer.schedule(new checkForDataTask(), 10);
        }
    }

    // checkForDataTask() - executes asynchronous task getXBeeDataTask, which goes off on a separate thread (doInBackground()) from the main UI thread (so app does not hang / wait)
    class checkForDataTask extends TimerTask
    {
        public void run()
        {
            new getXBeeDataTask().execute(xBeeInterface);

        }

    }

    // getXBeeDataTask (XBeeInterface)
    //    In the background (separate thread), opens the connection to the XBee if necessary and reads all data available from the passed in XBee interface
    //    When done processing the data, getXBeeDataTask returns to main UI thread (onPostExecute()), and ends with a call to scheduleCheckForData() to set up the next check for data run before ending the task
    private class getXBeeDataTask extends AsyncTask<AndroidXBeeInterface, Void, String>
    {
        private int usefulLength = 0;
        @Override
        protected String doInBackground(AndroidXBeeInterface... xbee)
        {
            AndroidXBeeInterface myXBeeInterface = xbee[0];
            //    DebugUtils.msg("getXBeeDataTask doInBackground() trying to open XBee Connection");
            if (myXBeeInterface == null)
            {
                //        DebugUtils.msg("getXBeeDataTask doInBackground() No XBee Interface");
            }
            else
            {
                String sData = "";
                ContentValues processedData = new ContentValues();
                try
                {
                    if (!myXBeeInterface.isOpen())
                    {
                        myXBeeInterface.open();
                    }
                    byte[] buffer = new byte[1024];
                    int iLength;



                    //       DebugUtils.msg("getXBeeDataTask doInBackground() reading XBee data");
                    while ((iLength = myXBeeInterface.readData(buffer)) > 0)
                    {
                        usefulLength = usefulLength + iLength;
                        DebugUtils.msg("getXBeeDataTask doInBackground() read data length " + usefulLength);
                        sData = sData + new String(buffer,0,iLength); //just keep adding to sData until we reset it
//                        sData = new String(buffer);
                        DebugUtils.msg("getXBeeDataTask doInBackground() data is before change " + sData);
                    }

                }
                catch (Exception e)
                {
                    //         DebugUtils.msg("getXBeeDataTask doInBackground() problem getting XBee data " + e.toString());
                }
                if(sData.length()>0){
                    processedData = processData(sData);}
                if (bRecording && processedData.size() > 0) // need to record and have good data
                {
                    recorder.recordData(processedData);
                }
            }
            return "OK";
        }


        // process things back on the main thread
        protected void onPostExecute(String sStatus)
        {
            updateMonitors();
            recorder.updateDataCount(recorder.dataCount);


            //       DebugUtils.msg("getXBeeDataTask onPostExecute() calling scheduleCheckForData()");

            scheduleCheckFoData(); // set up the next check for data run
        }

    }



}
