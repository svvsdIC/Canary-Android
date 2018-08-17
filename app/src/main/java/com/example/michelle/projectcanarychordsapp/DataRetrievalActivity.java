package com.example.michelle.projectcanarychordsapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class DataRetrievalActivity extends AppCompatActivity {

    Button startDataInput;
    Button stopDataInput;
    TextView dataOutputHost;
    String output = "";
    int delay = 0; // delay for 0 sec.
    int period = 1000; // repeat every 10 sec.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_retrieval);
        startDataInput = (Button) findViewById(R.id.InputDataButton);
        stopDataInput = (Button) findViewById(R.id.stopInputDataButton);
        dataOutputHost = (TextView) findViewById(R.id.outputDataHereTextview);

        // PLACE THE SERIAL CONNECTION CODE HERE
        //
        //
        //
        //
        //
        //       ... as in setup code
        //
        //
        //
        //
        //
        /////////////////////////////////////////

    }

    public void InputStreamControl(View v) {
        if(v.equals(startDataInput)){
            startDataInput.setVisibility(startDataInput.INVISIBLE);
            stopDataInput.setVisibility(stopDataInput.VISIBLE);
            ///////// Start data stream ////////
            //
            //
            //
            //    Instantiate Xbee connection
            //
            //
            //
            ////////////////////////////////////

            // ************ Filler *************
            output = output + "\n Once we get the serial data, each data point will be printed out here!";
            dataOutputHost.setText(output);
            // *********************************
        }
        if(v.equals(stopDataInput)) {
            stopDataInput.setVisibility(stopDataInput.INVISIBLE);
            startDataInput.setVisibility(startDataInput.VISIBLE);
            ////////// End data stream /////////
            //
            //
            //
            //       Close Xbee connection
            //
            //
            //
            ////////////////////////////////////

            // ************ Filler *************
            dataOutputHost.setText("Printing data will cease.");
            // *********************************

        }

    }

}
