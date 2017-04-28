package net.stechschulte.kilolani;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.stechschulte.kilolani.Constants.tcp_pt;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class TcpClient extends IntentService {
    public static final String TAG = "TcpClient";
    private static final String ACTION_REQUEST = "net.stechschulte.kilolani.action.REQUEST";
    private static final String EXTRA_REQUEST = "net.stechschulte.kilolani.extra.REQUEST";

    public TcpClient() {
        super("TcpClient");
    }

    public static void startActionFindPeers(Context context, int n) {
        try {
            JSONObject request = new JSONObject().put("req", "FindPeers").put("n", n);
            Intent intent = new Intent(context, TcpClient.class);
            intent.setAction(ACTION_REQUEST);
            intent.putExtra(EXTRA_REQUEST, request.toString());
            context.startService(intent);
        } catch (JSONException jse) {
            Log.e(TAG, jse.getLocalizedMessage());
        }
    }

    public static void startActionRequestPositions(Context context, double lat, double lon, float radius) {
        try {
            Intent intent = new Intent(context, TcpClient.class);
            intent.setAction(ACTION_REQUEST);
            JSONObject request = new JSONObject();
            request.put("req", "RequestPositions");
            request.put("lat", lat);
            request.put("lon", lon);
            request.put("radius", radius);
            intent.putExtra(EXTRA_REQUEST, request.toString());
            Log.v(TAG, request.toString());
            context.startService(intent);
        } catch (JSONException jse) {
            Log.e(TAG, jse.getLocalizedMessage());
        }
    }

    public static void startActionSharePositions(Context context, List<Position> positions) {
        Log.v(TAG, String.format("sharing %d positions", positions.size()));
        try {
            JSONObject request = new JSONObject();
            request.put("req", "SharePositions");
            JSONArray ja_positions = new JSONArray();
            for (Position p: positions) {
                JSONObject jo_p = new JSONObject();
                jo_p.put("lat", p.getLatitude());
                jo_p.put("lon", p.getLongitude());
                jo_p.put("acc", p.getAccuracy());
                jo_p.put("time", p.getTime());
                JSONObject jo_s = new JSONObject();
                for (Map.Entry<String, Integer> obs : p.getWifiObservations().entrySet()) {
                    jo_s.put(obs.getKey(), obs.getValue());
                }
                jo_p.put("signals", jo_s);
                ja_positions.put(jo_p);
            }
            request.put("positions", ja_positions);

            Intent intent = new Intent(context, TcpClient.class);
            intent.setAction(ACTION_REQUEST);
            intent.putExtra(EXTRA_REQUEST, request.toString());
            Log.v(TAG, request.toString());
            context.startService(intent);
        } catch (JSONException jse) {
            Log.e(TAG, jse.getLocalizedMessage());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String request = intent.getStringExtra(EXTRA_REQUEST);
            Set<String> peers = ManageSharedPrefs.getInstance().getPeers();

            for (String peer : peers) {
                try {
                    String result = executeRequest(peer, request);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }
    }

    private String executeRequest(String address, String request) {
        try {
            InetAddress addr = InetAddress.getByName(address);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(addr, tcp_pt), 500);
            PrintWriter outgoing = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            BufferedReader incoming = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            outgoing.write(request);
            outgoing.flush();

            String reply = incoming.readLine();
            Log.v(TAG, "Received: "+reply);
            incoming.close();
            outgoing.close();
            socket.close();
            return reply;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Handle action FindPeers in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFindPeers(int n) {
        ManageSharedPrefs sharedPrefs = ManageSharedPrefs.getInstance();
        Set<String> peers = sharedPrefs.getPeers();

        // construct JSON request
        JSONObject req;
        try {
            req = new JSONObject().put("req", "FindPeers").put("n", n);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON (this shouldn't happen)");
            return;
        }

        Set<String> peers_to_add = new HashSet<>();
        Set<String> peers_to_del = new HashSet<>();

        for (String peer : peers) {
            try {
                JSONArray new_peers = new JSONObject(executeRequest(peer, req.toString()))
                        .getJSONArray("result");
                for (int i=0; i<new_peers.length(); i++) {
                    String new_peer = new_peers.getString(i);
                    peers_to_add.add(new_peer);
                }
            } catch (JSONException | NullPointerException e) {
                peers_to_del.add(peer);
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        sharedPrefs.addPeers(peers_to_add);
        sharedPrefs.delPeers(peers_to_del);
    }

    private List<Position> handleActionRequest(String request) {
        return null;
    }
}
