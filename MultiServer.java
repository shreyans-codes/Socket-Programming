import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiServer implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private boolean done;
    private ServerSocket server;
    private int port;
    private ArrayList<String> userNames;

    private ExecutorService pool;

    public MultiServer(int port) {
        this.connections = new ArrayList<>();
        this.done = false;
        this.port = port;
        this.userNames = new ArrayList<>();
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
                updateUserNames();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void updateUserNames() {
        for (ConnectionHandler ch : connections) {
            if (!userNames.contains(ch.nickname)) {
                userNames.add(ch.nickname);
            }
        }
    }

    public void addUserName(String newName) {
        userNames.add(newName);
    }

    public void removeUserName(String nameToRemove) {
        userNames.remove(nameToRemove);
    }

    public boolean isUserNamePresent(String nameToFind) {
        return userNames.contains(nameToFind);
    }

    public void sendCustomMessage(String senderName, String recieverName, String message) {
        for (ConnectionHandler ch : connections) {
            System.out.println("Current: " + ch.nickname.length());
            if (ch.nickname.equals(recieverName)) {
                System.out.println("Gupchup" + ch.nickname);
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

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                boolean temp = true;
                while (temp) {
                    out.println("Please enter a nickname: ");
                    nickname = in.readLine();
                    if (isUserNamePresent(nickname)) {
                        out.println("Nickname already exists!");
                        continue;
                    }
                    addUserName(nickname);
                    temp = false;
                    break;
                }
                System.out.println(nickname + " connected!");
                broadcast(nickname + " has entered the chat");
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.startsWith("/nick")) {
                        String messages[] = clientMessage.split(" ");
                        String recieverName = messages[1];
                        String privateMessage = messages[2];
                        sendCustomMessage(nickname, recieverName, privateMessage);
                    } else if (clientMessage.startsWith("/quit")) {
                        broadcast(nickname + " has left the chat");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + clientMessage);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            out.close();
            removeUserName(nickname);
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
