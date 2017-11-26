package utils;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import database.AbstractData;
import database.DatabaseHelper;
import database.GroupData;
import database.RecordData;

public class Utils {
    public interface AbstractDataSource {
        int size();

        AbstractData get(int pos);
    }

    public static int getPositionById(AbstractDataSource list, long id) {
        for (int pos = 0; pos < list.size(); ++pos) {
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

    public static class DateTimeFormatted {
        public final String date;
        public final String time;

        public DateTimeFormatted(String date, String time) {
            this.date = date;
            this.time = time;
        }

        public String getDateTime() {
            return date + " " + time;
        }
    }

    public static DateTimeFormatted formatDateTime(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("E, MMM d, yyyy");
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
        return new DateTimeFormatted(df.format(date), tf.format(date));
    }
}
