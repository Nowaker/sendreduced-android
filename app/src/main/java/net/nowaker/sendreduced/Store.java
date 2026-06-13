package net.nowaker.sendreduced;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Persistence for profiles and global settings, backed by SharedPreferences. */
public final class Store {
    private static final String PREFS = "sendreduced";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_INCLUDE_DIRECT = "includeDirect";

    private Store() {
    }

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Returns the stored profiles, seeding a single default profile on first run. */
    public static List<Profile> loadProfiles(Context c) {
        List<Profile> list = new ArrayList<>();
        String json = prefs(c).getString(KEY_PROFILES, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(Profile.fromJson(arr.getJSONObject(i)));
                }
            } catch (JSONException e) {
                list.clear();
            }
        }
        if (list.isEmpty()) {
            list.add(Profile.createDefault());
            saveProfiles(c, list);
        }
        return list;
    }

    public static void saveProfiles(Context c, List<Profile> profiles) {
        JSONArray arr = new JSONArray();
        try {
            for (Profile p : profiles) {
                arr.put(p.toJson());
            }
        } catch (JSONException e) {
            return;
        }
        prefs(c).edit().putString(KEY_PROFILES, arr.toString()).apply();
    }

    public static Profile profileById(Context c, String id) {
        for (Profile p : loadProfiles(c)) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static boolean isIncludeDirect(Context c) {
        return prefs(c).getBoolean(KEY_INCLUDE_DIRECT, true);
    }

    public static void setIncludeDirect(Context c, boolean value) {
        prefs(c).edit().putBoolean(KEY_INCLUDE_DIRECT, value).apply();
    }
}
