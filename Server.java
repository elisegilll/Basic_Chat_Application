import java.io.*; //* for the java Input Output package, reading and write to files and many I/O functions */
import java.net.*; //* networking package which allows functions like sockets, network interface, and URLs */
import java.time.LocalDateTime; //* for using arraylist */
import java.time.format.DateTimeFormatter; //* for using collections to synchronise the list */
import java.util.ArrayList; //* for using list interface */
import java.util.Collections; //* */
import java.util.List; //* for the current date and time */
import java.util.concurrent.Executors; //* for formatting the date and time */

    
public class Server {

    //* default port for the server */
    private static int PORT = 8080;

    //* synchronized list to store PrintWriter objects for each connected client */
    private static final List<PrintWriter> outputs = Collections.synchronizedList(new ArrayList<>());

    //* FileWriter to log messages from clients */
    private static FileWriter logFile;

    //* synchronized list to store messages */
    private static final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        //* allows the user to specify port from the command line and uses default if nothing is provided */
        if (args.length > 0) {
            try {
                PORT = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default port: " + PORT);
            }
        }

        //* tries to open the log file in append mode */
        try {
            logFile = new FileWriter("chat.log", true);
        } catch (IOException e) {
            System.out.println("Could not open log file for writing. Continuing without logging.");
        }

        //* ensure the uploads directory exists */
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        //* starts the HTTP server for stats page on a separate thread */
        Executors.newSingleThreadExecutor().submit(Server::startHttpServer);

        //* try-with-resources statement to ensure the server socket is closed automatically */
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            //* continuously listens for a new client connection */
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                handleClientConnection(clientSocket);
            }
        }
    }

    private static void startHttpServer() {
        try (ServerSocket httpServerSocket = new ServerSocket(8081)) {
            while (true) {
                try (Socket client = httpServerSocket.accept()) {
                    handleStatsRequest(client);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleStatsRequest(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        while (!(in.readLine()).isBlank()) { }
        String response = generateStatsPage();
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: " + response.length());
        out.println();
        out.println(response);
    }

    private static void handleClientConnection(Socket clientSocket) {
        new Thread(() -> {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                outputs.add(out);
                String inputLine;

                //* continuously reads messages from the client */
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("/upload ")) {
                        handleFileUpload(clientSocket, inputLine);
                    } else if (inputLine.startsWith("/download ")) {
                        handleFileDownload(clientSocket, inputLine);
                    } else {
                        //* logs the received message to the log file and broadcasts it */
                        logAndBroadcastMessage(inputLine, out);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                //* clean up and close resources */
                cleanupResources(clientSocket);
            }
        }).start();
    }

    private static void logAndBroadcastMessage(String message, PrintWriter sender) {
        synchronized (logFile) {
            try {
                logFile.write(message + "\n");
                logFile.flush();
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
        synchronized (outputs) {
            for (PrintWriter writer : outputs) {
                if (writer != sender) {
                    writer.println(message);
                    writer.flush();
                }
            }
        }
    }

    private static void handleFileUpload(Socket clientSocket, String inputLine) {
        String[] tokens = inputLine.split(" ");
        String fileName = tokens[1];
        long fileSize = Long.parseLong(tokens[2]);
        File file = new File("uploads", fileName);
        try (BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
             FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] buffer = new byte[4096];
            long remaining = fileSize;
            while (remaining > 0) {
                int bytesRead = bis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead == -1) break;
                bos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            bos.flush();
            System.out.println("File uploaded: " + fileName);
        } catch (IOException e) {
            System.err.println("File Uploaded Successfully: " + e.getMessage());
        }
    }


    private static void handleFileDownload(Socket clientSocket, String inputLine) {
        String[] tokens = inputLine.split(" ");
        String fileName = tokens[1].substring(tokens[1].lastIndexOf('/') + 1); //* Ensure just the filename */
        File file = new File("uploads", fileName);
        if (file.exists()) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                 OutputStream os = clientSocket.getOutputStream()) {
                PrintWriter out = new PrintWriter(os, true);
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/octet-stream");
                out.println("Content-Length: " + file.length());
                out.println("Content-Disposition: attachment; filename=\"" + fileName + "\"");
                out.println();
                out.flush();  //*  Ensure header is sent before file data */
    
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();  // Ensure all data is sent
            } catch (IOException e) {
                System.err.println("Error handling file download: " + e.getMessage());
            }
        } else {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                out.println("HTTP/1.1 404 Not Found");
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending 404 message: " + e.getMessage());
            }
        }
    }

    private static void cleanupResources(Socket clientSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Failed to close client socket: " + e.getMessage());
        }
    }

    private static String generateStatsPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Chat Statistics</title></head><body>");
        sb.append("<h1>Chat Statistics</h1>");
        sb.append("<p>Current date and time: ").append(getCurrentDateTime()).append("</p>");
        sb.append("<p>Number of connected clients: ").append(outputs.size()).append("</p>");
        sb.append("<h2>Messages</h2><ul>");
        synchronized (messages) {
            for (String message : messages) {
                sb.append("<li>").append(message).append("</li>");
            }
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    private static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}