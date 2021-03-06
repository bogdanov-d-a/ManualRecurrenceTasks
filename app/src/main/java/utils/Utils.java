package utils;

import android.content.Context;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import database.AbstractData;
import database.GroupData;
import database.RecordData;
import notification.NotificationUtils;

public class Utils {
    public static final String YEAR_TAG = "YEAR_TAG";
    public static final String MONTH_TAG = "MONTH_TAG";
    public static final String DAY_TAG = "DAY_TAG";
    public static final String HOUR_TAG = "HOUR_TAG";
    public static final String MINUTE_TAG = "MINUTE_TAG";

    public static void putDateToBundle(int year, int monthOfYear, int dayOfMonth, Bundle bundle) {
        bundle.putInt(YEAR_TAG, year);
        bundle.putInt(MONTH_TAG, monthOfYear);
        bundle.putInt(DAY_TAG, dayOfMonth);
    }

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
        ArrayList<RecordData> records = ObjectCache.getDbInstance(context).getRecords(groupId, Long.MIN_VALUE, false);

        for (RecordData record : records) {
            if (record.isChecked)
                return true;
        }

        return false;
    }

    public static boolean groupHasRecords(Context context, long groupId) {
        ArrayList<RecordData> records = ObjectCache.getDbInstance(context).getRecords(groupId, Long.MIN_VALUE, false);
        return !records.isEmpty();
    }

    public static void deleteRecord(Context context, ArrayList<GroupData> groupsList, RecordData record) {
        final GroupData recordGroup = groupsList.get(Utils.getPositionById(groupsList, record.groupId));
        NotificationUtils.unregisterGroup(context, recordGroup);

        ObjectCache.getDbInstance(context).deleteRecord(record.id);
        NotificationUtils.unregisterRecord(context, recordGroup, record.id);

        NotificationUtils.registerGroup(context, recordGroup);
    }

    public static void copyFile(String sourcePath, String targetPath) throws Exception {
        FileInputStream sourceStream = new FileInputStream(new File(sourcePath));
        try {
            OutputStream targetStream = new FileOutputStream(targetPath);
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = sourceStream.read(buffer)) > 0) {
                    targetStream.write(buffer, 0, length);
                }
            } finally {
                targetStream.flush();
                targetStream.close();
            }
        } finally {
            sourceStream.close();
        }
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
