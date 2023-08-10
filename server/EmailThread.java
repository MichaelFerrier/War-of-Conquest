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

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import WOCServer.*;

public class EmailThread extends Thread {

	static EmailNode firstEmailNode = null;
	static EmailNode lastEmailNode = null;

	static int prev_sleep_time = 0;

	public EmailThread()
	{
		super("EmailThread");
	}

	static void QueueEmail(String _from, String _from_name, String _address, String _subject, String _body)
	{
		// Construct url string
		_body = URLEncoder.encode(_body);
		_subject = URLEncoder.encode(_subject);
		_address = URLEncoder.encode(_address);
		_from = URLEncoder.encode(_from);
		_from_name = URLEncoder.encode(_from_name);
		String url_string = "from=" + _from + "&from_name=" + _from_name + "&address=" + _address + "&subject=" + _subject + "&body=" + _body;

		// Sanity check _address
		if (_address.equals(""))
		{
			Output.PrintToScreen("QueueEmail(): Attempt to send e-mail with no address given. URL string: " + url_string);
			return;
		}

		EmailNode new_node = new EmailNode(url_string);
		//Output.PrintToScreen("queueing email node, address: " + _address); // TESTING

		// Add the new email node to the end of the queue.
		if (lastEmailNode == null) {
			firstEmailNode = new_node;
		}	else {
			lastEmailNode.next = new_node;
		}
		lastEmailNode = new_node;
	}

	public void run()
	{
		BufferedReader input = null;
		InputStream stream;
		String buffer = "";
		boolean error;

		// Cycle
		for (;;)
		{
			// Sleep for a while, until the next check.
			try{
			  Thread.sleep(Constants.EMAIL_THREAD_WAIT_SLEEP_MILLISECONDS);
				prev_sleep_time = Constants.GetTime();
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("EmailThread Insomnia");}

			// If the email thread is not active, continue to next loop iteration.
			if (!WOCServer.email_thread_active) {
				continue;
			}

			// Reset error flag
			error = false;

			// Process any queued EmailNodes
			while (firstEmailNode != null)
			{
				try
				{
					URL url = new URL("https://warofconquest.com/send_email_wrapper.php?key=98797987&" + firstEmailNode.url_string);
					//Output.PrintToScreen("URL: " + url.toString()); // TESTING

					stream = url.openStream();
					input = new BufferedReader(new InputStreamReader(stream));
					buffer = input.readLine();
					input.close();
					stream.close();
				}
				catch (IOException ioe)
				{
					Output.PrintToScreen("*** EmailThread: failed to read data from send_email.php " + Constants.GetFullDate());
          Output.PrintToScreen("url_string: " + firstEmailNode.url_string);
					break;
				}

				if (buffer.compareTo("success") != 0)
				{
					Output.PrintToScreen("*** EmailThread: send_email.php was not successful " + Constants.GetFullDate() + ", result: " + buffer);
					Output.PrintToScreen("url_string: " + firstEmailNode.url_string);
					break;
				}

				// Advance to next EmailNode
				firstEmailNode = firstEmailNode.next;

				if (firstEmailNode == null) {
					lastEmailNode = null;
				}
			}
		}
	}
}

class EmailNode
{
	String url_string;
	EmailNode next;

	public EmailNode(String _url_string)
	{
		url_string = _url_string;
	}
};
