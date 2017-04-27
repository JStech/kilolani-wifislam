package net.stechschulte.kilolani;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static net.stechschulte.kilolani.Constants.tcp_pt;

/**
 * Created by john on 4/27/17.
 */

public class TcpServer implements Runnable {
    final static String TAG="TcpServer";
    private boolean running = false;
    private ServerSocket serverSocket;
    private Socket client;
    private PrintWriter outgoing;

    @Override
    public void run() {
        running = true;
        try {
            serverSocket = new ServerSocket(tcp_pt);
            Log.v(TAG, String.format("Server bound at %s",
                    serverSocket.getLocalSocketAddress().toString()));
            while (running) {
                client = serverSocket.accept();
                outgoing = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        client.getOutputStream())), true);
                BufferedReader incoming = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));

                String message = null;
                try {
                    message = incoming.readLine();
                    outgoing.write("You sent: "+message);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                Log.v(TAG, message);

                outgoing.flush();
                outgoing.close();
                client.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public void stop() {
        running = false;
        if (outgoing != null) {
            outgoing.flush();
            outgoing.close();
            outgoing = null;
        }

        try {
            client.close();
            serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        Log.v(TAG, "Server done.");
        serverSocket = null;
        client = null;
    }
}
