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
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import WOCServer.WOCServer;
import WOCServer.DataManager;
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.InputEvents;

public class ClientThread
{
	static final int FREE_STACK_LENGTH = 5000;
	static ClientThread free_stack[] = new ClientThread[FREE_STACK_LENGTH];
	static int free_stack_top = 0;
	static Semaphore semaphore_ClientThread = new Semaphore();
	static Semaphore semaphore_send = new Semaphore();
	private boolean free = true;

	// ClientThreads are re-used, so make sure all members are initialized in Init()!
	private SocketChannel socket_channel = null;
	public PlayerAccountData player_account = null;
	public String clientID;
	public String clientUID;
	public String clientIP;
	public String client_activation_code = "";
	public DeviceData device_data;
	public int client_index;
	public int client_version = -1;
	private int userID = -1;
	public boolean userIsAdmin = false;
	public boolean logged_in = false;
	public boolean in_game = false;
	public long prev_use_fine_time = 0;
	public long mean_use_interval = 60000; // Start at 1 minute
	private Date date;
	public long log_suspect_expire_time = 0;
	public long fine_time = 0, cur_use_interval, new_mean_use_interval;
	public int bad_passwords_count = 0, bad_passwords_time = 0;

	private int READ_ARRAY_LENGTH = 2048;
	private char[] read_array = new char[READ_ARRAY_LENGTH];
	private int read_array_pos = 0;
	private StringBuffer message_stringbuffer = new StringBuffer(READ_ARRAY_LENGTH);
	private StringBuffer message_decrypt_stringbuffer = new StringBuffer(READ_ARRAY_LENGTH);

	// NEW MESSAGE SENDING SYSTEM
	public List messages_to_send = Collections.synchronizedList(new LinkedList<ByteBuffer>());
	public ByteBuffer cur_message = null;
	public long cur_message_send_start_time = 0;
	public int bytes_sent = 0;
	public int bytes_size = 0;
	public boolean queued_to_send = false;
	public boolean sending_disabled = false;

	public static ClientThread Get()
	{
		ClientThread object;

		semaphore_ClientThread.acquire();

		if (free_stack_top > 0)
		{
			free_stack_top--;
			object = free_stack[free_stack_top];
		}
		else
		{
			// Create a new ClientThread
			object = new ClientThread();
		}

		// Record that this object is not free.
		object.free = false;

		// No longer busy
		semaphore_ClientThread.release();

		return object;
	}

	public static void Free(ClientThread _object)
	{
		if (_object.free)
		{
			Output.PrintToScreen("ERROR: ClientThread.Free() called for ClientThread with hash " + _object.hashCode() + " that is already marked as being free!");
			Output.PrintStackTrace();
			return;
		}

		// TESTING -- Do not allow the same ClientThread to be in the free list more than once! Eventually this can be deleted, and left to the above.
		for (int i = 0; i < free_stack_top; i++)
		{
			if (free_stack[i] == _object)
			{
				Output.PrintToScreen("ERROR: ClientThread.Free() called for ClientThread with hash " + _object.hashCode() + " that is already in the free list!");
				Output.PrintStackTrace();
				return;
			}
		}

		semaphore_ClientThread.acquire();

		// Add the object to the free stack if there's room
		if (free_stack_top < FREE_STACK_LENGTH)
		{
			free_stack[free_stack_top] = _object;
			free_stack_top++;
		}

		// Record that this object is free.
		_object.free = true;

		// No longer busy
		semaphore_ClientThread.release();
	}

	public boolean IsFree()
	{
		return free;
	}

	public void Init(SocketChannel _socket_channel, int _client_index)
	{
		socket_channel = _socket_channel;
		client_index = _client_index;

		// Get this client's IP address
		clientIP = socket_channel.socket().getInetAddress().getHostAddress();

		// Initialize members
		player_account = null;
		clientID = "";
		client_activation_code = "";
		device_data = null;
		userID = -1;
		userIsAdmin = false;
		logged_in = false;
		in_game = false;
		prev_use_fine_time = 0;
		mean_use_interval = 60000; // Start at 1 minute
		log_suspect_expire_time = 0;
		fine_time = 0;
		cur_use_interval = 0;
		new_mean_use_interval = 0;
		bad_passwords_count = 0;
		bad_passwords_time = 0;
		read_array_pos = 0;

		// NEW MESSAGE SENDING SYSTEM
		messages_to_send.clear();
		cur_message = null;
		bytes_sent = 0;
		bytes_size = 0;
		sending_disabled = false;

		// Record the exact time at which this ClientThread started
		date = new Date();
		prev_use_fine_time = date.getTime();

		if ((WOCServer.log_flags & Constants.LOG_SEND) != 0) Constants.WriteToLog("log_send.txt", "ClientThread " + GetClientIndex() + " ID " + GetClientID() + " Init() called\n");
	}

	public void Reset()
	{
		try
		{
			// Close the SocketChannel
			if (socket_channel != null) {
				socket_channel.close();
			}
		}
		catch (Exception e)
		{
			Output.PrintException(e);
		}

		// Set the SocketChannel to null
		socket_channel = null;

		if ((WOCServer.log_flags & Constants.LOG_SEND) != 0) Constants.WriteToLog("log_send.txt", "ClientThread " + GetClientIndex() + " ID " + GetClientID() + " Reset() called\n");
	}

	public void InputReceived(CharBuffer _input_buffer)
	{
		int i, j;
		boolean exit_loop = false;

		// Append the input characters to the end of the read_array
		// and add the number of characters read to the read_array_pos
		int input_length = _input_buffer.length();
		for (i = 0; i < input_length; i++) {
			read_array[read_array_pos++] = _input_buffer.get(i);
		}

		// Log input from client we are tracking, if appropriate.
		if ((Admin.track_clientID.length() > 0) && (clientID.indexOf(Admin.track_clientID) != -1)) {
			Output.PrintToScreen("TRACKING: " + Constants.GetShortTimeString() + ": InputReceived() for client ID " + clientID + ": length " + input_length);
		}

		// If any complete messages have been received, process them.
		for (i = 0; i < read_array_pos; i++)
		{
			// If a null character is found in the data that's been read, process everything
			// up to that null character as a complete message.
			if (read_array[i] == '\0')
			{
				// Copy the complete message to the message_stringbuffer
				message_stringbuffer.setLength(0);
				message_stringbuffer.append(read_array, 0, i);

				// Process the message
				//Output.PrintToScreen("Message received: " + message_stringbuffer);
				exit_loop = MessageReceived();

				// Update the record of the previous time that the client was active, so that it won't be logged off.
				prev_use_fine_time = date.getTime();

				if (exit_loop) {
					break;
				}

				// Advance i past null terminator character at end of last message
				i++;

				// Copy remainder of data read down to beginning of read_array
				for (j = i; j < read_array_pos; j++)	{
					read_array[j - i] = read_array[j];
				}

				// Reset read_array_pos and i to start continue search for null chars from beginning of read_array
				read_array_pos -= i;
				i = -1;
			}
		}
	}

	private boolean MessageReceived()
	{
		// Record the exact time at which this message was received
		date = new Date();
		fine_time = date.getTime();

		// Determine time since last message received from this thread's client
		cur_use_interval = fine_time - prev_use_fine_time;

		// Record time when last message received from this client
		prev_use_fine_time = fine_time;

		if (cur_use_interval < 100000)
		{
			// Determine new mean_use_interval for this client, weighting old and new intervals appropriately.
			mean_use_interval = (long)((mean_use_interval * Constants.OLD_USE_INTERVAL_WEIGHT) + (cur_use_interval * Constants.NEW_USE_INTERVAL_WEIGHT));

			//Output.PrintToScreen("Mean use interval: " + mean_use_interval);
			if (mean_use_interval < Constants.MEAN_USE_INTERVAL_BOOT_THRESHOLD)
			{
				// The client's input is too frequent; boot the client.
				UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, GetUserID(), false);
				Constants.WriteToLog("log_hack.txt", Constants.GetTimestampString() + ": User " + userID + ((userID > 0) ? (" (" + userData.name + ")") : "") + ", IP " + clientIP + " booted, mean use interval " + mean_use_interval + ".\n");

  			// Queue a quit client event for processing, to boot this client
				QueueQuitClientEvent();

				return true;
			}
			else if (mean_use_interval < Constants.MEAN_USE_INTERVAL_IGNORE_THRESHOLD)
			{
				// Continue without queueing input event -- thereby ignoring it.
				UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, GetUserID(), false);
				Output.PrintToScreen("Ignoring input from user " + userID + ((userID > 0) ? (" (" + userData.name + ")") : "") + ", IP " + clientIP + " because mean_user_interval is " + mean_use_interval + ".");
				return false;
			}
		}

/*
  	// Decrypt the message string
		message_decrypt_stringbuffer.setLength(0);
		int len = message_stringbuffer.length();
		int ch;
		for (int i = 0; i < len; i++)
		{
			ch = message_stringbuffer.charAt(i);
			if ((ch >= 65) && (ch <= 122)) ch = (57 - (ch - 65)) + 65;

			message_decrypt_stringbuffer.append((char)ch);
		}
*/
		if (log_suspect_expire_time > fine_time)
		{
			UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, GetUserID(), false);
			if ((userData != null) && (userData.nationID > -1))
			{
				NationData nationData = (NationData)DataManager.GetData(Constants.DT_NATION, userData.nationID, false);
				Constants.WriteToLog("log_suspect.txt", Constants.GetFullDate() + ": Nation " + userData.nationID + ((userData.nationID > 0) ? (" (" + nationData.name + ")") : "") + ", User " + userID + ((userID > 0) ? (" (" + userData.name + ")") : "") + ", IP " + clientIP + ", mean use interval: " + mean_use_interval + ".\n");
				Constants.WriteToLog("log_suspect.txt", "          " + message_stringbuffer + "\n");
			}
		}

		// Queue the input for processing
		WOCServer.QueueInput(this, userID, message_stringbuffer, fine_time);

		return false;
	}

	public void TerminateAndSendNow(StringBuffer _output_buffer)
	{
		if (_output_buffer.length() == 0) {
			return;
		}

		// Add end marker to output string
		Constants.EncodeString(_output_buffer, "end");

		// NULL terminate output string so that it can be sent.
		_output_buffer.append('\u0000');

//    // NOTE: Outputting here in the client thread will be blocking, if an admin user is connected.
//		Output.PrintToScreen("Output event: '" + _output_buffer.toString() + "'");
//		Output.PrintToScreen("TerminateAndSendNow(), _output_buffer length: " + _output_buffer.length());
//		Output.PrintStackTrace();

		// Send the output string to the client
		SendNow(_output_buffer.toString());
	}

	public void SendNow(String _message)
	{
		// Do nothing if this ClientThread has no SocketChannel
		if (socket_channel == null) {
			return;
		}

		// FOR TESTING ONLY:
		long send_start_time = Constants.GetFreshFineTime();

		if ((WOCServer.log_flags & Constants.LOG_SEND) != 0) Constants.WriteToLog("log_send.txt", "ClientThread " + GetClientIndex() + " ID " + GetClientID() + " SendNow() called for message of length " + _message.length() + ".\n");

		// Log output to client we are tracking, if appropriate.
		if ((Admin.track_clientID.length() > 0) && (clientID.indexOf(Admin.track_clientID) != -1)) {
			Output.PrintToScreen("TRACKING: " + Constants.GetShortTimeString() + ": SendNow() for client ID " + clientID + ": sending length " + _message.length() + ", start: " + _message.substring(0, 15));
		}

		// Acquire the send semaphore
		semaphore_send.acquire();

		try
		{
			ByteBuffer buf = ByteBuffer.wrap( _message.getBytes(Constants.charset) );

			/// NEW SYSTEM
			QueueMessageToSend(buf);

			/*
			/// OLD SYSTEM
			// Send message in a loop, because it may take more than one call to to write() to send the complete message.
			int bytes_sent = 0;
			int size = buf.limit();
			while (bytes_sent < size)
			{
				// FOR TESTING ONLY:
    		long write_start_time = Constants.GetFreshFineTime();

				bytes_sent += socket_channel.write(buf);
				//Output.PrintToScreen("SendNow() sent " + bytes_sent + " of " + size + " bytes.");

				// Check whether the above individual call to write() took too long. It shouldn't, because the SocketChannel is in non-blocking mode. Instead, the below check for how long the current loop of write()s has taken should be sufficient.
				if (Constants.GetFreshFineTime() - write_start_time > 2000)
				{
					Output.PrintToScreen(">> socket_channel.write() took " + (Constants.GetFreshFineTime() - write_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + size + " bytes)\n");
					Constants.WriteToLog("log_lag.txt", "\n " + Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " !!!socket_channel.write() TOOK " + (Constants.GetFreshFineTime() - write_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + size + " bytes)\n");
				}

				// Because the SocketChannel is in non-blocking mode, it should return immediately even if no bytes are written. So by checking how long it's taken since we've started trying to send this message, we should be able to abort a send that takes too long.
				// Why does it take so long? If sends to the same client repeatedly abort, that client may need to be disconnected.
				if (Constants.GetFreshFineTime() - send_start_time > 5000)
				{
					Output.PrintToScreen(">> socket_channel.write() loop aborted after taking " + (Constants.GetFreshFineTime() - send_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + size + " bytes)\n");
					Output.PrintStackTrace();
					Constants.WriteToLog("log_lag.txt", "\n " + Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " !!!socket_channel.write() loop aborted after taking " + (Constants.GetFreshFineTime() - send_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + size + " bytes)\n");
					break;
				}
			}
			*/

//			out.println(_message);
		}
		catch (Exception e)
		{
      // NOTE: Since re-implementing net code using the Nio library, there are frequent exceptions here, with the following message:
      // java.io.IOException: An established connection was aborted by the software in your host machine.
      // This seems to have no ill effects, but it may mean some messages don't make it to the clients.
			//Output.PrintToScreen("**ERROR: ClientThread.SendNow() exception: " + e + "\n ClientThread " + GetClientIndex() + " ID " + GetClientID() + ", IP: " + clientIP + ", isConnected(): " + socket_channel.isConnected());
			//Output.PrintException(e);
		}

		// Release the send semaphore
		semaphore_send.release();

		if (Constants.GetFreshFineTime() - send_start_time > 2000)
		{
			Output.PrintToScreen(">> ClientThread.SendNow() took " + (Constants.GetFreshFineTime() - send_start_time) + " MS! (userID " + userID + ")\n");
			Constants.WriteToLog("log_lag.txt", "\n " + Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " !!!ClientThread.SendNow() TOOK " + (Constants.GetFreshFineTime() - send_start_time) + " MS! (userID " + userID + ")\n");
		}
	}

	// NEW MESSAGE SENDING SYSTEM

	public void QueueMessageToSend(ByteBuffer _message)
	{
		// If sending to this client has been disabled, do not queue the message.
		if (sending_disabled) {
			return;
		}

		// Queue this message to be sent.
		messages_to_send.add(_message);
		if ((messages_to_send.size() > 100) && ((messages_to_send.size() % 25) == 0)) Output.PrintToScreen("WARNING: ClientThread.QueueMessageToSend() messages_to_send list has size " + messages_to_send.size()); // TESTING

		// If this client is not yet queued to have messages sent to it...
		if (!queued_to_send)
		{
			// Add this client to the server's list of clients to send to, and record that it is in that list.
			WOCServer.message_senders.add(this);
			if (WOCServer.message_senders.size() > 100) Output.PrintToScreen("WARNING: ClientThread.QueueMessageToSend() message_senders list has size " + WOCServer.message_senders.size()); // TESTING
			queued_to_send = true;
		}
	}

	public boolean SendMessage()
	{
		// If we're not currently in the process of sending a message...
		if (cur_message == null)
		{
			// Advance to the first message in the queue.
			cur_message = (messages_to_send.size() == 0) ? null : (ByteBuffer)(messages_to_send.get(0));
			bytes_size = (cur_message == null) ? 0 : cur_message.limit();
			bytes_sent = 0;
			cur_message_send_start_time = Constants.GetFineTime();
		}

		// If we're in the process of sending a message...
		if (cur_message != null)
		{
			try
			{
				// Send part of the current message.
				bytes_sent += socket_channel.write(cur_message);
			}
			catch (Exception e)
			{
				// NOTE: Since re-implementing net code using the Nio library, there are frequent exceptions here, with the following message:
				// java.io.IOException: An established connection was aborted by the software in your host machine.
				// This seems to have no ill effects, but it may mean some messages don't make it to the clients.
				//Output.PrintToScreen("**ERROR: ClientThread.SendNow() exception: " + e + "\n ClientThread " + GetClientIndex() + " ID " + GetClientID() + ", IP: " + clientIP + ", isConnected(): " + socket_channel.isConnected());
				//Output.PrintException(e);
			}

			// If the entire current message has been sent...
			if (bytes_sent >= bytes_size)
			{
				// We're done sending this message, remove it from the list and reset message send info.
				messages_to_send.remove(0);
				cur_message = null;
				bytes_size = 0;
				bytes_sent = 0;
			}
			else if ((Constants.GetFineTime() - cur_message_send_start_time) > 30000)
			{
				// Log this error
				Output.PrintToScreen(">> SendMessage() client " + client_index + " send aborted after taking " + (Constants.GetFineTime() - cur_message_send_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + bytes_size + " bytes)");
				Constants.WriteToLog("log_lag.txt", "\n " + Constants.GetShortDateString() + " " + Constants.GetShortTimeString() + " SendMessage() client " + client_index + " send aborted after taking " + (Constants.GetFineTime() - cur_message_send_start_time) + " MS! (userID " + userID + ", sent " + bytes_sent + " of " + bytes_size + " bytes)");

				// This message has taken too long to send. Clear this client's message queue and exit it from the server.
				messages_to_send.clear();
				cur_message_send_start_time = 0;
				bytes_size = bytes_sent = 0;

				// Record that sending to this client is disabled, so no more messages will be sent to it.
				sending_disabled = true;

				// Queue an input event as is this client has been quit out.
				QueueQuitClientEvent();

				// Return true; this client has no more messages to be sent to it.
				return true;
			}
		}

		// Return true if there are no more messages to send to this client.
		return (messages_to_send.size() == 0);
	}

	public boolean IsQueuedToSend()
	{
		return queued_to_send;
	}

	public void QueueQuitClientEvent()
	{
		message_decrypt_stringbuffer.setLength(0);
		message_decrypt_stringbuffer.append("action=quit_client");
		WOCServer.QueueInput(this, userID, message_decrypt_stringbuffer, fine_time);
	}

	public int GetClientIndex() {
		return client_index;
	}

	public void SetClientVersion(int _client_version) {
		client_version = _client_version;
	}

	public int GetClientVersion() {
		return client_version;
	}

	public void SetDeviceData(String _clientID, String _clientUID, DeviceData _device_data)
	{
		clientID = _clientID;
		clientUID = _clientUID;
		device_data = _device_data;

		// Log input from client we are tracking, if appropriate.
		if ((Admin.track_clientID.length() > 0) && (clientID.indexOf(Admin.track_clientID) != -1)) {
			Output.PrintToScreen(Constants.GetShortTimeString() + ": SetDeviceData() for client ID " + clientID + ", UID: " + _clientUID);
		}
	}

	public void SetClientActivationCode(String _activation_code)
	{
		client_activation_code = _activation_code;
	}

	public void SetPlayerAccount(PlayerAccountData _player_account)
	{
		player_account = _player_account;
	}

	public void SetGameInfo(int _userID, boolean _userIsAdmin, boolean _logged_in)
	{
		//Output.PrintToScreen("ClientThread.SetGameInfo() clientThread " + GetClientIndex() + " ID " + GetClientID() + ", userID: " + _userID + ", _logged_in: " + _logged_in);
		userID = _userID;
		userIsAdmin = _userIsAdmin;
		logged_in = _logged_in;
	}

	public int GetUserID(){
		return userID;
	}

	public int GetPlayerID(){
		return (player_account == null) ? -1 : player_account.ID;
	}

	public boolean UserIsAdmin(){
		return userIsAdmin;
	}

	public boolean UserIsLoggedIn(){
		return logged_in;
	}

	public boolean UserIsInGame() {
		return in_game;
	}

	public String GetClientID(){
		return clientID;
	}

	public String GetClientActivationCode(){
		return client_activation_code;
	}

	public String GetClientIP(){
		return clientIP;
	}

	public DeviceData GetDeviceData() {
		return device_data;
	}

	public SocketChannel GetSocketChannel() {
		return socket_channel;
	}

	public void BadPasswordEntered()
	{
		// Get the current time
		int cur_time = Constants.GetTime();

		if ((cur_time - bad_passwords_time) >= Constants.BAD_PASSWORD_PERIOD)
		{
			// Start a new bad password period
			bad_passwords_time = cur_time;
			bad_passwords_count = 1;
		}
		else
		{
			// Increment the count of bad passwords given during the current bad password period
			bad_passwords_count++;
		}
	}

	public int GetTimeBeforePasswordAllowed()
	{
		// Get the current time
		int cur_time = Constants.GetTime();

		if (bad_passwords_count >= Constants.BAD_PASSWORD_MAX_COUNT) {
			return Math.max(0, Constants.BAD_PASSWORD_PERIOD - (cur_time - bad_passwords_time));
		}	else {
			return 0;
		}
	}

	public void CaptchaFailed(String _event)
	{
		UserData userData = (UserData)DataManager.GetData(Constants.DT_USER, GetUserID(), false);
		Constants.WriteToLog("log_hack.txt", Constants.GetTimestampString() + " Captcha " + _event + " for user " + ((userData == null) ? "[none]" : userData.name) + " (" + GetUserID() + "), IP: " + GetClientIP() + ", device ID: " + GetDeviceData().ID + ".\n");
	}
}
