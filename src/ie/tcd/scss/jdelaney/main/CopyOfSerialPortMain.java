package ie.tcd.scss.jdelaney.main;

import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm;
import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm.SerialReader;
import ie.tcd.scss.jdelaney.framing.*;

public class CopyOfSerialPortMain {
	static final byte type_rtp 		= '1';
	static final byte type_rtsp 	= '2';
	static final byte type_ack 		= '3';
	static final byte type_message 	= '4';

	// Debugging constants
	static final int NONE 			= 0;
	static final int NORMAL 		= 1;
	static final int VERBOSE 		= 2;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        Framing f = new Framing();
        f.setDebugLevel(NORMAL);
        
        TwoWaySerialComm serialConnection;

        // Start a thread to de-frame the data coming from the serial port
        (new Thread(f)).start();

        /*
        try
        {
	        f.addToBuffer(blah, 4);
	        f.addToBuffer(blah, 1);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        	System.out.println("Too big for the buffer");
        }*/

        try
        {
        	serialConnection = new TwoWaySerialComm(f);
        	serialConnection.connect("COM4");
        	serialConnection.setDebugLevel(NONE);
        	
        	while (true)
        	{
        		// Test sending an RTSP play command to the embedded device
        		//byte[] request = "PLAY";
/*
        		PLAY rtsp://audio.example.com/audio RTSP/1.0
        			CSeq: 835
        			Session: 12345678
        			Range: npt=10-15
*/
        		Thread.sleep(2000);
        		
                String blah = "test";
                //byte[] blah = {'a', 'b', 'c', 'd', 'e'};
        		serialConnection.sendToSerialPort(f.getFramedData( blah.getBytes(), type_rtp));
        	}
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

}
