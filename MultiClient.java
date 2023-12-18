import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MultiClient implements Runnable {
    private String ip_address;
    private int port;

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private Boolean done;

    public MultiClient(String ip_address, int port) {
        this.ip_address = ip_address;
        this.port = port;
        this.done = false;
    }

    @Override
    public void run() {
        try {
            client = new Socket(ip_address, port);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            InputHandler inputHandler = new InputHandler();
            Thread t = new Thread(inputHandler);
            t.start();
            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // TODO: ignore
        }
    }

    public static void main(String[] args) {
        MultiClient client = new MultiClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

    /**
     * InputHandler
     */
    class InputHandler implements Runnable {

        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equalsIgnoreCase("/quit")) {
                        inReader.close();
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

    }

}
