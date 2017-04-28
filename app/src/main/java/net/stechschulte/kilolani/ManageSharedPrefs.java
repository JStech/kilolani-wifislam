package net.stechschulte.kilolani;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * Created by john on 4/28/17.
 */

public class ManageSharedPrefs {
    private static ManageSharedPrefs instance;
    private static SharedPreferences preferences = null;
    public static final String PREF_PEERS = "peers";

    private ManageSharedPrefs() {}

    static {
        instance = new ManageSharedPrefs();
    }

    public static ManageSharedPrefs getInstance() {
        return instance;
    }

    public static void Initialize(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Set<String> getPeers() {
        Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
        return peers;
    }

    public void addPeers(Set<String> peers_to_add) {
        synchronized (preferences) {
            Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
            peers.addAll(peers_to_add);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(PREF_PEERS, peers);
            editor.commit();
        }
    }

    public void delPeers(Set<String> peers_to_del) {
        synchronized (preferences) {
            Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
            peers.removeAll(peers_to_del);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(PREF_PEERS, peers);
            editor.commit();
        }
    }
}
