package net.stechschulte.kilolani;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * Created by john on 4/28/17.
 */

class ManageSharedPrefs {
    private static ManageSharedPrefs instance;
    private static SharedPreferences preferences = null;
    private static final String PREF_PEERS = "peers";

    private ManageSharedPrefs() {}

    static {
        instance = new ManageSharedPrefs();
    }

    static ManageSharedPrefs getInstance() {
        return instance;
    }

    static void Initialize(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    Set<String> getPeers() {
        return preferences.getStringSet(PREF_PEERS, null);
    }

    void addPeer(String peer_to_add) {
        try {
            synchronized (this) {
                Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
                peers.add(peer_to_add);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(PREF_PEERS, peers);
                editor.commit();
            }
        } catch (NullPointerException e) {}
    }

    void addPeers(Set<String> peers_to_add) {
        try {
            synchronized (this) {
                Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
                peers.addAll(peers_to_add);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(PREF_PEERS, peers);
                editor.commit();
            }
        } catch (NullPointerException e) {}
    }

    void delPeers(Set<String> peers_to_del) {
        try {
            synchronized (this) {
                Set<String> peers = preferences.getStringSet(PREF_PEERS, null);
                peers.removeAll(peers_to_del);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(PREF_PEERS, peers);
                editor.commit();
            }
        } catch (NullPointerException e) {}
    }
}
