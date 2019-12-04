package org.svvsd.droneteam.canary;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

////////////////////////////////////
// Notes
//
// Uploader handles uploading the stored data to the web, in concert with Recorder
//   Optionally displays the size of all uploaded stored data
//
// Things that need customizing
//  1) Set the correct upload url sUploadUrl (You can keep the tuckerfoltz.com one now for testing if you wish, see note above)
//  2) Set the correct value in uploadDataToServerTask onPostExecute() for what server response indicates a successful upload of the data
//
// Constructor
//   Uploader(Recorder) - Recorder is the dataSource
//
// Public Methods
//   startUploading() - starts uploading data
//   stopUploading() - stops uploading data
//   isUploading() - returns whether currently uploading data
//   setUploadCountView(TextView) - set the UI field to update with the current size of data uploaded, and sets the UI field with the current size
//   updateUploadCount() - get the size of the data already uploaded and update the UI field with it. Called initially
//   updateUploadCount(long) - updates the UI field with the value of long, formatted. Used ongoing
//
///////////////////////////

public class Uploader
{
    // TODO: Set correct name of upload URL
    private String sUploadUrl = "http://tuckerfoltz.com/canary/canary.php?"; // the URL to upload
    private Boolean bUploading = false; // whether uploading non-uploaded data or not
    private Recorder dataSource;
    private long uploadCount = 0;
    private TextView uploadCountView = null;
    private NumberFormat numberFormat = NumberFormat.getInstance(); // get local number formatting scheme
    private Timer uploadDataTimer = new Timer(); // timer for launching thread to see if there is any data to upload

    public Uploader(Recorder dataSource) // general constructor, passing in a dataSource to use for uploading
    {
        this.dataSource = dataSource;
    }

    // setUploadCountView(TextView) - set the UI field to update with the current size of data uploaded, and sets the UI field with the current size
    public void setUploadCountView(TextView textView)
    {
        uploadCountView = textView;
        updateUploadCount();
    }

    // updateUploadCount() - get the size of the data already uploaded and update the UI field with it. Called initially
    public void updateUploadCount()
    {
        if (dataSource != null)
        {
            updateUploadCount(dataSource.getUploadedDataSize());
        }
    }

    // updateUploadCount(long) - updates the UI field with the value of long, formatted. Used ongoing
    public void updateUploadCount(long count)
    {
        if (uploadCountView != null)
        {
            uploadCountView.setText(numberFormat.format(count));
        }
        uploadCount = count;
    }

    // startUploading() - starts uploading data
    public void startUploading()
    {
        bUploading = true;
        scheduleUploadData();
    }

    // stopUploading() - stops uploading data
    public void stopUploading()
    {
        bUploading = false;
    }

    // isUploading() - returns whether currently uploading data
    public Boolean isUploading()
    {
        return bUploading;
    }

    ///////
    // Upload data routines
    //
    // There is probably much more elegant solution to it all, but this works
    // Here's how it works currently
    // 1) scheduleUploadData() is called, which schedules a program thread uploadDataTask to launch at some small amount of time in the future like 10ms
    // 2) uploadDataTask executes asynchronous task sendDataToServerTask, which goes off on a separate thread (doInBackground()) from the main UI thread (so it does not hang / wait)
    // 3) sendDataToServerTask sees if there is any non-uploaded data, and if there is sends the data to the server
    //    When done sending all the data, sendDataToServerTask returns to main UI thread (onPostExecute()), and ends with a call to scheduleUploadData() to set up the next uploading data run before ending the task
    //
    ///////

    // scheduleUploadData() - schedules a program thread uploadDataTask to launch at some small amount of time in the future like 10ms
    private void scheduleUploadData()
    {
        if (bUploading) // if we are supposed to be uploading, schedule the next uploading
        {
    //       DebugUtils.msg("scheduleUploadData() scheduling next data upload");
            uploadDataTimer.schedule(new uploadDataTask(), 10);
        }
    }

    // uploadDataTask() - executes asynchronous task sendDataToServerTask, which goes off on a separate thread (doInBackground()) from the main UI thread (so app does not hang / wait)
    class uploadDataTask extends TimerTask
    {
        public void run()
        {
            new uploadDataToServerTask().execute(sUploadUrl);

        }

    }

    // sendDataToServerTask (DataSource)
    //    In the background (separate thread), see if there is any un-uploaded data, and send it to the server if there is
    //    When done processing the data, getXBeeDataTask returns to main UI thread (onPostExecute()), and ends with a call to scheduleCheckForData() to set up the next check for data run before ending the task
    private class uploadDataToServerTask extends AsyncTask<String, Void, String>
    {
        private long dataId = 0; // the data id being uploaded for marker as uploaded

        @Override
        protected String doInBackground(String... urls)
        {
            String sServerResponse = ""; // default response passed on to onPostExecute()

            String sUrl = urls[0];

            ContentValues contentValues = dataSource.getNonUploadedData();

            String sContentValues = "";
            String sValue = "";
            for (String sKey : contentValues.keySet())
            {
                if (sKey.contentEquals("dataId"))
                {
                    dataId = contentValues.getAsLong(sKey); // save for marking in onPostExecute()
                }
                else if (!sKey.contentEquals("uploaded")) // these are internal and not meant to be sent
                {
                    DebugUtils.msg("key is " + sKey);
                    sValue = contentValues.getAsString(sKey);
                    if (sValue.length()>0) // have some data value to send
                    {
                        if(sContentValues.length()>0)
                            sContentValues += "&";
                        sContentValues += sKey + "=" + sValue;
                    }
                }

            }
            if (sContentValues.length() > 0) // have some values to send, try to send them
            {
                DebugUtils.msg("non-uploaded data is " + contentValues.toString());
                // convert to real url
                URL url = null;
                try {
                    url = new URL(sUrl +URLEncoder.encode( sContentValues));
                } catch (Exception e) {
                    DebugUtils.msg("sendDataToServerTask doInBackground() unable to format url " + sUrl + ", " + e.toString());
                }

                // if have good url, send it off and get response
                if (url != null) {
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        byte[] buffer = new byte[1024];
                        in.read(buffer);
                        sServerResponse = new String(buffer);
                        //DebugUtils.msg("sendDataToServerTask doInBackground() server response is " + sServerResponse);
                        in.close();
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        DebugUtils.msg("sendDataToServerTask doInBackground() unable to send to server " + e.toString());
                    }

                }
            }

            return sServerResponse;
        }


        // process things back on the main thread
        protected void onPostExecute(String sServerResponse)
        {
            // TODO: Set the value for what server response indicates successfully uploaded
            if (sServerResponse.contains("OK")) // test for whatever response indicates successful transmission
            {
                DebugUtils.msg("sendDataToServerTask onPostExecute() successfully sent data, marking as uploaded");

                dataSource.markDataUploaded(dataId); // mark the data as uploaded
                updateUploadCount(uploadCount + 1); // upload the progress counter
            }

            scheduleUploadData(); // set up the next upload data run
        }

    }

}
