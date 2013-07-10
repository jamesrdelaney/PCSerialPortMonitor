package ie.tcd.scss.jdelaney.framing;

public class Frame {
	private int sequenceNumber;						// the sequencenumber
	private byte type;								// the type of data
	private byte[] data;							// the raw data
	private byte[] framed_data;						// the framed data
	private int sendCount;
	private int timer;
	
	private static byte m_STX=0x02;
	private static byte m_ETX=0x03;
	private static byte m_DLE=0x10;

	public Frame(int sequenceNumber, byte[] data, byte type)
	{
		this.sequenceNumber 		= sequenceNumber;
		this.data				= data;
		this.type				= type;
		this.sendCount			= 0;
		this.timer				= 0;
	}

	public int getSequenceNumber()
	{
		return sequenceNumber;
	}	
	public byte[] getData()
	{
		return data;
	}	
	public byte getType()
	{
		return type;
	}	
	public int getSendCount()
	{
		return sendCount;
	}	
	public void increaseSendCount()
	{
		sendCount++;
	}	
	public void increaseTimer()
	{
		if(timer>10000)
			timer=0;
		else
		timer++;
	}	
	public int getTimer()
	{
		return timer;
	}	

	//Public method for framing data
//	public byte[] getFramedData(byte[] data, byte type) {
	public byte[] getFramedData() {
		int buf_index=0;
		
		// Create CRC here
		CRC createCRC = new CRC();
		
		framed_data = new byte[1000];
		
		//Add start flag
		framed_data[buf_index]=m_DLE;
		buf_index++;
		framed_data[buf_index]=m_STX;
		buf_index++;
		
		//iSendSeq = 0;

		//System.out.println("Sequence Hi: " + BytePrinter.printByte((byte) (iSendSeq>>8)));
		//Put in sequence number hi byte
		framed_data[buf_index] = (byte) (sequenceNumber>>8);
		buf_index++;
		createCRC.next_databyte((byte) (sequenceNumber>>8));

		//System.out.println("Sequence Lo: " + BytePrinter.printByte((byte) (iSendSeq)));
		//Put in sequence number lo byte
		framed_data[buf_index] = (byte) (sequenceNumber);
		buf_index++;
		createCRC.next_databyte((byte) sequenceNumber);

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
}
