War of Conquest Server
=========================

This repository contains all the code needed to build and run the War of Conquest server. The source code is provided under the [GPLv3 license](LICENSE). Below you’ll find a FAQ followed by setup and usage instructions.


F.A.Q.
======

Q. What part of War of Conquest has been made open source?
----------------------------------------------------------------------------------
A. The server code for the “new” War of Conquest (2016) has been made open source under the GPLv3 license. This means you can redistribute it, modify it, and use it for commercial or non-commercial purposes, subject to the terms of the GPLv3 license.

Q. What about the War of Conquest client?
----------------------------------------------------------------------------------
A. The War of Conquest client has not been made open source. It contains and depends on a lot of code, graphics, 3D models, and other assets that are not open source, and so I do not have the right to distribute them. The War of Conquest client will not be developed any further, so if you would like to run a server you will need to ensure that it remains compatible with the existing War of Conquest client. Alternatively, you can develop your own new client. This would allow you the freedom to modify the server code without being limited to maintaining compatibility with the existing client. If you are interested in doing this, let me know and I can share with you the parts of the client source code that I have the right to distribute and that are useful for interfacing with the server.

Q. What about the original, or “classic” War of Conquest?
----------------------------------------------------------------------------------
A. The rights to further develop the original War of Conquest were licensed to Kete Games in 2011. From what I understand they have recently resumed work on an updated version of classic War of Conquest. Neither the original War of Conquest client nor server are being made open source, and neither of them are available for licensing or purchase.

Q. Can I buy the rights to War of Conquest?
----------------------------------------------------------------------------------
A. No. The rights to develop the original War of Conquest client and server are currently licensed to Kete Games. The new War of Conquest server has been made open source under the GPLv3 license. The new War of Conquest client is not being sold or open sourced, though I will make some of its source code available to anyone interested in developing a new client.

Q. Can I run a War of Conquest server that continues with the world state of one of the “official” servers?
----------------------------------------------------------------------------------
A. No, the databases for the “official” War of Conquest servers contain lots of personal information, such as e-mail addresses and security questions, that it would not be in good faith for me to resell or distribute. If you would like to run a War of Conquest server, it will need to be a new game world starting from scratch, with no previously existing player accounts.

Q. Will you help me set up a War of Conquest server?
----------------------------------------------------------------------------------
A. I’d be glad to answer the occasional question should you run into a problem. That said, you will need some computer expertise to set up and run a War of Conquest server, and especially if you are looking to modify its source code. I will not hold hands during this process or provide support once a server is running. Between Google, Slashdot, and new tools like ChatGPT, there are plenty of resources for someone knowledgeable about computers to solve whatever problems might come up.

Q. Can I use the “War of Conquest” name for my own server?
----------------------------------------------------------------------------------
A. You can use the War of Conquest trademark if your server is compatible with the War of Conquest client. If you’ve created your own custom client to work with your customized server, you’ll need to come up with a different name.

Q. Why are you shutting down the official servers and open sourcing the code?
—-------------------------------------------------------------------------------------------------------------------
A. War of Conquest has some deep game balance issues that I haven’t managed to resolve. It’s an open world game where skill and effort, as well as a bit of luck, lead to success, and where success for some players means failure for others. The result is that the largest teams – especially when they have the most time to play and the most devices to play on, and sometimes when they discover the newest tricks and loopholes – are able to completely dominate the game, making it very little fun for other players. This dynamic has driven away most players, and makes the game largely unappealing to new players. I haven’t succeeded in solving this problem, though I tried hard for several years. What’s more, negativity seems to thrive among the player base of WoC, with harassment, bullying, and threats being commonplace. I don’t want anything to do with that. Finally, my interests have just moved on to other things. However, War of Conquest still seems to be enjoyable, and even meaningful, to a handful of players, so rather than shut it down completely I’m open sourcing the server so that others can run their own game worlds, and maybe find solutions to these game balance and community problems.


Compiling the Server
=========================

Install the latest Java JDK and JRE from here:
https://www.oracle.com/java/technologies/downloads/
Make sure the locations of the java and javac commands have been added to your path, so that they are available at the command line.
Download the required .jar files for common Java libraries. These files are:
json-simple-1.1.1.jar
mysql-connector-java-5.1.34-bin.jar
finj-1.1.5.jar
commons-codec-1.10.jar
ij.jar

They can be downloaded from these URLs:

http://www.java2s.com/Code/Jar/j/Downloadjsonsimple111jar.htm
http://www.java2s.com/ref/jar/download-mysqlconnectorjava5134jar-file.html
https://sourceforge.net/projects/finj/files/finj-api/
http://www.java2s.com/ref/jar/download-commonscodec110jar-file.html
http://www.java2s.com/Code/Jar/i/Downloadijjar.htm

Once downloaded, create a directory server/java libs/ and copy the .jar files into that directory. The compile script, compile.bat, adds that directory to the class path so that the compiler knows to look in it for the .jar files.
From a command line, cd to the “server” directory and run compile.bat. This will compile the server code. The compiled .class files will be located in the WOCServer subdirectory. These are the .class files you will want to copy into the WOCServer subdirectory on your game server machine, later.

Setting Up a Server Machine
=============================

First you will need to have space on a cloud server where you can run Java, a web server, and Php, as well as MySQL. I went to Linode.com (now Akamai.com) and set up a 4GB dedicated instance running the Ubuntu operating system. (These currently cost $36 / month as of August 2023.) I then performed the following steps. (Note that this was done in 2016, so you may need to adapt some of these steps.)

Create a Linode instance. (https://www.linode.com/docs/getting-started)
Putty can be used to access it via ssh. Take note of the IP address you will need to access it via Putty (command line) and SFTP (for uploading/downloading files).
Deploy Ubuntu, the most recent stable version, and take note of the root user password.
Boot the server instance.
Turn on automatic weekly backups for the server, if you’d like.
Connect using Putty (ssh). Log in as root, using password.
Follow the guides to create a host name, set time zone, and create a non-root user. Take note of the non-root user’s username and password, this is what you will most often be logging in as, via ssh.
Set up the Apache web server. (the web server is used to enable access to log files over the web.)
Install MySQL, create user (eg. “woc2”), and take note of the MySQL root password.
FileZilla can be used to SFTP files to the server, using the server’s non-root admin username and password, and IP address, on port 22.
Install Java8 as described here: http://tecadmin.net/install-oracle-java-8-jdk-8-ubuntu-via-ppa/
Install the War of Conquest server files in home/admin/server/. Create the directory home/admin/server, and within it create these subdirectories:
home/admin/server/generated/
home/admin/server/generated/backup/
home/admin/server/generated/backup/daily/
home/admin/server/generated/backup/monthly/
home/admin/server/generated/backup/weekly/
home/admin/server/generated/clientmaps/
home/admin/server/generated/log/
home/admin/server/generated/moderators/
home/admin/server/generated/nationlog/
home/admin/server/generated/publiclog/
home/admin/server/generated/ranks/
home/admin/server/images/
home/admin/server/WOCServer/
Within the “WOCServer” subdirectory directory, copy in all of the War of Conquest server .class files. Within the home/admin/server/ directory, copy in all the .tsv and .json files, and the map .png files, as well as server.sh. Within the home/admin/server/images/ directory, upload all of the image files from that directory in the repository.
Make server.sh executable using “chmod +x server.sh”. It can now be run using: “./server.sh”
The War of Conquest server also depends on a few common java libraries. These .jar files are:

json-simple-1.1.1.jar
mysql-connector-java-5.1.34-bin.jar
finj-1.1.5.jar
commons-codec-1.10.jar
ij.jar

They can be downloaded from these URLs:

http://www.java2s.com/Code/Jar/j/Downloadjsonsimple111jar.htm
http://www.java2s.com/ref/jar/download-mysqlconnectorjava5134jar-file.html
https://sourceforge.net/projects/finj/files/finj-api/
http://www.java2s.com/ref/jar/download-commonscodec110jar-file.html
http://www.java2s.com/Code/Jar/i/Downloadijjar.htm

Once downloaded, you need to tell Java where to find them when the server is run. Depending on the version of Java you’ve installed, that either means simply copying those .jar files into the Java install’s ext directory (/usr/lib/jvm/java-8-oracle/jre/lib/ext), or for more recent versions, you may need to copy the .jar files into the server/ directory and then modify the server.sh script to add those files to the class path, something like this:

java -cp .:json-simple-1.1.1.jar:mysql-connector-java-5.1.34-bin.jar:finj-1.1.5.jar:commons-codec-1.10.jar:ij.jar -Xms512m -Xmx2048m WOCServer.WOCServer


Before running the server, you must create the databases and grant the server’s mysql user all permissions to those databases:
mysql -u root -p [enter MySQL root password]
create database ACCOUNTS;
create database WOC1;
grant all on ACCOUNTS.* to ‘woc2’;
grant all on WOC1.* to ‘woc2’;
In order to automatically upload regular backups to a different server via FTP, the Config.json file must contain the backup server’s FTP info.
Xvfb must be installed, to provide a virtual frame buffer for the imagej library, which is used to generate the UI map image. First install xvfb:
sudo apt-get install xvfb
Then install various libraries it depends on:
sudo apt install libXrender1
sudo apt install libXtst6
sudo apt install libXi6
(For more info: http://elementalselenium.com/tips/38-headless, https://askubuntu.com/questions/674579/libawt-xawt-so-libxext-so-6-cannot-open-shared-object-file-no-such-file-or-di)
To prevent MySQL connections from timing out after the default 8 hours of inactivity, add the following to etc/mysql/my.cnf (if done by FTP, log in as root so it’s editable):
[mysql]
wait_timeout=2592000
Then to restart mysql:
service mysql restart
As above, in my.cnf set:
max_allowed_packet	= 500M
Likewise, if done via FTP log in as root. Then restart mysql.
On the server that hosts the player account MYSQL DB,which must be accessed remotely, FTP login using root account and edit file etc/mysql/my.cnf to comment out (place # before) the line “bind-address = 127.0.0.1”. Doing so will allow remote connections. Then type “sudo service mysql restart”.
For security, activate Fail2Ban on the server (to temp ban after 3 failed attempts to log in to ssh) following instructions here: https://www.linode.com/docs/security/using-fail2ban-for-security/
Default configuration is fine.
Install and use Uncomplicated Firewall (ufw) as described here:
https://www.linode.com/docs/security/firewalls/configure-firewall-with-ufw/
The following commands are needed to open the necessary ports:
sudo ufw allow 2001
sudo ufw allow http
sudo ufw allow https
sudo ufw allow ssh
The following only applies if you are running multiple game servers that use the same player account database:
On the server hosting the account DB, open the MySQL port to only the IP addresses of the other WoC servers. Eg.:
sudo ufw allow from 104.237.143.194 to any port 3306
Enable ufw:
sudo ufw enable
You can see all active rules like so:
sudo ufw status
If your server is intended to work with the official War of Conquest client, you will need to let me know the IP address of your new server so that I can add it to the list of server options that the client will display to players. You can email me at contact@ironzog.com. I will then give you the server_id number that you can set in the config.json file.
In the new server’s server/config.json, change the server_id. Each War of Conquest server that works with the official client must have its own unique ID, so I will let you know your server’s ID when you send me the new server’s IP address.
Also in the new server’s server/config.json file, set account_db_url to the MySQL url of your new server, for example jdbc:mysql://45.56.124.170/

Assorted notes you may eventually find handy:
------------------------------------------------------------
To Find out the PID of a Java process, use the command “ps -el|grep java” The 4th column in the output is the PID. Or, just use “jps” which comes with the JDK.
To kill a Java process that has hung and output the stack of each thread to a text file, use the command “kill -3 <pid> > kill.txt” NOTE: This doesn’t seem to kill it, but in screen, it lists the stack trace for threads. To scroll up in screen hit ctrl+a then esc (to enter copy mode) then use arrow keys to scroll. (On my laptop, it’s ctrl+fn+right-shift)
To capture heap dump: “jmap -dump:live,file=<file-path.hprof> <pid>”
To restore server DB from backup:
I can use the unzip command to unzip the backup file in place, then use cp to copy it up:
cp backup.sql ../../../
Copy or upload backup.sql int the dir I will run mysql from. Then:
mysql -u root -p [enter root password]
use WOC1;
source backup.sql;
To monitor network traffic: Log on as root, run iftop and glances.

Updating a Linode Server
-------------------------------------------------

Log in as root user.
Run command: apt-get update && apt-get upgrade
This may also update the Java JVM, which may remove the ext .jar files you’ve installed. To fix this, FTP to the server (using the root username) and upload the .jar files into /usr/lib/jvm/java-8-oracle/jre/lib/ext. These include:
commons-codec-1.10.jar
finj-1.1.5.jar
ij.jar
json-simple-1.1.1.jar
mysql-connector-java-5.1.34-bin.jar

Running the Game Server
===============================
Before running the server, type “screen”. Cd to the “server” directory. Then start the server with “./server.sh”. Before closing the ssh session, detach the server program from the session using ctrl+a followed by ‘d’. Then you can close the session and come back later. When you come back, you can re-attach to the server program using “screen -dr”. If you want to kill a screen session, you can re-attach to it and then type ctrl+a followed by ‘k’. (Note: the “screen” tool allows a command line session, with the server running, to continue running even though you’re no longer connected to it via ssh. Without using screen, the command line sessions, and so the WoC server, would stop running as soon as you disconnect your ssh connection.)
Once the server is running, at the command line, you can input various commands to tell the server what to do. The “help” command will list all of these for you. (Alternatively, you can look through the source file Admin.java, where all the commands are defined.) Some commands take parameters. Commands that take more than one parameter require that they be separated with the | character. For example, “rename_nation old=StarSeeker|new=StarWarrior”
Before players can use the server, it needs to have a world landscape. There are three parts to this process. First, you can have the server generate an image file that specifies where land and water will go, and where resources will be placed. This is done using the “generate_landscape” command. (You can have the server generate a random landscape image in this way, or you can use the provided landgen_with_orbs.png. Or, you can create this image file by hand in a paint program, but it’s a lot of work.) Then, in a paint program, you can touch up the image, and add dots of particular colors to represent the positions of the orbs. Finally, you load the landscape into the server’s database using the “load_landscape” command.

For example, use the command “generate_landscape w=2000|h=1500|seed=5013|border=250” to generate a landscape image with dimensions 2000x1500, and a border of water 250 wide, using random seed 5013 (each random seed will produce a different landscape.) “Generate_ladscape_set” can be used to create several different images so that you can choose a seed you prefer from them. Load the image into a paint program such as Gimp and add pixels representing Orbs, along with making any modifications to the landscape or resource placement you would like. (It’s a good idea to make these changes on a higher layer, so that you always have the original you can go back to.) Choose a leftmost x position where the level 1 nation area will start, making sure there’s a good amount of land just to the east of it and none to the west. Export this modified image to a png file with a name such as “landgen_with_orbs.png”.

In the same way, you’ll need to make a number of small maps to use as home islands, or else use the included map_2.png through map_16.png.

Next, you’ll use the load_landscape command to load the landscape from the image file into the database. When you start a run of the server where you will be using the load_landscape or generate_map commands, start it this way:

xvfb-run ./server.sh

That enables xvfb’s virtual frame buffer, which is necessary for those commands.

To load the main landscape from a file named landgen_with_orbs.png, you would use this command:

load_landscape id=1|file=landgen_with_orbs.png|really=1

You then need to load the various home island landscapes, as follows:

load_landscape id=2|file=map_2.png
(Do this for map IDs 2 through 16, if using that many different home island images.)

Finally, you’ll need to generate the map image that is displayed on the client, using this command:

generate_map file=landgen_800_400_5000.png


In the server’s config.json file change the min_level_limit to the left-most x position on the map that you want level 1 players to be able to start at, as you determined in the previous step.
You can now run the new game server using screen (as described above), test it, and then let in players!
