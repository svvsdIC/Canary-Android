package com.example.michelle.projectcanarychordsapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button LetsStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LetsStart = (Button) findViewById(R.id.buttonLetsBegin);
    }

    public void LetsStartButton(View v) {
        Intent goToDataRetrievalActivity = new Intent();
        goToDataRetrievalActivity.setClass(this, DataRetrievalActivity.class);
        startActivity(goToDataRetrievalActivity);
    }
}
