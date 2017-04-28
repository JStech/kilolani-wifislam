package net.stechschulte.kilolani;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;
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
    private static final Random prng = new Random();

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(tcp_pt);
            while (!myThread.isInterrupted()) {
                client = serverSocket.accept();
                String peer_addr = client.getInetAddress().getHostAddress().toString();
                Log.v(TAG, String.format("Connection from %s", peer_addr));
                ManageSharedPrefs.getInstance().addPeer(peer_addr);

                outgoing = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        client.getOutputStream())), true);
                BufferedReader incoming = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));

                String message = null;
                try {
                    message = incoming.readLine();
                    JSONObject req = new JSONObject(message);
                    outgoing.write(handleRequest(req).toString()+"\n");
                } catch (IOException ioe) {
                    Log.e(TAG, ioe.getLocalizedMessage());
                } catch (JSONException jse) {
                    Log.e(TAG, jse.getLocalizedMessage());
                } catch (NullPointerException npe) {
                    Log.e(TAG, "Bad request? "+npe.getLocalizedMessage());
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
            if (serverSocket != null) {serverSocket.close();}
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        if (myThread != null) {
            myThread.interrupt();
        }
    }

    private JSONObject handleRequest(JSONObject req) throws JSONException {
        Log.v(TAG, "Handling request");
        JSONObject ret = null;
        String request = req.getString("req");
        switch (request) {
            case "FindPeers":
                int n = req.getInt("n");
                List<String> peers =
                        new ArrayList<String>(ManageSharedPrefs.getInstance().getPeers());
                n = min(n, peers.size());
                Set<Integer> sample = new HashSet<>();
                while (sample.size() < n) {
                    sample.add(prng.nextInt(peers.size()));
                }
                Integer[] s = sample.toArray(new Integer[sample.size()]);
                JSONArray ret_peers = new JSONArray();
                for (int i: sample) {
                    ret_peers.put(peers.get(s[i]));
                }
                ret = new JSONObject();
                ret.put("result", ret_peers);
                break;
            case "SharePositions":
                break;
            case "RequestPositions":
                break;
        }
        return ret;
    }
}
