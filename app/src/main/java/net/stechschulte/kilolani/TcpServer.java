package net.stechschulte.kilolani;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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
    private ServerSocket serverSocket;
    private Socket client;
    private PrintWriter outgoing;
    private Thread myThread = null;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(tcp_pt);
            Log.v(TAG, String.format("Server bound at %s",
                    serverSocket.getLocalSocketAddress().toString()));
            while (!myThread.isInterrupted()) {
                Log.v(TAG, "Here 1");
                client = serverSocket.accept();
                Log.v(TAG, "Here 2");
                outgoing = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        client.getOutputStream())), true);
                BufferedReader incoming = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));
                Log.v(TAG, "Here 3");

                String message = null;
                try {
                    message = incoming.readLine();
                    // TODO: process request
                    outgoing.write("You sent: " + message + "\n");
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                Log.v(TAG, "Received: "+message);

                outgoing.flush();
                outgoing.close();
                client.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        if (outgoing != null) {
            outgoing.flush();
            outgoing.close();
            outgoing = null;
        }

        try {
            if (client != null) {client.close();}
            if (serverSocket != null) {serverSocket.close();}
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        Log.v(TAG, "Server done.");
        serverSocket = null;
        client = null;
    }

    public void start() {
        if (myThread == null) {
            myThread = new Thread(this);
            myThread.start();
        }
    }

    public void stop() {
        Log.v(TAG, "Interrupting server");
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        if (myThread != null) {
            myThread.interrupt();
        }
    }
}
