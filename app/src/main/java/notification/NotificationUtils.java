package notification;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

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

    private static PendingIntent createAlarmIntent(Context context, long recordRowid)
    {
        Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
        intent.setAction("ru.trjoxuvw.manualrecurrencetasks.SHOW");
        intent.putExtra(NotificationBroadcastReceiver.RECORD_ROWID, recordRowid);
        return PendingIntent.getBroadcast(context, (int)recordRowid, intent, 0);
    }

    public static void schedule(Context context, long recordRowid, long time)
    {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, time, createAlarmIntent(context, recordRowid));
    }

    public static void unschedule(Context context, long recordRowid)
    {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(createAlarmIntent(context, recordRowid));
    }
}
