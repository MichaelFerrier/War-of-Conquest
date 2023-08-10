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

class SocketPolicyServer extends Thread
{
    public static final String POLICY_XML =
            "<?xml version=\"1.0\"?>"
                    + "<cross-domain-policy>"
                    + "<allow-access-from domain=\"*\" to-ports=\"*\" />"
                    + "</cross-domain-policy>";

    public void run()
		{
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(2000);//843);
        } catch (IOException e) {e.printStackTrace();}
        while(true){
            try {
                final Socket client = ss.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
														System.out.println("About to write socket policy.");
                            client.setSoTimeout(10000); //clean failed connections
                            client.getOutputStream().write(SocketPolicyServer.POLICY_XML.getBytes());
                            client.getOutputStream().write(0x00); //write required endbit
                            client.getOutputStream().flush();
														System.out.println("Finished writing socket policy.");
														BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
														//reading two lines empties flashs buffer and magically it works!
                            in.readLine();
                            in.readLine();
														System.out.println("Finished reading for socket policy.");
												} catch (IOException e) {
                        }
                    }
                }).start();
            } catch (Exception e) {}
        }
    }
};
