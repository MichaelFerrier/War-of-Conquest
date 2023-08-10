//
// War of Conquest Server
// Copyright (c) 2002-2023 Michael Ferrier, IronZog LLC
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

package WOCServer;

import WOCServer.Constants;
import java.util.*;

class Semaphore
{
	private volatile int semaphore = 0;

	// How long to wait for another thread to complete its operation before trying again
	static int WAIT_FOR_THREAD_MILLISECONDS = 10;

	// Reset the stuck semaphore after we've been waiting for it for this many seconds.
	static int RESET_AFTER_MILLISECONDS = 30000;

	public boolean available()
	{
		synchronized(this)
		{
			return semaphore == 0;
		}
	}

	public void acquire()
	{
		long acquire_request_time = -1;

		//// TESTING
		//Output.PrintStackTrace();

		// If this semaphore is currently busy, sleep for a bit and then try again.
		while (acquire_internal() > 1)
		{
			release();

			// Must acquire fresh time; cannot rely on Constants.GetTime() because this paused thread may the the one that updates it.
			Date whole_date = new Date();

			if (acquire_request_time == -1)
			{
				acquire_request_time = whole_date.getTime();
			}
			else
			{
				if ((whole_date.getTime() - acquire_request_time) >= RESET_AFTER_MILLISECONDS)
				{
					Output.PrintToScreen("ERROR: Semaphore acquire() reset after waiting " + (whole_date.getTime() - acquire_request_time) + " milliseconds!");
					Output.PrintStackTrace();
					return;
				}
			}

			// Sleep for a while, until the next check.
			try{
				Thread.sleep(WAIT_FOR_THREAD_MILLISECONDS);
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("Semaphore busy insomnia");}
		}
	}

	public int acquire_internal()
	{
		synchronized(this)
		{
			return ++semaphore;
		}
	}

	public int release()
	{
		synchronized(this)
		{
			if (semaphore <= 0) Output.PrintToScreen("ERROR: Semaphore.release() called, semaphore: " + semaphore);
			return --semaphore;
		}
	}
}
