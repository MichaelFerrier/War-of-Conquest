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
import org.finj.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.SimpleDateFormat;
import WOCServer.WOCServer;
import WOCServer.DataManager;
import WOCServer.Output;
import WOCServer.Constants;
import WOCServer.InputEvents;

public class BackupThread extends Thread
{
	static boolean force_backup = false;
	static int prev_sleep_time = 0;

	public BackupThread()
	{
		super("BackupThread");

		// Make sure that all the necessary directories exist.
		Constants.EnsureDirExists(Constants.backup_dir);
		Constants.EnsureDirExists(Constants.backup_dir + "/daily");
		Constants.EnsureDirExists(Constants.backup_dir + "/weekly");
		Constants.EnsureDirExists(Constants.backup_dir + "/monthly");
	}

	public static void ForceBackup()
	{
		force_backup = true;
	}

	public void run()
	{
		// Update cycle
		for (;;)
		{
			// Sleep for a while, until the next check. (Done first to give server a chance to start up.)
			try{
			  Thread.sleep(Constants.BACKUP_THREAD_SLEEP_MILLISECONDS);
				prev_sleep_time = Constants.GetTime();
			}catch(java.lang.InterruptedException e){Output.PrintTimeToScreen("BackupThread Insomnia");}

			// If the backup thread is not active, continue to next loop iteration.
			if (!WOCServer.backup_thread_active) {
				continue;
			}

			// Back up data, if necessary.

			// Determine current backup period
			int new_backup_period = (Constants.GetTime() - Constants.BACKUP_PERIOD_OFFSET) / Constants.BACKUP_PERIOD;
      //Output.PrintToScreen("new_backup_period: " + new_backup_period + ", stored backup period: " + GlobalData.instance.cur_backup_period);

			if (new_backup_period != GlobalData.instance.cur_backup_period)
			{
				// Backup the game data
				Backup();

				// Store new backup period
				GlobalData.instance.cur_backup_period = new_backup_period;
				DataManager.MarkForUpdate(GlobalData.instance);
			}
			else if (force_backup)
			{
				// Backup the game data
				Backup();
			}

			// Reset force_backup to false
			force_backup = false;
		}
	}

	void Backup()
	{
		Output.PrintToScreen("About to backup data...");

		// Determine backup filenames with timestamp
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM-dd-yyyy_HH-mm");
    Date now = new Date();
    String timestamp = sdfDate.format(now);
		String serverBackupFilename = "woc_server_backup_" + Constants.GetServerID() + "_" + timestamp + ".zip";
		String playerBackupFilename = "woc_player_backup_" + timestamp + ".zip";
		String serverBackupRemoteFilename = "woc_server_backup_" + Constants.GetServerID() + ".zip";
		String playerBackupRemoteFilename = "woc_player_backup_" + Constants.GetServerID() + ".zip";

		// Determine whether to back up the player DB (if it is on this same server), and whether to store the new backup in the weekly and monthly directories.
		boolean backupPlayerDB = Constants.account_db_url.equals(Constants.game_db_url);
		boolean storeInWeekly = (Constants.GetDay() == 0);
		boolean storeInMonthly = (Constants.GetDate() == 1);

		// Create the game server data backup
		CreateBackup(Constants.game_db_user, Constants.game_db_pass, Constants.server_db_name, serverBackupFilename, serverBackupRemoteFilename, storeInWeekly, storeInMonthly);

		// Create the player account data backup if appropriate
		if (backupPlayerDB) {
			CreateBackup(Constants.account_db_user, Constants.account_db_pass, "ACCOUNTS", playerBackupFilename, playerBackupRemoteFilename, storeInWeekly, storeInMonthly);
		}

		Output.PrintToScreen("Done backing up data.");
	}

	public void CreateBackup(String _db_user, String _db_pass, String _db_name, String _filename, String _remote_filename, boolean _storeInWeekly, boolean _storeInMonthly)
	{
		// Determine path to store temporary file
		String savePathSql = Constants.backup_dir + "/backup.sql";
		String savePathZip = Constants.backup_dir + "/backup.zip";

		try
		{
			// Create a cmd command
			String executeCmd = "mysqldump -u" + _db_user + " -p" + _db_pass + " --database " + _db_name + " -r " + savePathSql;

			// Execute the command
			Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
			int processComplete = runtimeProcess.waitFor();

			// processComplete=0 if correctly executed, will contain other values if not.
			if (processComplete != 0)
			{
				Output.PrintToScreen("Failed to create backup for database " + _db_name + " at path '" + savePathSql + "'.");
				return;
			}
    }
		catch (IOException | InterruptedException ex)
		{
			Output.PrintToScreen("Exception when trying to create backup for database " + _db_name + " at path '" + savePathSql + "': " + ex.getMessage());
			return;
    }

		// Zip the backup file
		zipFile(savePathSql, savePathZip);

		// Copy the backup file to the daily subdirectory
		Constants.CopyFile(savePathZip, Constants.backup_dir + "/daily/" + _filename);

		// Copy the backup file to the weekly subdirectory if appropriate
		if (_storeInWeekly) Constants.CopyFile(savePathZip, Constants.backup_dir + "/weekly/" + _filename);

		// Copy the backup file to the monthly subdirectory if appropriate
		if (_storeInWeekly) Constants.CopyFile(savePathZip, Constants.backup_dir + "/monthly/" + _filename);

		UploadBackupFile(savePathZip, _remote_filename);

		// Delete the temp files
		File file;
		file= new File(savePathSql);
    file.delete();
		file = new File(savePathZip);
    file.delete();

		// Delete backup files that are older than a threshold age.
		DeleteOlderFiles(Constants.backup_dir + "/daily", Constants.SECONDS_PER_DAY * 14);
		DeleteOlderFiles(Constants.backup_dir + "/weekly", Constants.SECONDS_PER_DAY * 65);
		DeleteOlderFiles(Constants.backup_dir + "/monthly", Constants.SECONDS_PER_DAY * 365);
	}

	public void zipFile(String _sourceFilename, String _destFilename)
	{
		try
		{
			File toFile = new File(_destFilename);

			FileOutputStream fos = new FileOutputStream(toFile);
			ZipOutputStream outs = new ZipOutputStream(fos);

			File srcFile = new File(_sourceFilename);
			String filepath = srcFile.getAbsolutePath();
			//String dirpath = dir.getAbsolutePath();
			String entryName = srcFile.getName();//_sourceFilename;//filepath;//.substring(dirpath.length() + 1).replace('\\', '/');
			ZipEntry zipEntry = new ZipEntry(entryName);
			zipEntry.setTime(srcFile.lastModified());
			FileInputStream ins = new FileInputStream(srcFile);
			outs.putNextEntry(zipEntry);

			byte[] readBuffer = new byte[2048];
			int amountRead;
			int written = 0;

			while ((amountRead = ins.read(readBuffer)) > 0) {
					outs.write(readBuffer, 0, amountRead);
					written += amountRead;
			}

			outs.closeEntry();
			ins.close();
			outs.close();
		}
		catch (Exception e) {
			Output.PrintToScreen("*** BackupThread: failed to zip file '" + _sourceFilename + "':" + e.getMessage());
		}
	}

	public void UploadBackupFile(String _source_filename, String _dest_filename)
	{
		try
		{
			// Open ftp connection and transfer file
		  FTPClient ftpClient = new FTPClient();
		  ftpClient.open(Constants.ftp_host); // connect to FTP server
		  ftpClient.login(Constants.ftp_username, Constants.ftp_password.toCharArray()); // login
		  ftpClient.setDataType(FTPConstants.IMAGE_DATA_TYPE); // set to binary mode transfer
		  ftpClient.setWorkingDirectory("/backup"); // change directory
			ftpClient.usePassiveDataTransfer(true); // Use passive mode so only outgoing connections are established. (Better for firewall/security.)
		  File file = new File(_source_filename);
		  FileInputStream in = new FileInputStream(file);
			ftpClient.putFile(in, _dest_filename);
		  in.close();
		  ftpClient.close();
    }
	  catch (Exception exception)
	  {
			Output.PrintToScreen("Exception while attempting to upload backup file to server: " + exception);
			Output.PrintException(exception);
    }
	}

	public void DeleteOlderFiles(String _dir_path, int _threshold_age_in_seconds)
	{
		// Cutoff time
		long cutoff_time = (new Date().getTime()) - (_threshold_age_in_seconds * 1000);

	  File dir = new File(_dir_path);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null)
		{
			for (File child : directoryListing)
			{
				if (child.lastModified() <= cutoff_time)
				{
					Output.PrintToScreen("Deleting old backup " + child.getName());
					child.delete();
				}
			}
		}
	}
}
