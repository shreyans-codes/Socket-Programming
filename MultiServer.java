import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ConnectionHandler
 */

public class MultiServer implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private boolean done;
    private ServerSocket server;
    private int port;

    private ExecutorService pool;

    public MultiServer(int port) {
        this.connections = new ArrayList<>();
        this.done = false;
        this.port = port;
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
            shutdown();
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
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " connected!");
                broadcast(nickname + " has entered the chat");
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.startsWith("/nick")) {
                        // Todo: handle custom message sending
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
