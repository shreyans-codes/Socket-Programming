import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiServer implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private HashMap<String, ConnectionHandler> connectedClients;
    private boolean done;
    private ServerSocket server;
    private int port;
    private String passKey = "shreyans-chatmigo";

    private ExecutorService pool;

    public MultiServer(int port) {
        this.connections = new ArrayList<>();
        this.done = false;
        this.port = port;
        this.connectedClients = new HashMap<>();
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            shutdown();
        }
    }

    public boolean isUserPresent(String username) {
        return connectedClients.containsKey(username.toLowerCase());
    }

    public void sendCustomMessage(String senderName, String recieverName, String message) {
        for (ConnectionHandler ch : connections) {
            if (ch.nickname.equals(recieverName)) {
                ch.sendMessage(senderName + ": (To you) " + message);
                break;
            }
        }
    }

    public void shutdown() {
        this.done = true;
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
        if (!server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        MultiServer server = new MultiServer(Integer.parseInt(args[0]));
        server.run();
    }

    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String customPrivateKey;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                boolean temp = true;
                Random rand = new Random();
                Integer rn = rand.nextInt(10000);
                customPrivateKey = passKey + rn.toString();
                String keyShareMessage = new String(Base64.getEncoder().encode(("/rn " + rn.toString()).getBytes()),
                        "UTF-8");
                sendMessage(keyShareMessage);
                System.out.println(new String(Base64.getDecoder().decode(keyShareMessage)));
                while (temp) {
                    out.println("Please enter a nickname: ");
                    nickname = in.readLine();
                    if (!Character.isLetter(nickname.charAt(0)) || nickname.length() < 4) {
                        out.println("Nickname should start with a letter and should be at least 4 characters");
                        continue;
                    }
                    if (isUserPresent(nickname)) {
                        out.println("Nickname already exists!");
                        continue;
                    }
                    temp = false;
                    break;
                }
                System.out.println(nickname + " connected!");
                broadcast(nickname + " has entered the chat");
                connectedClients.put(nickname, this);
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.startsWith("/")) {
                        String command[] = clientMessage.split(" ");
                        switch (command[0]) {
                            case "/nick":
                                String recieverName = command[1];
                                String privateMessageList[] = Arrays.copyOfRange(command, 2, command.length);
                                String privateMessage = String.join(" ", privateMessageList);
                                sendCustomMessage(nickname, recieverName, privateMessage);
                                break;
                            case "/users":
                                for (String currentName : connectedClients.keySet()) {
                                    out.println(currentName);
                                }
                                break;
                            case "/help":
                                printHelp();
                                break;
                            case "/listfiles":
                                listfiles(true);
                                break;
                            case "/write":
                                String fileName = command[1];
                                String additionToFile[] = Arrays.copyOfRange(command, 2, command.length);
                                String fileContent = String.join(" ", additionToFile);
                                writeToFile(fileName,
                                        nickname + ": " + fileContent);
                                break;
                            case "/quit":
                                broadcast(nickname + " has left the chat");
                                shutdown();
                                break;
                            default:
                                out.println("Please check your command. Try /help for list of accepted commands");
                                break;
                        }
                    } else {
                        broadcast(nickname + ": " + clientMessage);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void printHelp() {
            out.println("Welcome to Chatmigo!\nList of commands:");
            out.println(
                    "/nick: Send message to a particular user (e.g /nick shreyans Hello)\n" +
                            "/users: List of users connected\n"
                            + "/help: Display list of commands\n"
                            + "/listfiles: Return the list of files in the current directory\n"
                            + "/write: Write content to a file (e.g. /write filename.txt content to write)\n"
                            + "/quit: Terminate your connection to the system\n");
        }

        public ArrayList<String> listfiles(boolean shouldPrint) {
            // Retrieve the current working directory
            String currentDirectory = System.getProperty("user.dir");

            // Create a File object for the current directory
            File directory = new File(currentDirectory);

            // Get a list of files in the directory
            File[] files = directory.listFiles();
            ArrayList<String> fileNames = new ArrayList<>();

            // Check if files exist and print their names
            if (files != null) {
                if (shouldPrint) {
                    out.println("Files in the current directory:");
                }
                for (File file : files) {
                    if (file.isFile()) {
                        if (shouldPrint)
                            out.println(file.getName());
                        fileNames.add(file.getName());
                    }
                }
            } else {
                out.println("No files found in the current directory.");
            }
            return fileNames;
        }

        public void writeToFile(String fileName, String message) {

            try {
                Path fileNameLocal = Paths.get(fileName);
                ArrayList<String> filesInDirectory = listfiles(false);
                if (!filesInDirectory.contains(fileName)) {
                    out.println("File does not exist in current directory: creation of files not allowed");
                    return;
                }
                if (!Files.exists(fileNameLocal)) {
                    out.println("File does not exist: creation of files not allowed");
                    return;
                }
                if (!Files.isWritable(fileNameLocal)) {
                    out.println("Cannot write to file: not allowed!");
                    return;
                }
                FileOutputStream fStream = new FileOutputStream(fileName, true);
                byte[] strToBytes = message.getBytes(StandardCharsets.UTF_8);
                File file = new File(fileName);
                if (file.length() != 0)
                    fStream.write("\n".getBytes());
                fStream.write(strToBytes);
                fStream.close();
                out.println("Content appended to the file");
            } catch (Exception e) {
                System.out.println(
                        "An error occured while writing to file " + fileName + "\nCommand excecuted by " + nickname);
                out.println("An error occured. Could not write to file");
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            out.close();
            connectedClients.remove(nickname);
            try {
                in.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
