package notification;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.Calendar;

import database.RecordData;
import database.TagData;
import database.DatabaseHelper;
import ru.trjoxuvw.manualrecurrencetasks.AddRecordActivity;
import ru.trjoxuvw.manualrecurrencetasks.MainActivity;

public class NotificationUtils {
    public static void show(Context context, String tagName, String recordLabel, long recordRowid)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setContentTitle(tagName)
                        .setContentText(recordLabel)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(recordLabel))
                        .setOngoing(true);

        Intent resultIntent = new Intent(context, AddRecordActivity.class);
        resultIntent.putExtra(AddRecordActivity.OPERATION, AddRecordActivity.OPERATION_EDIT);
        resultIntent.putExtra(AddRecordActivity.EDIT_RECORD_ID, recordRowid);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(AddRecordActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        (int)recordRowid,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int)recordRowid, mBuilder.build());
    }

    public static void showInbox(Context context, TagData tag, long recordCount)
    {
        int uid = -1 * (int)tag.id;
        String fullText = tag.name + " has " + recordCount + " pending records in it.";

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setContentTitle(recordCount + " records in " + tag.name)
                        .setContentText(fullText)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(fullText))
                        .setOngoing(true);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra(MainActivity.TAG_ID_TAG, tag.id);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        uid,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int)uid, mBuilder.build());
    }

    public static void hide(Context context, long recordRowid)
    {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel((int)recordRowid);
    }

    public static void hideInbox(Context context, long tagRowid)
    {
        int uid = -1 * (int)tagRowid;
        hide(context, uid);
    }

    private static PendingIntent createAlarmIntent(Context context, long recordRowid)
    {
        Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
        intent.setAction("ru.trjoxuvw.manualrecurrencetasks.SHOW");
        intent.putExtra(NotificationBroadcastReceiver.RECORD_ROWID, recordRowid);
        return PendingIntent.getBroadcast(context, (int)recordRowid, intent, 0);
    }

    private static void schedule(Context context, long recordRowid, long time)
    {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, time, createAlarmIntent(context, recordRowid));
    }

    private static void unschedule(Context context, long recordRowid)
    {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(createAlarmIntent(context, recordRowid));
    }

    public static void registerRecord(Context context, TagData tag, RecordData record)
    {
        if (tag.isNotification)
        {
            Calendar calendarNow = Calendar.getInstance();

            if (record.nextAppear < calendarNow.getTimeInMillis())
                show(context, tag.name, record.label, record.id);
            else
                schedule(context, record.id, record.nextAppear);
        }
    }

    public static void unregisterRecord(Context context, long recordRowid)
    {
        unschedule(context, recordRowid);
        hide(context, recordRowid);
    }

    public static void registerTagData(Context context, TagData tag)
    {
        registerTag(context, tag);

        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(tag.id, Long.MIN_VALUE);
        for (RecordData record : records)
        {
            registerRecord(context, tag, record);
        }
    }

    public static void registerAllData(Context context)
    {
        ArrayList<TagData> tags = DatabaseHelper.getInstance(context).getTags();
        for (TagData tag : tags)
        {
            registerTagData(context, tag);
        }
    }

    public static void unregisterTagData(Context context, TagData tag)
    {
        unregisterTag(context, tag);

        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(tag.id, Long.MIN_VALUE);
        for (RecordData record : records)
        {
            unregisterRecord(context, record.id);
        }
    }

    public static void unregisterAllData(Context context)
    {
        ArrayList<TagData> tags = DatabaseHelper.getInstance(context).getTags();
        for (TagData tag : tags)
        {
            unregisterTagData(context, tag);
        }
    }

    public static void registerTag(Context context, TagData tag) {
        if (tag.isInbox) {
            long count = DatabaseHelper.getInstance(context).getUndoneTasksCount(tag);
            if (count > 0) {
                showInbox(context, tag, count);
            } else {
                hideInbox(context, tag.id);
            }
        }
    }

    public static void unregisterTag(Context context, TagData tag) {
        hideInbox(context, tag.id);
    }
}
