package utils;

import android.content.Context;

import java.util.ArrayList;

import database.DatabaseHelper;
import database.GroupData;

public class ObjectCache {
    private static DatabaseHelper dbInstance = null;
    private static ArrayList<GroupData> groups = null;

    public static DatabaseHelper getDbInstance(Context context) {
        if (dbInstance == null) {
            dbInstance = new DatabaseHelper(context);
        }
        return dbInstance;
    }

    public static ArrayList<GroupData> getGroups(Context context) {
        if (groups == null) {
            groups = getDbInstance(context).getGroups();
        }
        return groups;
    }

    public static void invalidateCachedGroups() {
        groups = null;
    }
}
