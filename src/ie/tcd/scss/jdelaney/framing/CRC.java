package ie.tcd.scss.jdelaney.framing;

/*	
CRC_16.cpp - Arduino CRC 16 generator
Copyright (C) 2013 Graeme Wilson <gnw.wilson@gmail.com>

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

public class CRC
{
	
	private long m_crc = 0x18005;
	private long m_remainder=0x00; //Initialize remainder to zero
	
	//Constructor Implementation
	void CRC_16() {
		m_remainder=0x00; //Initialize remainder to zero
	}

	//Public method for reseting CRC_16 class to ready state
	public void reset() {
		m_remainder=0x00;
	}

	private String printByte(byte b)
	{
		return String.format("0x%02X", b);
	}		

	//Public method for calculating remainder for next databyte in stream
	public void next_databyte(byte databyte) {
		
		//System.out.println("Adding " + printByte(databyte));

		for (int i=7; i>=0; i--) {
			m_remainder=(m_remainder<<1)|((databyte>>i)&0x01);
			if ((m_remainder>>16)==0x01)
				m_remainder=m_remainder^m_crc;
			//System.out.println("  m_remainder=" + m_remainder);
		} 

	}

	//Public method for returning 16 bit CRC and reseting CRC_16 to ready state
	short returnCRC_reset() {
		short rem;
		for (int i=16; i>0; i--) {
			m_remainder=(m_remainder<<1);
			if ((m_remainder>>16)==0x01)
				m_remainder=m_remainder^m_crc;
		}
		
		rem=(short)m_remainder;
		reset();
		return rem;
	}
}
