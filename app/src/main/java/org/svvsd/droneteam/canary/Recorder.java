package org.svvsd.droneteam.canary;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.TextView;

////////////////////////////////////
// Notes
//
// Recorder handles storing data from the XBee, in concert with Receiver, and uploading the data in concert with Uploader
//   Optionally displays the size of all the recorded data
//
// Things that need customizing
//   Add the XBee field names to DatabaseHelper.onCreate() data table definition (use INTEGER, TEXT, REAL column types)
//
// Constructor
//   Recorder(Context)
//
// Public variables
//   dataCount - how much recorded data there is (number of rows in database)
//
// Public Methods
//   recordData(ContentValues) - records the passed in ContentValues into the database. ContentValues keys are the database column names to update
//   getNonUploadedData() - returns ContentValues of 1 row of non-uploaded data. Will have zero size if there are no non-uploaded data rows. Called by Uploader to get a row of data to upload.
//   markDataUploaded(dataId) - marks the data row of dataId as uploaded by setting a timestamp value in the uploaded column. Used by Uploader to mark data as uploaded when it has been successfully sent to the server
//   getRecordedDataSize() - gets the current size of the stored data (number of data rows stored in the database)
//   getUploadedDataSize() - gets the current size of the uploaded stored data (number of data rows stored in the database that have an uploaded value set)
//   eraseData() - erases all the stored data
//   setDataCountView(TextView) - sets the UI field to update with size of data, and does initial calculation of data size
//   updateDataCount() - gets current actual data size, and updates UI field with it. Use once at beginning.
//   updateDataCount(long) - updates UI field with value of long (formatted). Use for ongoing updates once initialized
//
///////////////////////////

public class Recorder
{
    public long dataCount = 0; // how much recorded data there is
    private static final String DATABASE_NAME = "canary.db";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;
    private TextView dataCountView = null;
    private NumberFormat numberFormat = NumberFormat.getInstance(); // get local number formatting scheme

    public Recorder(Context context)
    {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    // setDataCountView(TextView) - sets the UI field to update with size of data, and does initial calculation of data size
    public void setDataCountView(TextView textView)
    {
        dataCountView = textView;
        updateDataCount();
    }

    // updateDataCount() - gets current actual data size, and updates UI field with it
    public void updateDataCount()
    {
        updateDataCount(getRecordedDataSize());
    }

    // updateDataCount(long) - updates UI field with value of long (formatted). Use for ongoing updates once initialized
    public void updateDataCount(long count)
    {
        if (dataCountView != null)
        {
            dataCountView.setText(numberFormat.format(count));
        }
        dataCount = count;
    }

    // recordData(ContentValues) - records the passed in ContentValues into the database. ContentValues keys are the database column names to update
    public long recordData(ContentValues contentValues)
    {
        long dataId = insertIntoTable("canarydata", contentValues);
        dataCount += 1;
        return dataId;
    }

    // getNonUploadedData() - returns ContentValues of 1 row of non-uploaded data. Will have zero size if there are no non-uploaded data rows. Called by Uploader to get a row of data to upload.
    public ContentValues getNonUploadedData()
    {
        ContentValues contentValues = new ContentValues();
        Cursor cursor = db.rawQuery("select * from canarydata where uploaded is null limit 1", null);
        if (cursor.getCount() > 0)
        {
            cursor.moveToPosition(0);
            DatabaseUtils.cursorRowToContentValues(cursor, contentValues);
        }
        cursor.close();
        return contentValues;
    }

    // markDataUploaded(dataId) - marks the data row of dataId as uploaded by setting a timestamp value in the uploaded column. Used by Uploader to mark data as uploaded when it has been successfully sent to the server
    public void markDataUploaded(long dataId)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("uploaded", new Date().getTime());
        updateTable("canarydata", contentValues, "dataId=" + dataId);
    }

    // getRecordedDataSize() - gets the current size of the stored data (number of data rows stored in the database)
    public long getRecordedDataSize()
    {
        return DatabaseUtils.queryNumEntries(db, "canarydata");
    }

    // getUploadedDataSize() - gets the current size of the uploaded stored data (number of data rows stored in the database that have an uploaded value set)
    public long getUploadedDataSize()
    {
        Cursor cursor = db.rawQuery("select count(*) from canarydata where uploaded is not null", null);
        long size = 0;
        if (cursor.getCount() > 0)
        {
            cursor.moveToPosition(0);
            size = cursor.getLong(0);
        }
        cursor.close();
        return size;
    }

    // eraseData() - erases all the stored data
    public void eraseData()
    {
        db.execSQL("delete from canarydata");
    }


    //////////////
    // database routines
    //////////////
    public class DatabaseHelper extends SQLiteOpenHelper
    {

        DatabaseHelper(Context context)
        {
            // calls the super constructor, requesting the default cursor factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        // create the database
        public void onCreate(SQLiteDatabase db)
        {
            // table to record data and keep track of what has been uploaded
            //   only required is dataId (primary key, automatically generated, you do not set) and uploaded (timestamp), which are used for uploading data
            //   column names should be the same name as the field names you plan on uploading, and the same names as the receiver process data and receiver monitors
            db.execSQL("CREATE TABLE IF NOT EXISTS canarydata (dataId INTEGER PRIMARY KEY AUTOINCREMENT, uploaded INTEGER, testfield TEXT, " +
                    "temperature REAL, pressure INTEGER, humidity INTEGER, CO INTEGER, H2 INTEGER, NH4 INTEGER, CH4 INTEGER, O3 INTEGER, " +
                    "Lidar INTEGER, Latitude REAL, Longitude REAL, Altitude REAL, Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }


        @Override
        // put any changes needed when migrating the data from database version to another, for backward compatibility
         public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {

        }
    }



    private long insertIntoTable(String sTable, ContentValues contentValues)
    {
        long rowId = db.insert(sTable, null, contentValues);

        return rowId;
    }

    private void updateTable(String sTable, ContentValues contentValues, String sWhere)
    {
        db.update(sTable, contentValues, sWhere, null);
    }


}
