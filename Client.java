import java.io.*; //*  imports the java Input Output package, reading and write to files and many I/O functions */
import java.net.*; //*  imports networking package which allows functions like sockets, network interface, and URLs */
import java.util.Scanner;  //*  recognises and compiles any code that uses the Scanner class for input  */


public class Client {

    //* connect to the server  */
    private Socket socket = null;

    //* to read input from the user  */
    private BufferedReader input = null;

    //* send output to the server */
    private PrintWriter output = null;

    //* read input from the server */
    private BufferedReader serverInput = null;

    public Client(String address, int port, String username) {
        try {

            //* establishes connection to the server using the provided address and port */
            socket = new Socket(address, port);
            System.out.println("Connected to the server as " + username);


            //* initialises bufferedreader to read user input from the console */
            input = new BufferedReader(new InputStreamReader(System.in));

            //* initialises printwriter to send messages to the server */
            output = new PrintWriter(socket.getOutputStream(), true);

            //* initialises bufferedreader to read messages from the server */
            serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            //* creates and starts a new thread to handle incoming messages from the server */
            new Thread(() -> {
                try {
                    String fromServer;

                    //* continuously reads messages from the server and prints them to the console */
                    while ((fromServer = serverInput.readLine()) != null) {
                        System.out.println(fromServer);
                    }
                } catch (IOException e) {

                    //* prints a message if the connection to the server is lost */
                    System.out.println("Connection to server lost.");
                }
            }).start();

            //* continuously reads the user input and sends it to the server until the user types "/exit" */
            String line = "";
            while (!line.equals("/exit")) {
                //* trims the imput to hanndle accidental whitespace */
                line = input.readLine().trim();

                //* debugs output   */
                System.out.println(username + ": " + line);
                

                //* handles file upload if the user input starts with "/upload" */
                if (line.startsWith("/upload ")) {
                    uploadFile(line.substring(8).trim());

                    //* handles file downlad if the user input starts with "/download" */
                } else if (line.startsWith("/download ")) {
                    downloadFile(line.substring(10).trim());
                } else {

                    //* sends the message to the server as "username: message"  */
                    output.println(username + ": " + line);
                }
            }

            //* closes all the resources after user exits */
            closeEverything();
        } catch (UnknownHostException u) {

            //*handles the case where the server address is unknown */
            System.err.println("Unknown Host: " + u);
        } catch (IOException i) {

            //* handles general I/O errors */
            System.err.println("IO Error: " + i);
        }
    }

    private void uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {

            //* prints a message if the file is not found */
            System.out.println("File not found: " + filePath);
            return;
        }

        try {

            //* sends the upload command to the server with the file name and size */
            output.println("/upload " + file.getName() + " " + file.length());
            byte[] buffer = new byte[4096];

            //* reads the file and sends it to the server  */
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 OutputStream os = socket.getOutputStream()) {
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

        //* prints a message confirming the file has been uploaded */

            System.out.println("File uploaded: " + filePath);
        } catch (IOException e) {

            //* prints an error message if there is an issue with the file upload  */
            System.err.println("Error uploading file: " + e.getMessage());
        }
    }

    private void downloadFile(String fileName) {
        try {

        //* sends the download command to the server with the file name */
            output.println("/download " + fileName);
            InputStream is = socket.getInputStream();
            File file = new File("downloads/" + fileName);

            //* creates the directory if it doesn't exist */
            file.getParentFile().mkdirs(); 

            //* reads the file from the server and saves it to the local system */
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
            }

            //* prints a message confirming the file has been downloaded */
            System.out.println("File downloaded: " + fileName);
        } catch (IOException e) {

            //* prints an error message if there is an issue with the file download */
            System.err.println("Error downloading file: " + e.getMessage());
        }
    }

    //* method to close all I/O resources properly */
    private void closeEverything() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (serverInput != null) serverInput.close();
            if (socket != null) socket.close();

            //* prints a message confirming the user has disconnected from the server */
            System.out.println("Disconnected from the server.");
        } catch (IOException i) {

            //* handles I/O errors during the closing of resources */
            System.err.println("IO Error on closing: " + i);
        }
    }

    //* main method - entry point of the application */
    public static void main(String[] args) {

         //* uses a try-with-resources statement to ensure the scanner is closed automatically  */
        try (Scanner scanner = new Scanner(System.in)) {

            //* prompts the user to enter the server address with a default option */
            System.out.println("Enter server address (default: localhost): ");
            String inputLine = scanner.nextLine();
            String address = inputLine.isEmpty() ? "localhost" : inputLine;

            //* prompts the user to enter the server port with a default option */
            System.out.println("Enter server port (default: 8080): ");
            inputLine = scanner.nextLine();
            int port = inputLine.isEmpty() ? 8080 : Integer.parseInt(inputLine);

            //* prompts the user to enter their username with a default option */
            System.out.println("Enter your username (default: unknown user): ");
            inputLine = scanner.nextLine();
            String username = inputLine.isEmpty() ? "unknown user" : inputLine;


            //* creates a new client instance with the provided address, port and username */
            new Client(address, port, username);
        }
    }
}
