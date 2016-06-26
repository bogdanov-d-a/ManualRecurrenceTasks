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

import data.RecordData;
import data.TagData;
import database.DatabaseHelper;
import ru.trjoxuvw.manualrecurrencetasks.AddRecordActivity;

public class NotificationUtils {
    public static void show(Context context, String tagName, String recordLabel, long recordRowid)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setContentTitle(tagName)
                        .setContentText(recordLabel)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(recordLabel));

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

    public static void hide(Context context, long recordRowid)
    {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel((int)recordRowid);
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

    public static void registerRecord(Context context, RecordData data, String tagName)
    {
        if (data.needNotice)
        {
            Calendar calendarNow = Calendar.getInstance();

            if (data.nextAppear < calendarNow.getTimeInMillis())
                show(context, tagName, data.label, data.id);
            else
                schedule(context, data.id, data.nextAppear);
        }
    }

    public static void unregisterRecord(Context context, long recordRowid)
    {
        unschedule(context, recordRowid);
        hide(context, recordRowid);
    }

    public static void registerAllRecords(Context context)
    {
        ArrayList<TagData> tags = DatabaseHelper.getInstance(context).getTags();
        for (TagData tag : tags)
        {
            ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(tag.id, Long.MIN_VALUE);
            for (RecordData record : records)
            {
                registerRecord(context, record, tag.name);
            }
        }
    }

    public static void unregisterAllRecords(Context context)
    {
        ArrayList<TagData> tags = DatabaseHelper.getInstance(context).getTags();
        for (TagData tag : tags)
        {
            ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(tag.id, Long.MIN_VALUE);
            for (RecordData record : records)
            {
                unregisterRecord(context, record.id);
            }
        }
    }
}
