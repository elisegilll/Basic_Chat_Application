# Basic_Chat_Application

Project Overview: 
This project is a basic chat application implemented in java. It contains two major components: a server and a client. The server handles client connections, message broadcasting, file uploads and downloads and serves a statistics webpage. 
The client connects to the server, allows users to send and recieve messages and upload and download files. 

Installation Instructions: 
What you'll need:
- Java Development Kit (8 or higher)
- Terminal 

Steps to run: 
1. download and extract the project.
2. compile the source code 
open terminal, navigate to project directory (cd src), run the following: Javac Server.java,  and javac Client.java to compile the source files

3. Run the server 
navigate to project directory
cd src 
java Server.java 
it will start running on default port 8080 

4. Run the client 
navigate to project directory
cd src 
java Client.java 

client may choose the server, port it wants to connect to and a username 

5. Available commands: 
Send message - simply type and select enter and server will broadcast it to connected clients 
Upload File: /upload [file_path] server saves files to directory (uploads)
Download File: /download [file_name] server downloads file to directory (downloads)
Exit: type /exit 

