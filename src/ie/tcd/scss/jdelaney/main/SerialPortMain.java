package ie.tcd.scss.jdelaney.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm;
import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm.SerialReader;
import ie.tcd.scss.jdelaney.framing.*;

public class SerialPortMain {
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
        //Framing f = new Framing("COM12");
        Framing f = new Framing("COM4");
        f.setDebugLevel(NORMAL);
        
        // Start a thread to de-frame the data coming from the serial port
        (new Thread(f)).start();

        /*try {
			Thread.sleep(5000);
        	while(true)
        	{
				Thread.sleep(2000);
				System.out.println("\nStart Loop");
				
				byte[] bytes = new byte[1];
				bytes[0] = 'a';
				
			
				try {
					f.send(bytes, type_message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        
        try 
        {
		    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));


			System.out.println("**************************");
			System.out.println("* RTSP ARDUINO INTERFACE *");

		    String s = "1";
			while(!s.equals("x"))
			{
				System.out.println("**************************");
				System.out.println("\nSelect Your Query:\n" +
						"S SETUP\n" +
						"P PLAY\n"+
						"E PAUSE\n" +
						"T TEARDOWN\n" +
						"D DESCRIBE\n"+
						"x Exit\n");

				s = bufferRead.readLine();
				System.out.println("You chose " + s);
		    
				try
				{
					if(s.equals("S"))
				    {
			                String rtspRequest =   "SETUP rtsp://example.com/ecg.ps RTSP/1.0\r\nCSeq: 302\r\nTransport: RTP/AVP;unicast;client_port=4588-4589";
	
	        				//byte[] blah = {'a', 'b', 'c', 'd', 'e'};
	                		f.send(rtspRequest.getBytes(), type_rtsp);
				    }
					else
					if(s.equals("P"))
				    {
	        				String rtspRequest = "PLAY rtsp://example.com/ecg.ps RTSP/1.0\r\nCSeq: 833\r\nSession: 123\r\nRange: npt=10-15";   
	
	                		f.send(rtspRequest.getBytes(), type_rtsp);
				    }
					else
					if(s.equals("E"))
				    {
	        				String rtspRequest = "PAUSE rtsp://example.com/ecg.ps RTSP/1.0\r\nCSeq: 835\r\nSession: 123\r\n";   
	
	                		f.send(rtspRequest.getBytes(), type_rtsp);
				    }
					else
					if(s.equals("D"))
				    {
	        				String rtspRequest = "DESCRIBE rtsp://example.com/ecg.ps RTSP/1.0\r\nCSeq: 312\r\nAccept: application/sdp, application/rtsl, application/mheg";   
	
	                		f.send(rtspRequest.getBytes(), type_rtsp);
				    }
					else
					if(s.equals("T"))
				    {		
				           
	        				String rtspRequest = "TEARDOWN rtsp://example.com/ecg.ps RTSP/1.0\r\nCSeq: 892\r\nSession: 123";   
	
	                		f.send(rtspRequest.getBytes(), type_rtsp);
				    }
				}
		        catch ( Exception e )
		        {
					System.out.println("Failed to send data");
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
				
    		//Thread.sleep(5000);
    		//Thread.sleep(20000);

        		// Test sending an RTSP play command to the embedded device
        		//byte[] request = "PLAY";

        		//PLAY rtsp://audio.example.com/audio RTSP/1.0
        		//	CSeq: 835
        		//	Session: 12345678
        		//	Range: npt=10-15


        		//f.send(blah.getBytes(), type_rtp);
        	}
        }
        catch (IOException e)
        {
        	System.out.println("IO Exception");
        }

}
	

}
