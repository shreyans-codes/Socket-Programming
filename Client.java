import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client
 */
public class Client {

    public static void main(String[] args) {
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            socket = new Socket(args[0], Integer.parseInt(args[1]));
            /*
             * Enter this line for debugging
             * socket = new Socket(localhost, 1234);
             */
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

            if (socket.isConnected())
                System.out.println("Connected to the server!");

            bufferedReader = new BufferedReader(inputStreamReader);
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                String messageToSend = scanner.nextLine();
                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                // * This is not used
                if (socket.isClosed()) {
                    System.out.println("Connection closed!\nExiting...");
                    break;
                }

                System.out.println("Server: " + bufferedReader.readLine());
                if (messageToSend.equalsIgnoreCase("bye"))
                    break;
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println("Connection Closed!\nExiting");
            return;
        } finally {
            try {
                if (socket != null)
                    socket.close();
                if (inputStreamReader != null)
                    inputStreamReader.close();
                if (outputStreamWriter != null)
                    outputStreamWriter.close();
                if (bufferedReader != null)
                    bufferedReader.close();
                if (bufferedWriter != null)
                    bufferedWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}