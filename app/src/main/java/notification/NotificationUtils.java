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
import database.GroupData;
import database.DatabaseHelper;
import ru.trjoxuvw.manualrecurrencetasks.RecordActivity;
import ru.trjoxuvw.manualrecurrencetasks.MainActivity;

public class NotificationUtils {
    public static void notifyRecord(Context context, String groupName, String recordLabel, long recordRowid)
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setContentTitle(groupName)
                        .setContentText(recordLabel)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(recordLabel))
                        .setOngoing(true);

        Intent resultIntent = new Intent(context, RecordActivity.class);
        resultIntent.putExtra(RecordActivity.OPERATION, RecordActivity.OPERATION_UPDATE);
        resultIntent.putExtra(RecordActivity.EDIT_RECORD_ID, recordRowid);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(RecordActivity.class);
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

    public static void notifyInbox(Context context, GroupData group, long recordCount)
    {
        int uid = -1 * (int)group.id;
        String fullText = recordCount + " pending records.";

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(android.R.drawable.ic_menu_info_details)
                        .setContentTitle(group.name)
                        .setContentText(fullText)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(fullText))
                        .setOngoing(true);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra(MainActivity.GROUP_ID_TAG, group.id);

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
        mNotificationManager.notify(uid, mBuilder.build());
    }

    private static void hide(Context context, long uid)
    {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel((int)uid);
    }

    public static void hideRecord(Context context, long recordRowid)
    {
        hide(context, recordRowid);
    }

    public static void hideInbox(Context context, long groupRowid)
    {
        int uid = -1 * (int)groupRowid;
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

    public static void registerRecord(Context context, GroupData group, RecordData record)
    {
        if (group.isNotification)
        {
            Calendar calendarNow = Calendar.getInstance();

            if (record.nextAppear < calendarNow.getTimeInMillis())
                notifyRecord(context, group.name, record.label, record.id);
            else
                schedule(context, record.id, record.nextAppear);
        }
    }

    public static void unregisterRecord(Context context, long recordRowid)
    {
        unschedule(context, recordRowid);
        hideRecord(context, recordRowid);
    }

    public static void registerGroupData(Context context, GroupData group)
    {
        registerGroup(context, group);

        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(group.id, Long.MIN_VALUE, false);
        for (RecordData record : records)
        {
            registerRecord(context, group, record);
        }
    }

    public static void registerAllData(Context context)
    {
        ArrayList<GroupData> groups = DatabaseHelper.getInstance(context).getGroups();
        for (GroupData group : groups)
        {
            registerGroupData(context, group);
        }
    }

    public static void unregisterGroupData(Context context, GroupData group)
    {
        unregisterGroup(context, group);

        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(group.id, Long.MIN_VALUE, false);
        for (RecordData record : records)
        {
            unregisterRecord(context, record.id);
        }
    }

    public static void unregisterAllData(Context context)
    {
        ArrayList<GroupData> groups = DatabaseHelper.getInstance(context).getGroups();
        for (GroupData group : groups)
        {
            unregisterGroupData(context, group);
        }
    }

    public static void registerGroup(Context context, GroupData group) {
        if (group.isInbox) {
            long count = DatabaseHelper.getInstance(context).getUndoneTasksCount(group);
            if (count > 0) {
                notifyInbox(context, group, count);
            } else {
                hideInbox(context, group.id);
            }
        }
    }

    public static void unregisterGroup(Context context, GroupData group) {
        hideInbox(context, group.id);
    }
}
