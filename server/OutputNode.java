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

class OutputNode
{
	static final int FREE_STACK_LENGTH = 5000;
	static OutputNode free_stack[] = new OutputNode[FREE_STACK_LENGTH];
	static int free_stack_top = 0;
	static Semaphore semaphore_OutputNode = new Semaphore();
	private boolean free = true;

	public StringBuffer output_buffer = new StringBuffer(Constants.OUTPUT_BUFFER_LENGTH);
	ClientThread client_thread;
	OutputNode next = null;

	public static OutputNode Get(ClientThread _clientThread,	String _message)
	{
		OutputNode object;

		semaphore_OutputNode.acquire();

		if (free_stack_top > 0)
		{
			free_stack_top--;
			free_stack[free_stack_top].Initialize(_clientThread, _message);
			object = free_stack[free_stack_top];
		}
		else
		{
			object = new OutputNode(_clientThread, _message);
		}

		// Record that this object is not free.
		object.free = false;

		// No longer busy
		semaphore_OutputNode.release();

		return object;
	}

	public static void Free(OutputNode _object)
	{
		if (_object.free)
		{
			Output.PrintToScreen("ERROR: OutputNode.Free() called for OutputNode " + _object.hashCode() + " that is already marked as being free!");
			Output.PrintStackTrace();
			return;
		}

		// Make sure the object being freed is non-null.
		if (_object == null) {
			Output.PrintToScreen("Attempt to free null OutputNode!");
			throw new RuntimeException("Attempt to free null OutputNode!");
		}

		// Reinitialize OutputNode's members upon freeing it
		_object.client_thread = null;
		_object.next = null;

		semaphore_OutputNode.acquire();

		// Add the object to the free stack if there's room
		if (free_stack_top < FREE_STACK_LENGTH)
		{
			free_stack[free_stack_top] = _object;
			free_stack_top++;
		}

		// Record that this object is free.
		_object.free = true;

		// No longer busy
		semaphore_OutputNode.release();
	}

	public OutputNode(ClientThread _clientThread, String _output)
	{
		Initialize(_clientThread,	_output);
	}

	public void Initialize(ClientThread _clientThread, String _output)
	{
		client_thread = _clientThread;
		next = null;

		// Copy the _input into the output_buffer StringBuffer
		output_buffer.setLength(0);
		output_buffer.append(_output);
	}
};
