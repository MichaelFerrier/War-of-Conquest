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

import java.io.*;
import java.util.*;

public class Output
{
	public static PrintWriter screenOut = new PrintWriter(System.out, true);

	static StringBuffer output_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);
	static Semaphore output_buffer_semaphore = new Semaphore();

	public static void PrintToScreen(String _output)
	{
		// Print the given _output to the screen.
		screenOut.println(_output);

		// Add newline and save the given _output to the log file.
		_output += "\n";
		Constants.WriteToLog("log_output.txt", _output);

		// If any admin clients are in the game, broadcast an event or the given _output text to each admin client.
		if (WOCServer.admin_clients.size() > 0)
		{
			if (output_buffer_semaphore.available())
			{
				// Acquire the output buffer
				output_buffer_semaphore.acquire();

				// Get the admin log event and terminate it.
				output_buffer.setLength(0);
				OutputEvents.GetAdminLogEvent(output_buffer, _output);
				Constants.EncodeString(output_buffer, "end");
				output_buffer.append('\u0000');

				// Broadcast the admin log event to all admins in the game.
				//OutputEvents.BroadcastToAllAdmins(output_buffer.toString());

				// Release the output buffer
				output_buffer_semaphore.release();
			}
			else
			{
				// Call a simple version of this method to output the below error, so a recursive loop isn't created.
				Output.PrintToScreenSimple("ERROR: Output.PrintToScreen() cannot create admin log event because temp_output_buffer_semaphore is unavailable. (Message: " + _output + ")");
			}
		}
	}

	public static void PrintToScreenSimple(String _output)
	{
		// Print the given _output to the screen.
		screenOut.println(_output);

		// Add newline and save the given _output to the log file.
		_output += "\n";
		Constants.WriteToLog("log_output.txt", _output);
	}

	public static void PrintTimeToScreen(String _output)
	{
		Date date = new Date();
		PrintToScreen(date.getTime() + ": " + _output);
	}

	public static void PrintException(Exception _e)
	{
		// Print the stack trace to a StringWriter
		StringWriter sw = new StringWriter();
		_e.printStackTrace(new PrintWriter(sw));

		// Output the exception's message and stack trace
		PrintToScreen("\ne.getMessage: " + _e.getMessage());
		PrintToScreen("\nStack Trace: " + sw.toString());
	}

	public static void PrintStackTrace()
	{
		// Print the stack trace to a StringWriter
		Exception e = new Exception();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		PrintToScreen("\nStack Trace: " + sw.toString());
	}
};
