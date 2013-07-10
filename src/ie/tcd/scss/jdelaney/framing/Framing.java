package ie.tcd.scss.jdelaney.framing;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import ie.tcd.scss.jdelaney.framing.timer.Timer;
import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm.SerialReader;
import ie.tcd.scss.jdelaney.serial.TwoWaySerialComm.SerialWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*	
Framing.cpp - Arduino Due data framing using flags, unsigned char stuffing, and CRC 16
Copyright (C) 2013 Graeme Wilson <gnw.wilson@gmail.com>

Modified for Java by James Delaney

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


public class Framing implements Runnable {
	int iCount = 0;
	// Debugging constants
	final int NONE 			= 0;
	final int NORMAL 		= 1;
	final int VERBOSE 		= 2;
	int debugLevel 	= VERBOSE;

	final byte type_rtp 	= '1';
	final byte type_rtsp 	= '2';
	final byte type_ack 	= '3';
	final byte type_message = '4';

	private byte[] buffer = new byte[100];
	private int iCurrentPosition = 0;

	private int iSendSeq = 500;

	//Define static constants
	private static byte m_STX=0x02;
	private static byte m_ETX=0x03;
	private static byte m_DLE=0x10;
	
	// Outputstream for serial port
	private OutputStream out;
	private InputStream in;
	private String portName;

	final int RTSPState_Ready       = 1;
	final int RTSPState_Recording   = 2;
	final int RTSPState_Init        = 3;
	final int RTSPState_Playing     = 4;

	String RTSP_States[] = {"BLANK", "READY", "RECORDING", "INIT", "PLAYING"};
	
	//Constructor for Framing
	public Framing(String portName) {
		this.portName = portName;
	}

	public void run()
	// The run method looks for incoming data in its input buffer and decodes it
	{

        CommPortIdentifier portIdentifier;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		
	        if ( portIdentifier.isCurrentlyOwned() )
	        {
	            if(debugLevel>=NORMAL) System.out.println("Error: Port is currently in use");
	        }
	        else
	        {
	            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
	            
	            if ( commPort instanceof SerialPort )
	            {
	                SerialPort serialPort = (SerialPort) commPort;
	                serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
	                //serialPort.setSerialPortParams(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
	                
	                in = serialPort.getInputStream();
	                out = serialPort.getOutputStream();
	
	                //new Thread(this).start();
	                //(new Thread(new SerialWriter(out))).start();
	            }
	            else
	            {
	            	if(debugLevel>=NORMAL) System.out.println("Error: Only serial ports are handled by this example.");
	            }
	        }
	
	        Boolean keepRunning = true;
	        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF8"));

			//byte[] tempdata = new byte[1000];
			//int tempcount = 0;
	        while(true)
			{
	        	
				byte newbyte, oldbyte;
				byte[] unframedData = new byte[1000];

				int count = 0;
				int input;
				char c;
	
				
				try
				// 1stCharTry
				{
	                if ( ( input = br.read()) != -1 )
	                {	
	                	//tempdata[tempcount] = (byte)input;
	                	//tempcount++;
	                	//System.out.println("\nTempdata now: " + BytePrinter.printBytes(tempdata, tempcount));
	                	//System.out.println("\nTempdata now: " + BytePrinter.printAscii(tempdata, tempcount));
	                	//System.out.println("Tempcount now: " + Integer.toString(tempcount));
	                	
						newbyte = (byte) input;

	                	//tempdata[tempcount] = (byte)input;
	                	//tempcount++;
	                	//System.out.println("\nTempdata now: " + BytePrinter.printBytes(tempdata, tempcount));
	                	//System.out.println("\nTempdata now: " + BytePrinter.printAscii(tempdata, tempcount));
						
						if(debugLevel>=NORMAL) System.out.println("\nReceived possible first byte: " + BytePrinter.printByte(newbyte));
		
						//Timer t = new Timer(1000);
						//t.start();
						
						//boolean messageReceived = false;
		
	                	int secondCharWaitCount=0;
		                
						boolean startSequenceReceived = false;
						
	                	try
		                // 2ndCharTry
		                {
		                	secondCharWaitCount=0;

	                		while ((!startSequenceReceived) && ( ( input = br.read()) != -1 ) && (secondCharWaitCount<1) )
		                	{
	    	                	//tempdata[tempcount] = (byte)input;
	    	                	//tempcount++;
	    	                	//System.out.println("\nTempdata now: " + BytePrinter.printBytes(tempdata, tempcount));
	    	                	//System.out.println("\nTempdata now: " + BytePrinter.printAscii(tempdata, tempcount));

	    	                	oldbyte = newbyte;
								newbyte = (byte) input;
		
								if(debugLevel>=NORMAL) System.out.println("Received possible second byte: " + BytePrinter.printByte(newbyte));
		
								if((oldbyte==m_DLE) && (newbyte==m_STX)) 
								{
									if(debugLevel == VERBOSE) System.out.println("Received start sequence");
									startSequenceReceived = true;
		
									int data_index=0;
									
									int otherCharsWaitCount = 0;

									try
									{
										while((!((oldbyte==m_DLE) && (newbyte==m_ETX)))) 
										{
											if (( ( input = br.read()) != -1 ) && (otherCharsWaitCount<3))
						                	{
					    	                	//tempdata[tempcount] = (byte)input;
					    	                	//tempcount++;
					    	                	//System.out.println("\nTempdata now: " + BytePrinter.printBytes(tempdata, tempcount));
					    	                	//System.out.println("\nTempdata now: " + BytePrinter.printAscii(tempdata, tempcount));

					    	                	otherCharsWaitCount = 0;
												oldbyte = newbyte;
												newbyte = (byte) input;
	
												if(newbyte==m_DLE) {
													if(oldbyte==m_DLE) {
														unframedData[data_index]=newbyte;
														data_index++;
			
														if(debugLevel>=NORMAL) System.out.println("Unpack byte: " + BytePrinter.printByte(newbyte));
														newbyte=0;
													}
												}
												else {
													unframedData[data_index]=newbyte;
													data_index++;
			
													//System.out.println("Received byte: " + BytePrinter.printByte(newbyte));
													//System.out.println("Received byte: " + BytePrinter.printByte(unframedData[data_index-1]));
			
												}
						                	}
										}
									}
									catch (IOException e)
									// otherCharsCatch
									{
					                	otherCharsWaitCount++;
					                	
					                	System.out.println("Wait for the other characters");
					                	
					                	try {
											Thread.sleep(100);
										} catch (InterruptedException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
									}
									// Now we have the unframed data in unframedData
		
									// Kill the timer
									//t.kill();
									
									// Also set a flag to stop looking at this message because the timer can take time to stop.
									//messageReceived = true;
		
									CRC crc = new CRC();
		
									
									//System.out.println("length is: " + Integer.toString(length));
									
									// Check checksum
									for(int i=0; i<data_index-3; i++) {
										crc.next_databyte(unframedData[i]);
			                			
										//if(debugLevel>=NORMAL) System.out.println("Adding to crc:     : " + BytePrinter.printByte(unframedData[i]));
									}

                					
									//System.out.println("Received CRC hi byte is: " + BytePrinter.printByte(unframedData[data_index-3]));
									//System.out.println("Received CRC lo byte is: " + BytePrinter.printByte(unframedData[data_index-2]));
									
									int receivedCrc = (unframedData[data_index-3]<<8) + (unframedData[data_index-2] & 0xff);
									//System.out.println("Received CRC is: " + BytePrinter.printByte((byte)receivedCrc));
									//System.out.println("Calculated CRC is: " + BytePrinter.printByte((byte)crc.returnCRC_reset()));
		
									short calculatedCRC = crc.returnCRC_reset();
									
		                			//System.out.println("Received CRC: " +  Integer.toString(receivedCrc) );
		                			//System.out.println("Calculated CRC: " +  Short.toString(calculatedCRC) );

									if(calculatedCRC==receivedCrc) {
										if(debugLevel>=NORMAL) System.out.println("Checksum OK");

			                			if(debugLevel>=NORMAL) System.out.println("Received:");
			                			if(debugLevel>=NORMAL) System.out.println("Unframed data: " + BytePrinter.printBytes(unframedData, data_index-1));
			                			if(debugLevel>=NORMAL) System.out.println("(ascii)      :  " + BytePrinter.printAscii(unframedData, data_index-1));
			                			switch(unframedData[2])
			                			{
			                				case type_rtp:
			                				{
			    	                			if(debugLevel>=NORMAL) System.out.println("RTP data received");
			    	                			break;
			                				}
			                				case type_rtsp:
			                				{
			    	                			if(debugLevel>=NORMAL) System.out.println("RTSP data received.");
			    	                			break;
			                				}
			                				case type_ack:
			                				{
			                					//int sequenceNumber = unframedData[0] << 8 + unframedData[1];
			                					int sequenceNumber = (unframedData[3]<<8) + (unframedData[4] & 0xff);
		
			    	                			//if(debugLevel>=NORMAL) System.out.println("Ack received for frame " + (int)(unframedData[0]<<8 + unframedData[1]));
			    	                			if(debugLevel>=NORMAL) System.out.println("Ack received for frame " + Integer.toString(sequenceNumber) );
			    	                			if(debugLevel>=NORMAL) System.out.println("Current state is: " + RTSP_States[((int) unframedData[5])]);
			    	                			break;
			                				}
			                				case type_message:
			                				{
			    	                			if(debugLevel>=NORMAL) System.out.println("Message received from serial device: " + new String(Arrays.copyOfRange(unframedData, 3, data_index-3) ) );
			    	                			break;
			                				}
			                				default:
			                				{
			    	                			if(debugLevel>=NORMAL) System.out.println("Unknown data type received");
			                				}
			                			}
									}
									else {
										if(debugLevel>=NORMAL) System.out.println("Checksum FALSE");

			                			if(debugLevel>=NORMAL) System.out.println("Received:");
			                			if(debugLevel>=NORMAL) System.out.println("Unframed data: " + BytePrinter.printBytes(unframedData, data_index-1));
			                			if(debugLevel>=NORMAL) System.out.println("(ascii):     : " + BytePrinter.printAscii(unframedData, data_index-1));
			                			
									}
									
								}
								else
								{
									if(debugLevel>=NORMAL) System.out.println("Bad start to string");
									//break; // break out of the while loop for the 2nd start character
								}
							}
		                }
		                catch (IOException e)
		                // 2ndCharCatch
		                {
		                	secondCharWaitCount++;
		                	
		                	System.out.println("Wait for a second character");
		                	
		                	//e.printStackTrace();

		                	try {
								Thread.sleep(100);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						
					}
				}
				catch (IOException e)
				// This is the first in.read()
				{
				}
			}
		} 
		catch (NoSuchPortException e) {
			System.out.println("No such port: " + portName);
			e.printStackTrace();
		}
		catch (UnsupportedCommOperationException e) {
			System.out.println("Unsupported Comm Operation");
			e.printStackTrace();
		} 
		catch (IOException e) {
			System.out.println("Error opening input/outputstream");
			e.printStackTrace();
		} 
		catch (PortInUseException e) {
			System.out.println("Port is in use");
			e.printStackTrace();
		}
	}

	
	
	public synchronized void addToBuffer(byte[] data, int len) throws ArrayIndexOutOfBoundsException
	// Add data to end of buffer
	{
		if(len>data.length)
		{
			System.out.println("Length specified is longer than data length");
		}
		else
		{
			if(iCurrentPosition+len > buffer.length)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				for(int i = 0;i<len;i++)
				{
						buffer[iCurrentPosition] = data[i];
						iCurrentPosition++;
				}
			}
		}
	}
	
	public synchronized byte getByteFromStartOfBuffer() throws BufferUnderflowException
	{
		if(iCurrentPosition==0)
			throw new BufferUnderflowException();
		else
		{
			byte returnbyte = buffer[0];
			
			for(int i=0; i<iCurrentPosition-1; i++)
			{
				buffer[i] = buffer[i+1];
			}
	
			iCurrentPosition--;
	
			return returnbyte;
		}
	}

	public synchronized int getBufferLength()
	{
		return iCurrentPosition;
	}


	public void send(byte[] data, byte type) throws IOException
	{
		//byte[] bytes = getFramedData(data, type);
		//System.out.println(Integer.toString(bytes.length));
		
		out.write(getFramedData(data, type));
		System.out.println(BytePrinter.printBytes(getFramedData(data, type)));
	}

	/*
	public void send(byte[] data, byte type) throws IOException
	{
		byte[] bytes = getFramedData(data, type);
		out.write(bytes);

		CRC crc = new CRC();
		
		System.out.println("\nbytes: " + BytePrinter.printBytes(bytes));

		// Check checksum
		for(int i=2;i<bytes.length-4;i++) {
			crc.next_databyte(bytes[i]);
			if(debugLevel>=NORMAL) System.out.println("Adding to crc:     : " + BytePrinter.printByte(bytes[i]));
		}
	
		short CRC=crc.returnCRC_reset();
		System.out.println("Send CRC: " +  Short.toString(CRC) );
	}
	*/
	//Public method for framing data
	public synchronized byte[] getFramedData(byte[] data, byte type) {
		int buf_index=0;
		byte[] framed_data = new byte[1000];
		
		// Create CRC here
		CRC createCRC = new CRC();
		
		//Add start flag
		framed_data[buf_index]=m_DLE;
		buf_index++;
		framed_data[buf_index]=m_STX;
		buf_index++;
		
		increaseSequenceNumber();
		//iSendSeq = 0;

		//System.out.println("Sequence Hi: " + BytePrinter.printByte((byte) (iSendSeq>>8)));
		//Put in sequence number hi byte
		framed_data[buf_index] = (byte) (iSendSeq>>8);
		buf_index++;
		createCRC.next_databyte((byte) (iSendSeq>>8));

		//System.out.println("Sequence Lo: " + BytePrinter.printByte((byte) (iSendSeq)));
		//Put in sequence number lo byte
		framed_data[buf_index] = (byte) (iSendSeq);
		buf_index++;
		createCRC.next_databyte((byte) iSendSeq);

		//System.out.println("Type: " + BytePrinter.printAscii((byte) (type)));
		//Put in type byte
		framed_data[buf_index] = type;
		buf_index++;
		createCRC.next_databyte(type);
		

		//Send data with unsigned char stuffing - Also calculate CRC (ignore stuffing)
		for (int i=0; i<data.length; i++) {
			if(data[i]==m_DLE) {
			
				//System.out.println("Packed Data: " +  BytePrinter.printByte((byte) (data[i])));
			  
			framed_data[buf_index]=m_DLE;
			  buf_index++;
			  
			  framed_data[buf_index]=data[i];
			  buf_index++;
			  createCRC.next_databyte(data[i]);

			}
			else {
				//System.out.println("Data: " +  BytePrinter.printByte((byte) (data[i])));
			  
				framed_data[buf_index]=data[i];
			  buf_index++;
			  createCRC.next_databyte(data[i]);

			}
		}

		//Return CRC
		short CRC=createCRC.returnCRC_reset();
		
		//System.out.println("CRC: " +  Short.toString(CRC) );
		
		//Send CRC with byte stuffing
		framed_data[buf_index]=(byte)((CRC>>8)&0xff);
		buf_index++;
		
		if(framed_data[buf_index-1]==m_DLE) {
			framed_data[buf_index]=m_DLE;
			buf_index++;
		}
		
		framed_data[buf_index]=(byte)(CRC&0xff);
		buf_index++;
		
		if(framed_data[buf_index-1]==m_DLE) {
			framed_data[buf_index]=m_DLE;
			buf_index++;
		}

		//Send end flag
		framed_data[buf_index]=m_DLE;
		buf_index++;
		framed_data[buf_index]=m_ETX;
		buf_index++;
	
		byte[] framedDataToSend = new byte[buf_index];
		System.arraycopy(framed_data, 0, framedDataToSend, 0, buf_index);
		
		//Serial.write(framed_data, buf_index);
		return framedDataToSend;
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

	private void increaseSequenceNumber()
	{
		if(iSendSeq<32767)
		iSendSeq++;
		else
		iSendSeq = 0;
	}

	
/*    public void connect ( String portName ) throws Exception
    {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() )
        {
            if(debugLevel>=NORMAL) System.out.println("Error: Port is currently in use");
        }
        else
        {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
            
            if ( commPort instanceof SerialPort )
            {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
                
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();

                new Thread(this).start();
                //(new Thread(new SerialWriter(out))).start();
            }
            else
            {
            	if(debugLevel>=NORMAL) System.out.println("Error: Only serial ports are handled by this example.");
            }
        }
    }*/
}

