package ie.tcd.scss.jdelaney.framing;

public abstract class BytePrinter {

	public static String printBytes(byte[] b, int len)
	{
		String formattedBytes = new String();
		
		for(int i=0;i<len;i++)
		{
			if(i>0)
			formattedBytes += "  ";
			
			formattedBytes += String.format("0x%02X", b[i]);
		}
		
		return formattedBytes;
	}

	public static String printBytes(byte[] b)
	{
		String formattedBytes = new String();
		
		for(int i=0;i<b.length;i++)
		{
			if(i>0)
			formattedBytes += "  ";
			
			formattedBytes += String.format("0x%02X", b[i]);
		}
		
		return formattedBytes;
	}

	public static String printByte(byte b)
	{
		return String.format("0x%02X", b);
	}


	public static String printAscii(byte[] b, int len)
	{
		String formattedBytes = new String();
		
		for(int i=0;i<len;i++)
		{
			if(i>0)
			formattedBytes += "  ";
			
			formattedBytes += (char)b[i] + "   ";
		}
		
		return formattedBytes;
	}
	
	public static String printAscii(byte[] b)
	{
		String formattedBytes = new String();
		
		for(int i=0;i<b.length;i++)
		{
			if(i>0)
			formattedBytes += "  ";
			
			formattedBytes += (char)b[i] + "   ";
		}
		
		return formattedBytes;
	}	

	public static String printAscii(byte b)
	{
		String formattedBytes = new String();
		formattedBytes += (char) b;
		return formattedBytes;
	}	
}
