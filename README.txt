-----------------------------------------------------------------------------------------------------------------------
HOW TO RUN:
-------------------------------------------
USING PYTHON SCRIPT:
---------------------
You can download the code onto any CSE labs machine and run the python script supplied:
	To run server processes: 
		python3 run.py SRV <numClients>

	To run client processes:
		python3 run.py CLNT <client-id> <num-threads>

This script will compile required java programs and start rmiregistry on the port specified by config file.
It will also run all the server processes which are needed on that machine according to 'configFile.txt'.

WITHOUT USING PYTHON SCRIPT:
----------------------------
- Change directory into the ServerReplication folder

- To compile server and client programs, do:
		javac RMIBankServerImpl.java
		javac RMIClient.java

- To start rmiregistry do:
		rmiregistry <port-number> &
	Make sure port number is correct according to the config file.

- To start server process:
		java RMIBankServerImpl <server-id> configFile.txt <numClients>

- Wait until all server connections are made. All server should display message saying they are ready for client messages.

- To start client process:
		java RMIClient <client-id> <num-threads> configFile.txt

-----------------------------------------------------------------------------------------------------------------------
LOG FILES
------------
- All log files are stored in the sub-directory ./logs

- Server log files are named according to their id. 
	For example: Server with id=0 has log file named 'Server-0.log'

- Similarly client log files are named as: 'Client-<id>.log'
	For example: 'Client-0.log'
-----------------------------------------------------------------------------------------------------------------------
KNOWN BUGS:
------------------
- The python script does not exit even after all the java processes have exited. 
	Just do a Ctrl-C to exit after all processes are done

- However, all Java processes exit gracefully.

- Before running client process, we have to wait for server processes to complete connection forming phase.
  Wait for a message from all server processes, which says:
	"---- Server Initialization complete. Ready for client messages. ----"

-----------------------------------------------------------------------------------------------------------------------
Results of Performance measurement experiment:
-----------------------------------------------
Server machines: csel-kh1260-01 to csel-kh1260-05
Client machine: csel-kh1260-20

5 servers, 1 client : 
----------------------
Server avg.: 0.1486s 0.1424s 0.1735s 0.1434s 0.1502s | Overall server avg.: 0.1516s
Client avg. time: 0.1528s

3 servers, 1 client:
---------------------
Server avg.: 0.1472s 0.1490s 0.1690s | Overall server avg.: 0.1550s
Client avg.: 0.1562s

1 servers, 1 client:
---------------------
Server avg.: 0.0217s 
Client avg.: 0.0224s

