package net.stechschulte.kilolani;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import static net.stechschulte.kilolani.Constants.PREF_PEERS;
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
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FIND_PEERS = "net.stechschulte.kilolani.action.FIND_PEERS";
    private static final String ACTION_BAZ = "net.stechschulte.kilolani.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_N = "net.stechschulte.kilolani.extra.N";
    private static final String EXTRA_PARAM1 = "net.stechschulte.kilolani.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "net.stechschulte.kilolani.extra.PARAM2";

    public TcpClient() {
        super("TcpClient");
    }

    /**
     * Starts this service to perform action FindPeers with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFindPeers(Context context, int n) {
        Intent intent = new Intent(context, TcpClient.class);
        intent.setAction(ACTION_FIND_PEERS);
        intent.putExtra(EXTRA_N, n);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, TcpClient.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FIND_PEERS.equals(action)) {
                final int n = intent.getIntExtra(EXTRA_N, 3);
                handleActionFindPeers(n);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    /**
     * Handle action FindPeers in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFindPeers(int n) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> peers = prefs.getStringSet(PREF_PEERS, null);
        if (peers==null) {
            Log.e(TAG, "I am lost and alone in the world.");
            return;
        }
        // construct JSON request
        String json = "";
        try {
            json = new JSONObject().put("req", "FindPeers").put("n", n).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON (this shouldn't happen)");
        }

        for (String peer : peers) {
            try {
                InetAddress addr = InetAddress.getByName(peer);
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(addr, tcp_pt), 500);
                PrintWriter outgoing = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader incoming = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                outgoing.write(json);
                outgoing.flush();

                String reply = incoming.readLine();
                Log.v(TAG, "Received: "+reply);
                incoming.close();
                outgoing.close();
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
