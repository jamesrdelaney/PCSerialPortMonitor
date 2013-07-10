package ie.tcd.scss.jdelaney.framing;

import ie.tcd.scss.jdelaney.framing.timer.Timer;

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


public class CopyOfFraming implements Runnable {
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

	private int iSendSeq = 32765;

	//Define static constants
	private static byte m_STX=0x02;
	private static byte m_ETX=0x03;
	private static byte m_DLE=0x10;
	
	//Constructor for Framing
	public CopyOfFraming() {
	}

	public void run()
	// The run method looks for incoming data in its input buffer and decodes it
	{
		while(true)
		{
			byte newbyte, oldbyte;
			byte[] unframedData = new byte[1000];
			int count = 0;

			if(getBufferLength()>0)
			{
				newbyte = getByteFromStartOfBuffer();

				if(debugLevel>=NORMAL) System.out.println("Received possible first byte: " + BytePrinter.printByte(newbyte));

				Timer t = new Timer(1000);
				t.start();
				
				boolean messageReceived = false;

				while(t.isrunning() && (!messageReceived))
				{
					//System.out.println("1");
					if(getBufferLength()>0)
					{
						oldbyte = newbyte;
						newbyte = getByteFromStartOfBuffer();

						if(debugLevel>=NORMAL) System.out.println("Received possible second byte: " + BytePrinter.printByte(newbyte));

						if((oldbyte==m_DLE) && (newbyte==m_STX)) 
						{
							if(debugLevel == VERBOSE) System.out.println("Received start sequence");

							int data_index=0;
							while(t.isrunning() && (!((oldbyte==m_DLE) && (newbyte==m_ETX)))) {
								if(getBufferLength()>0) {
									oldbyte = newbyte;
									newbyte = getByteFromStartOfBuffer();

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

										//System.out.println("Received byte: " + printByte(newbyte));

										}
							   }
							}
							// Now we have the unframed data in unframedData

							// Kill the timer
							t.kill();
							
							// Also set a flag to stop looking at this message because the timer can take time to stop.
							messageReceived = true;

							CRC crc = new CRC();
							boolean crc_valid;

							// Check checksum
							int length=data_index-3;
							
							for(int i=0; i<length+2; i++) {
								crc.next_databyte(unframedData[i]);
							}

							if(crc.returnCRC_reset()==0x00) {
	                			crc_valid=true;
								
	                			if(debugLevel==NORMAL) System.out.println("Received:");
	                			if(debugLevel==NORMAL) System.out.println("Unframed data: " + BytePrinter.printBytes(unframedData, data_index-3));
	                			if(debugLevel==NORMAL) System.out.println("(ascii):     : " + BytePrinter.printAscii(unframedData, data_index-3));

	                			switch(unframedData[2])
	                			{
	                				case type_rtp:
	                				{
	    	                			if(debugLevel==NORMAL) System.out.println("RTP data received");
	    	                			break;
	                				}
	                				case type_rtsp:
	                				{
	    	                			if(debugLevel==NORMAL) System.out.println("RTSP data received");
	    	                			break;
	                				}
	                				case type_ack:
	                				{
	                					//int sequenceNumber = unframedData[0] << 8 + unframedData[1];
	                					int sequenceNumber = (unframedData[3]<<8) + (unframedData[4] & 0xff);

	    	                			//if(debugLevel==NORMAL) System.out.println("Ack received for frame " + (int)(unframedData[0]<<8 + unframedData[1]));
	    	                			if(debugLevel==NORMAL) System.out.println("Ack received for frame " + Integer.toString(sequenceNumber) );
	    	                			break;
	                				}
	                				case type_message:
	                				{
	    	                			if(debugLevel==NORMAL) System.out.println("Message received from serial device: " + new String(Arrays.copyOfRange(unframedData, 3, data_index-3) ) );
	    	                			break;
	                				}
	                				default:
	                				{
	    	                			if(debugLevel==NORMAL) System.out.println("Unknown data type received");
	                				}
	                			}
							}
							else {
								crc_valid=false;
							}
							
							if(debugLevel>=NORMAL) System.out.println("Checksum = " + crc_valid);
						}
						else
						{
							if(debugLevel>=NORMAL) System.out.println("Bad start to string");
						}
							
					}
					else
					{
						if(iCount<1000000)
						{
							if(iCount==0)
							{
								System.out.println("Timed out");
							}
							iCount++;
						}
						else
							iCount=0;

					}
				}
			}
		}
			//byte[] b = new byte[buffer.remaining()];
			//buffer.get(b);
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

		//Put in sequence number hi byte
		framed_data[buf_index] = (byte) (iSendSeq>>8);
		buf_index++;

		//Put in sequence number lo byte
		framed_data[buf_index] = (byte) (iSendSeq);
		buf_index++;

		//Put in type byte
		framed_data[buf_index] = type;
		buf_index++;

		//Send data with unsigned char stuffing - Also calculate CRC (ignore stuffing)
		for (int i=0; i<data.length; i++) {
			if(data[i]==m_DLE) {
			
			  framed_data[buf_index]=m_DLE;
			  buf_index++;
			  
			  framed_data[buf_index]=data[i];
			  buf_index++;
			  //createCRC.next_databyte(data[i]);
			}
			else {
			  framed_data[buf_index]=data[i];
			  buf_index++;
			  //createCRC.next_databyte(data[i]);
			}
		}

		//Return CRC
		short CRC=createCRC.returnCRC_reset();
		
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

}

