package ie.tcd.scss.jdelaney.serial;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import ie.tcd.scss.jdelaney.framing.*;

public class TwoWaySerialComm
{
	// Debugging constants
	final int NONE 			= 0;
	final int NORMAL 		= 1;
	final int VERBOSE 		= 2;
	int debugLevel 	= NORMAL;

	Framing framing;
	
	OutputStream out;
	
	public TwoWaySerialComm(Framing f)
    {
        //super();
		framing = f;
    }


    public void connect ( String portName ) throws Exception
    {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() )
        {
            if(debugLevel==NORMAL) System.out.println("Error: Port is currently in use");
        }
        else
        {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
            
            if ( commPort instanceof SerialPort )
            {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
                
                InputStream in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                
                (new Thread(new SerialReader(in))).start();
                (new Thread(new SerialWriter(out))).start();
            }
            else
            {
            	if(debugLevel==NORMAL) System.out.println("Error: Only serial ports are handled by this example.");
            }
        }     
    }

    public void sendToSerialPort(byte[] b)
    {
        try {
			out.write(b);

			if(debugLevel==NORMAL)
			{
				System.out.println("Sending: " + BytePrinter.printBytes(b));
				System.out.println("(ascii): " + BytePrinter.printAscii(b));
			}
		} catch (IOException e) {
			System.out.println("Failed to send data");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /** */
    public class SerialReader implements Runnable 
    {
        InputStream in;
        
        public SerialReader ( InputStream in )
        {
            this.in = in;
        }

        public void run ()
        {
            byte[] buffer = new byte[1024];
            int len = -1;
            try
            {
                while ( ( len = this.in.read(buffer)) > -1 )
                {
                	if(len>0)
                	{
                		if(debugLevel>=NORMAL) System.out.print(new String(buffer,0,len));
	
	                    //framing.addToBuffer(buffer, len);
	                    try
	                    {
	                    	if(debugLevel==VERBOSE) System.out.println("Adding data to the framing object of length=" + len);
	                    	framing.addToBuffer(buffer, len);
	                    }
	                    catch (ArrayIndexOutOfBoundsException e)
	                    {
	                    	if(debugLevel==VERBOSE) System.out.println("Too big for the buffer");
	                    }
                	}
                }
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }            
        }
    }
    

    /** */
    public static class SerialWriter implements Runnable 
    {
        OutputStream out;
        
        public SerialWriter ( OutputStream out )
        {
            this.out = out;
        }
        
        public void run ()
        {
            try
            {                
                int c = 0;
                while ( ( c = System.in.read()) > -1 )
                {
                	//System.out.println("blah");
                    this.out.write(c);
                }                
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }            
        }
        

    }
    
    
	public boolean setDebugLevel(int level)
	{
		if((level==NORMAL)||(level==VERBOSE)||(level==NONE))
		{
			debugLevel = level;
			return true;
		}
		else
		{
			return false;
		}
	}



}