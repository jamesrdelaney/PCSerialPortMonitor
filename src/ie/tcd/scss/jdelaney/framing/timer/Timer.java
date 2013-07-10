package ie.tcd.scss.jdelaney.framing.timer;


public class Timer extends Thread
{
	// Debugging constants
	final int NONE 			= 0;
	final int NORMAL 		= 1;
	final int VERBOSE 		= 2;
	int debugLevel 	= NORMAL;

	/** Rate at which timer is checked */
	protected int m_rate = 1;
	
	/** Length of timeout */
	private int m_length;

	/** Time elapsed */
	private int m_elapsed;

	private volatile boolean running = false;

	/**
	  * Creates a timer of a specified length
	  * @param	length	Length of time before timeout occurs
	  */
	public Timer ( int length )
	{
		// Assign to member variable
		m_length = length;

		// Set time elapsed
		m_elapsed = 0;
	}

	
	/** Resets the timer back to zero */
	public synchronized void reset()
	{
		m_elapsed = 0;
	}

	/** Performs timer specific code */
	public void run()
	{
		if(debugLevel>=NORMAL) System.out.println("Start Timer");

		running = true;
		
		// Keep looping
		while (running)
		{
			//if(running)
			//System.out.println("Timer running");

			// Put the timer to sleep
			try
			{ 
				Thread.sleep(m_rate);
			}
			catch (InterruptedException ioe) 
			{
				continue;
			}

			// Use 'synchronized' to prevent conflicts
			synchronized ( this )
			{
				// Increment time remaining
				m_elapsed += m_rate;

				// Check to see if the time has been exceeded
				if (m_elapsed > m_length)
				{
					// Trigger a timeout
					timeout();
				}
			}
		}
	}

	/** Performs timer specific code */
	public boolean isrunning()
	{
		//if(debugLevel>=NORMAL) System.out.println ("Timer is: " + running);
		return running;
	}

	// Override this to provide custom functionality
	public void timeout()
	{
		running = false;

		if(debugLevel == NORMAL) System.out.println ("Timeout occurred.... terminating");
		//System.exit(1);
	}

	public void kill()
	// kill the timer
	{
		running = false;
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