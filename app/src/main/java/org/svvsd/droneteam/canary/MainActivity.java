package org.svvsd.droneteam.canary;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/////////////////
// Notes
//
// There is only one Activity (this one) that contains all the UI
//  3 classes do all the work
//    Receiver - handles getting data from the XBee
//    Recorder - handles storing data from the XBee, in concert with Receiver
//    Uploader - handles uploading the stored data to the web, in concert with Recorder
//
//  DebugUtils is a class just used for debugging.
//   Try to have all debug messages you want to log use the function DebugUtils.msg(), which writes to the system log.
//   Then in Logcat you can filter for string "canary tweet" and you'll see only those messages.
//   And before releasing the App you can comment out the system log line in it so you don't make noisy system logs
//
//  For testing, feel free to set the uploader server url sUploadUrl to "http://tuckerfoltz.com/canary/canary.php?"
//   Then you can see the results of your server submission at http://tuckerfoltz.com/canary/canarysubmissions.html
//
//  Also for testing, get another XBee hooked up to a laptop, download the Digi XCTU program and send packets to the XBee.
//  And you can use DebugUtils.simulateXBee() to send an XBee string to the receiver and recorder, sample commented out in onCreate() below
//
// Uploading is independent of having an XBee attached.
// When using in the field, you can have Upload and Record on at the same time for real-time uploading.
// Though it is nice if you want to avoid data charges to just upload when on wifi.
//
//  I added some rough UI elements..
//   Record - Upper left circle with arrow in it - toggle recording data on and off
//   Upload - Upper right cloud with up arroud - toggles uplodading data on and off
//     Below of each of them output for data recorded
//   Delete Data - Trash can below record - clears all data in the database
//
//
// Things you need to do to customize this for Canary project
//
// 1) Figure out exactly what the field names are for the XBee data coming in and uploading
// 2) Add the field names to Recorder.java DatabaseHelper.onCreate() data table definition (use INTEGER, TEXT, REAL column types)
// 3) Add the field names appropriately and figure out the code to process the actual received XBee data in Receiver.java processData()
// 4) Set the correct sUploadUrl in Uploader.java (You can keep the tuckerfoltz.com one now for testing if you wish, see note above)
// 5) Set the correct value in Uploader.java uploadDataToServerTask onPostExecute() for what server response indicates a successful upload of the data
// 6) Customize the UI in res/activity_main.xml and MainActivity.java to add UI Fields and add them to monitoring fields for all the XBee data coming in
//     Any display strings you add put in res/values/strings.xml
//     Then it becomes easy to put it in different languages
//      You have a values-<language code> folder for each translated strings
//        res/values-en/strings.xml, res/values-de/strings.xml, etc.
//       You might want to find native speakers in your high school(s) to translate the strings for you
//       It would be fun to have this in multiple languages
//
/////////////////


public class MainActivity extends AppCompatActivity
{
    // XBee specific vendor and product ids;
    private int vendorId = 1027;
    private int productId = 24597;

    // Main objects that do all the work
    private Receiver receiver; // the device that receives the data - the XBee
    private Recorder recorder; // the utility that will store the data and be fetched from when uploading the data
    private Uploader uploader; // the process that will upload data

    // UI state variables
    private Boolean bRecordingOn = false; // whether the recording button is on or not
    private Boolean bUploadingOn = false; // whether the uploading button is on or not


    ///////////////
    // App lifecycle routines (onCreate(), onStart(), onStop()) - for description understanding see https://developer.android.com/guide/components/images/activity_lifecycle.png
    ///////////////

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // keep the screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set up major objects
        recorder = new Recorder(this); // an object to record data from xbee
        uploader = new Uploader(recorder); // an object to upload the data, using the recorder as a data source
        receiver = new Receiver(this, recorder, vendorId, productId); // an object to get data from the xbee, using recorder as a place to

        // set up UI elements

        // set up size of recorded / uploaded data monitoring field
        recorder.setDataCountView((TextView) findViewById(R.id.datacount)); // set where to display recorded data count and display current size
        uploader.setUploadCountView((TextView) findViewById(R.id.uploadcount)); // set where to display uploaded data count and display current size

        // set up any monitor views (for seeing real time data)
        // an example: receiver.processData() sets a field "testfield".
        // this line adds the testfield UI textfield to be filled in with the latest testfield value
        // TODO: 10/20/18 Add monitors for all the XBee data so you can see it as it comes in
        receiver.addMonitor("O3", (TextView) findViewById(R.id.Ozone));
        receiver.addMonitor("H2", (TextView) findViewById(R.id.hydrogen));
        receiver.addMonitor("NH4", (TextView) findViewById(R.id.ammmonia));
        receiver.addMonitor("CO", (TextView) findViewById(R.id.co));
        receiver.addMonitor("CH4", (TextView) findViewById(R.id.methane));
        receiver.addMonitor("Timestamp",(TextView) findViewById(R.id.dataWarnings));




        // a little test code to test out data when there is no XBee to receive data
        //DebugUtils.simulateXBee("hi jim", receiver, recorder); // can be used to simulate an xbee signal

    }

    @Override
    protected void onStart()
    {
        DebugUtils.msg("onStart() making sure listening for data");
        super.onStart();
        // start receiver listening as soon as we start up
        receiver.startListening();


    }

    @Override
    protected void onStop()
    {
        super.onStop();
        DebugUtils.msg("onStop() stopping listening for data");
        receiver.stopListening();

    }


    ///////////////////////////
    // UI routines
    ///////////////////////////
    public void checkWarnings(View view)
    {
        TextView textView = (TextView) findViewById(R.id.gpsWarnings);
        //Control the warnings area of the app
        if (receiver.gpsProblem == 1)
        {
            textView.setText(getString(R.string.gpsWarn));
        }
        else if (receiver.gpsProblem == 0)
        {
            textView.setText(getString(R.string.gpsNoWarn));
        }

        TextView textView2 = (TextView) findViewById(R.id.gpsWarnings);

    }
    // toggleRecord - turns recording on and off
    public void toggleRecord(View view)
    {
        ImageView toggle = (ImageView) findViewById(view.getId());
        if (bRecordingOn) // if recording, stop recording and change to unhighlighted toggle
        {
            DebugUtils.msg("toggleRecord() stopping recording");
            receiver.stopRecording();
            toggle.setImageResource(R.drawable.record);
            bRecordingOn = false;
        }
        else // otherwise start recording and change to highlighted toggle
        {
            DebugUtils.msg("toggleRecord() starting recording");
            receiver.startRecording();
            if (receiver.isRecording())
            {
                toggle.setImageResource(R.drawable.record_highlight);
                bRecordingOn = true;
            }
            else
            {
                screenMessage(R.string.no_record_no_xbee_hint);
            }

        }

    }

    // toggleRecord - turns uploading on and off
    public void toggleUpload(View view)
    {
        ImageView toggle = (ImageView) findViewById(view.getId());
        if (bUploadingOn) // if uploading, stop uploading and change to unhighlighted toggle
        {
            DebugUtils.msg("toggleUpload() stopping uploading");
            uploader.stopUploading();
            toggle.setImageResource(R.drawable.upload);
            bUploadingOn = false;
        }
        else // start recording and change to highlighted toggle
        {
            DebugUtils.msg("toggleUpload() starting uploading");
            uploader.startUploading();
            toggle.setImageResource(R.drawable.upload_highlight);
            bUploadingOn = true;
        }

    }


    // deleteData - deletes all the recorded data
    public void deleteData(View view)
    {
        recorder.eraseData(); // erase the data
        recorder.updateDataCount(); // recalculate the data count
        uploader.updateUploadCount(); // recalculate the upload data count
        screenMessage(R.string.data_erased);
    }


    // screenMessage - sends a temporary message to the screen, from strings defined in res/values/strings.xml - Need to put all strings here, so easy to translate to other languages
    private void screenMessage(int iResourceValueId)
    {
        Toast toast = Toast.makeText(this, getString(iResourceValueId), Toast.LENGTH_SHORT);
        toast.show();
    }

}
