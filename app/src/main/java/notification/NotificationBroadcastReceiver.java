package notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import database.DatabaseHelper;
import utils.ObjectCache;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    public static final String RECORD_ROWID = "RECORD_ROWID";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(RECORD_ROWID, 0);
        DatabaseHelper.RecordDataMin record = ObjectCache.getDbInstance(context).getRecordMin(id);
        NotificationUtils.notifyRecord(context, record.groupName, record.label, id, true);
    }
}
