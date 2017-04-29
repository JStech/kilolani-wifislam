package net.stechschulte.kilolani;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import static net.stechschulte.kilolani.Constants.meters_per_deg_lat;
import static net.stechschulte.kilolani.Constants.meters_per_deg_lon;

/**
 * Created by john on 4/24/17.
 */

public class MapDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MapDatabaseHelper";

    // Database
    private static final String DATABASE_NAME = "KilolaniMap";
    private static final int DATABASE_VERSION = 2;

    // Tables
    private static final String TABLE_POSITIONS = "Positions";
    private static final String TABLE_WIFI_APS = "WifiAPs";
    private static final String TABLE_WIFI_OBS = "WifiObs";

    // Columns: shared
    private static final String COL_ID = "id";

    // Columns: positions table
    private static final String COL_LAT = "lat";
    private static final String COL_LON = "lon";
    private static final String COL_ACC = "acc";
    private static final String COL_TIME = "time";

    // Indices: positions table
    private static final String IDX_LAT = "lat_idx";
    private static final String IDX_LON = "lon_idx";

    // Columns: Wifi APs
    private static final String COL_BSSID = "bssid";

    // Columns: Wifi Obs
    private static final String COL_POS_ID = "pos_id";
    private static final String COL_AP_ID = "ap_id";
    private static final String COL_RSSI = "rssi";

    // SQL: create tables
    private static final String SQL_CREATE_TABLE_POS = "CREATE TABLE IF NOT EXISTS " +
            TABLE_POSITIONS + "(" + COL_ID + " INTEGER PRIMARY KEY," + COL_LAT + " DOUBLE," +
            COL_LON + " DOUBLE," + COL_ACC + " FLOAT," + COL_TIME + " INTEGER)";
    private static final String SQL_CREATE_TABLE_WIFI_APS = "CREATE TABLE IF NOT EXISTS " +
            TABLE_WIFI_APS + "(" + COL_ID + " INTEGER PRIMARY KEY," + COL_BSSID +
            " TEXT UNIQUE ON CONFLICT IGNORE)";
    private static final String SQL_CREATE_TABLE_WIFI_OBS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_WIFI_OBS + "(" +
            COL_POS_ID + " INTEGER REFERENCES " + TABLE_POSITIONS + "(" + COL_ID + ")," +
            COL_AP_ID + " INTEGER REFERENCES " + TABLE_WIFI_APS + "(" + COL_ID + ")," +
            COL_RSSI + " INTEGER)";

    // SQL: create indices
    private static final String SQL_CREATE_INDEX_LAT = "CREATE INDEX IF NOT EXISTS " + IDX_LAT +
            " ON " + TABLE_POSITIONS +"(" + COL_LAT + ")";
    private static final String SQL_CREATE_INDEX_LON = "CREATE INDEX IF NOT EXISTS " + IDX_LON +
            " ON " + TABLE_POSITIONS +"(" + COL_LON + ")";

    MapDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_POS);
        db.execSQL(SQL_CREATE_TABLE_WIFI_APS);
        db.execSQL(SQL_CREATE_TABLE_WIFI_OBS);
        db.execSQL(SQL_CREATE_INDEX_LAT);
        db.execSQL(SQL_CREATE_INDEX_LON);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSITIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFI_APS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFI_OBS);

        onCreate(db);
    }

    // insert position with observations into DB
    public void insertPosition(Position position) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_LAT, position.getLatitude());
        values.put(COL_LON, position.getLongitude());
        values.put(COL_ACC, position.getAccuracy());
        values.put(COL_TIME, position.getTime());

        long position_id = db.insert(TABLE_POSITIONS, null, values);

        String last_bssid = "";
        long ap_id = -1;
        for (Map.Entry<String, Integer> entry : position.getWifiObservations().entrySet()) {
            String bssid = entry.getKey();
            int rssi = entry.getValue();

            if (!last_bssid.equals(bssid)) {
                values.clear();
                values.put(COL_BSSID, bssid);
                ap_id = db.insert(TABLE_WIFI_APS, null, values);
                // all this mess is necessary because insert-or-update doesn't work right
                if (ap_id < 0) {
                    Cursor c = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_WIFI_APS +
                            " WHERE " + COL_BSSID + "=?", new String[]{bssid});
                    try {
                        while (c.moveToNext()) {
                            ap_id = c.getLong(0);
                        }
                    } finally {
                        c.close();
                    }

                    if (ap_id < 0) {
                        Log.e(TAG, "DB reading error");
                    }
                }
            }
            values.clear();
            values.put(COL_POS_ID, position_id);
            values.put(COL_AP_ID, ap_id);
            values.put(COL_RSSI, rssi);
            db.insert(TABLE_WIFI_OBS, null, values);
        }
    }

    // find number of positions within radius of center
    public int countObservationsNear(Position center, float radius) {
        SQLiteDatabase db = this.getReadableDatabase();

        double lat_min = center.getLatitude() - radius/meters_per_deg_lat;
        double lat_max = center.getLatitude() + radius/meters_per_deg_lat;
        double lon_min = center.getLongitude() - radius/meters_per_deg_lon;
        double lon_max = center.getLongitude() + radius/meters_per_deg_lon;

        String sql_query = "SELECT COUNT(*) FROM " + TABLE_POSITIONS + " JOIN " + TABLE_WIFI_OBS +
                " ON " + TABLE_POSITIONS + "." + COL_ID + "=" + TABLE_WIFI_OBS + "." + COL_POS_ID +
                " JOIN " + TABLE_WIFI_APS +
                " ON " + TABLE_WIFI_APS + "." + COL_ID + "=" + TABLE_WIFI_OBS + "." + COL_AP_ID +
                " WHERE " +
                "(" + TABLE_POSITIONS + "." + COL_LAT + ">= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LAT + "<= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LON + ">= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LON + "<= ?" + ") " +
                "ORDER BY " + TABLE_POSITIONS + "." + COL_ID;

        Cursor c = db.rawQuery(sql_query, new String[]{Double.toString(lat_min), Double.toString(lat_max),
                Double.toString(lon_min), Double.toString(lon_max)});

        c.moveToFirst();
        int ret = c.getInt(0);
        c.close();

        return 0;
    }

    // get all observations within radius of center
    public ArrayList<Position> getObservationsNear(Position center, float radius) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Position> ret = new ArrayList<>();

        double lat_min = center.getLatitude() - radius/meters_per_deg_lat;
        double lat_max = center.getLatitude() + radius/meters_per_deg_lat;
        double lon_min = center.getLongitude() - radius/meters_per_deg_lon;
        double lon_max = center.getLongitude() + radius/meters_per_deg_lon;

        String sql_query = "SELECT * FROM " + TABLE_POSITIONS + " JOIN " + TABLE_WIFI_OBS +
                " ON " + TABLE_POSITIONS + "." + COL_ID + "=" + TABLE_WIFI_OBS + "." + COL_POS_ID +
                " JOIN " + TABLE_WIFI_APS +
                " ON " + TABLE_WIFI_APS + "." + COL_ID + "=" + TABLE_WIFI_OBS + "." + COL_AP_ID +
                " WHERE " +
                "(" + TABLE_POSITIONS + "." + COL_LAT + ">= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LAT + "<= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LON + ">= ?" + ") AND " +
                "(" + TABLE_POSITIONS + "." + COL_LON + "<= ?" + ") " +
                        "ORDER BY " + TABLE_POSITIONS + "." + COL_ID;

        Cursor c = db.rawQuery(sql_query, new String[]{Double.toString(lat_min), Double.toString(lat_max),
                Double.toString(lon_min), Double.toString(lon_max)});
        Position current_position = null;
        try {
            long current_pos_id = -1;
            int pos_id_col_idx = c.getColumnIndex(TABLE_POSITIONS + "." + COL_ID);
            int bssid_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            int lat_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            int lon_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            int acc_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            int time_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            int rssi_col_idx = c.getColumnIndex(TABLE_WIFI_APS + "." + COL_BSSID);
            while (c.moveToNext()) {
                if (current_pos_id != c.getLong(pos_id_col_idx)) {
                    current_pos_id = c.getLong(pos_id_col_idx);
                    if (current_position != null) {
                        ret.add(current_position);
                    }
                    current_position = new Position(
                            c.getDouble(lat_col_idx),
                            c.getDouble(lon_col_idx),
                            c.getFloat(acc_col_idx),
                            c.getLong(time_col_idx));
                }
                current_position.addObservation(c.getString(bssid_col_idx), c.getInt(rssi_col_idx));
            }
        } finally {
            if (current_position != null) {
                ret.add(current_position);
            }
            c.close();
        }

        return ret;
    }
}
