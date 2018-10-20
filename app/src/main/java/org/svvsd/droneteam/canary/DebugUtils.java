package org.svvsd.droneteam.canary;

public class DebugUtils
{
    public static void msg(String sMsg)
    {
        System.out.println("Canary Tweet " + sMsg);
    }

    public static void simulateXBee(String xbeeString, Receiver receiver, Recorder recorder)
    {
        receiver.processData(xbeeString); // simulate processing XBee data string
        receiver.updateMonitors(); // updates ui monitors with data
        recorder.recordData(receiver.lastProcessedData); // record the data
        recorder.updateDataCount(recorder.dataCount); // update the data count
    }
}
