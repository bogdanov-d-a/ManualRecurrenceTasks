package utils;

import android.content.Context;

import java.util.ArrayList;

import database.AbstractData;
import database.DatabaseHelper;
import database.RecordData;
import database.GroupData;

public class Utils {
    public interface AbstractDataSource {
        int size();
        AbstractData get(int pos);
    }

    public static int getPositionById(AbstractDataSource list, long id) {
        for (int pos = 0; pos < list.size(); ++pos)
        {
            if (list.get(pos).id == id)
                return pos;
        }
        return -1;
    }

    public static int getPositionById(final ArrayList<GroupData> groups, long id) {
        return Utils.getPositionById(
                new Utils.AbstractDataSource() {
                    @Override
                    public int size() {
                        return groups.size();
                    }
                    @Override
                    public AbstractData get(int pos) {
                        return groups.get(pos);
                    }
                },
                id
        );
    }

    public static boolean recordCanBeChecked(final ArrayList<GroupData> groups, long recordGroupId) {
        return groups.get(getPositionById(groups, recordGroupId)).isChecklist;
    }

    public static boolean groupHasCheckedRecords(Context context, long groupId) {
        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(groupId, Long.MIN_VALUE, false);

        for (RecordData record : records) {
            if (record.isChecked)
                return true;
        }

        return false;
    }

    public static boolean groupHasRecords(Context context, long groupId) {
        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(groupId, Long.MIN_VALUE, false);
        return !records.isEmpty();
    }
}
