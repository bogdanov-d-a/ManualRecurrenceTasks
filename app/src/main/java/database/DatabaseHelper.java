package database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "Tasks.db";

    private static String createGen(StaticInfo.Type type) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table " + StaticInfo.getTableName(type) + " (");

        for (int i = 0; i < StaticInfo.getRowCount(type); ++i) {
            if (i != 0)
                sb.append(",");
            sb.append(StaticInfo.getRowName(type, i) + " " + StaticInfo.getRowType(type, i));
        }

        sb.append(");");
        return sb.toString();
    }

    private static String insertGen(AbstractData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into " + data.getTableName() + " (");

        for (int i = 1; i < data.getRowCount(); ++i) {
            if (i != 1)
                sb.append(",");
            sb.append(data.getRowName(i));
        }

        sb.append(") values (");

        for (int i = 1; i < data.getRowCount(); ++i) {
            if (i != 1)
                sb.append(",");
            sb.append(escapeStr(data.getDataString(i)));
        }

        sb.append(");");
        return sb.toString();
    }

    private static String updateGen(AbstractData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("update " + data.getTableName() + " set ");

        for (int i = 1; i < data.getRowCount(); ++i) {
            if (i != 1)
                sb.append(",");
            sb.append(data.getRowName(i) + "=" + escapeStr(data.getDataString(i)));
        }

        sb.append(" where " + data.getRowName(0) + "=" + escapeStr(data.getDataString(0)) + ";");
        return sb.toString();
    }

    private static RecordData createRecordDataFromCursor(Cursor cursor) {
        return new RecordData(
                cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(0))),
                cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(1))),
                cursor.getString(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(2))),
                cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(3))),
                cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(4))) != 0
        );
    }

    private static DatabaseHelper instance = null;
    private static final Object instanceLock = new Object();

    public static DatabaseHelper getInstance(Context context)
    {
        synchronized (instanceLock)
        {
            if (instance == null)
                instance = new DatabaseHelper(context);
            return instance;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createGen(StaticInfo.Type.GROUP));
        db.execSQL(createGen(StaticInfo.Type.RECORD));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int curVersion = oldVersion;

        while (curVersion != newVersion) {
            switch (curVersion) {
                case 1:
                    db.execSQL("alter table " + StaticInfo.getGroupTableName() +
                            " add column " + StaticInfo.getGroupRowName(StaticInfo.GroupRowId.IS_CHECKLIST) + " integer;");
                    db.execSQL("alter table " + StaticInfo.getRecordTableName() +
                            " add column " + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.IS_CHECKED) + " integer;");
                    break;

                case 2:
                    db.execSQL("alter table " + StaticInfo.getGroupTableName() +
                            " add column " + StaticInfo.getGroupRowName(StaticInfo.GroupRowId.IS_INBOX) + " integer;");
                    break;

                case 3:
                    db.execSQL("alter table " + StaticInfo.getGroupTableName() +
                            " add column " + StaticInfo.getGroupRowName(StaticInfo.GroupRowId.IS_NOTIFICATION) + " integer;");

                    db.execSQL("alter table " + StaticInfo.getRecordTableName() +
                            " rename to " + StaticInfo.getRecordTableName() + "_old;");
                    db.execSQL(createGen(StaticInfo.Type.RECORD));
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append("insert into " + StaticInfo.getRecordTableName() + " select ");

                        for (int i = 0; i < StaticInfo.getRecordRowCount(); ++i) {
                            if (i != 0)
                                sb.append(",");
                            sb.append(StaticInfo.getRecordRowName(i));
                        }

                        sb.append(" from " + StaticInfo.getRecordTableName() + "_old;");
                        db.execSQL(sb.toString());
                    }
                    db.execSQL("drop table " + StaticInfo.getRecordTableName() + "_old;");

                    break;

                case 4:
                    db.execSQL("alter table " + StaticInfo.getGroupTableName() +
                            " add column " + StaticInfo.getGroupRowName(StaticInfo.GroupRowId.FILTER_MODE) + " integer;");
                    break;

                case 5:
                    // rename old group table
                    db.execSQL("alter table " + StaticInfo.getGroupTableName() +
                            " rename to " + StaticInfo.getGroupTableName() + "_old;");

                    // rename old record table
                    db.execSQL("alter table " + StaticInfo.getRecordTableName() +
                            " rename to " + StaticInfo.getRecordTableName() + "_old;");

                    // create new group table
                    db.execSQL(createGen(StaticInfo.Type.GROUP));
                    db.execSQL("insert into " + StaticInfo.getGroupTableName() + " select * from " + StaticInfo.getGroupTableName() + "_old;");

                    // create new record table
                    db.execSQL(createGen(StaticInfo.Type.RECORD));
                    db.execSQL("insert into " + StaticInfo.getRecordTableName() + " select * from " + StaticInfo.getRecordTableName() + "_old;");

                    // delete old record table
                    db.execSQL("drop table " + StaticInfo.getRecordTableName() + "_old;");

                    // delete old group table
                    db.execSQL("drop table " + StaticInfo.getGroupTableName() + "_old;");

                default:
                    break;
            }

            ++curVersion;
        }
    }

    private static long getLastInsertRowid(SQLiteDatabase db)
    {
        Cursor cursor = db.rawQuery("select last_insert_rowid();", null);

        try
        {
            if (cursor.moveToFirst())
                return cursor.getLong(0);
            return 0;
        }
        finally
        {
            cursor.close();
        }
    }

    public ArrayList<GroupData> getGroups()
    {
        ArrayList<GroupData> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + StaticInfo.getGroupTableName() +
                " order by " + StaticInfo.getGroupRowName(StaticInfo.GroupRowId.NAME) + " asc;", null);
        if (cursor.moveToFirst())
        {
            do
            {
                result.add(new GroupData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(0))),
                        cursor.getString(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(1))),
                        cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(2))) != 0,
                        cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(3))) != 0,
                        cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(4))) != 0,
                        GroupData.ID_TO_FILTER_MODE.get((int)cursor.getLong(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(5))))
                ));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return result;
    }

    private static String escapeStr(String str)
    {
        StringBuilder result = new StringBuilder();
        result.append('\'');

        for (char c : str.toCharArray()) {
            if (c == '\'')
                result.append('\'');
            result.append(c);
        }

        result.append('\'');
        return result.toString();
    }

    private ArrayList<RecordData> getRecords(SQLiteDatabase db, long groupId, long maxDate, boolean notificationsOnly)
    {
        ArrayList<RecordData> result = new ArrayList<>();

        Cursor cursor;
        {
            String groupIdExpr = "1";
            if (groupId != Long.MIN_VALUE)
                groupIdExpr = StaticInfo.getRecordRowName(StaticInfo.RecordRowId.GROUP_ID) + "=" + escapeStr(Long.toString(groupId));

            String maxDateExpr = "1";
            if (maxDate > Long.MIN_VALUE)
                maxDateExpr = StaticInfo.getRecordRowName(StaticInfo.RecordRowId.NEXT_APPEAR) + "<" + escapeStr(Long.toString(maxDate));

            String notificationsOnlyExpr = "1";
            if (notificationsOnly)
                notificationsOnlyExpr = StaticInfo.getGroupRowName(StaticInfo.GroupRowId.IS_NOTIFICATION) + "!=" + escapeStr(Long.toString(0));

            cursor = db.rawQuery("select * from " + StaticInfo.getRecordTableName() + " inner join " + StaticInfo.getGroupTableName() +
                    " on " + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.GROUP_ID) + "=" + StaticInfo.getGroupRowName(0) +
                    " where (" + groupIdExpr + ") and (" + maxDateExpr + ") and (" + notificationsOnlyExpr + ")" +
                    " order by " + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.NEXT_APPEAR) + " asc;", null);
        }

        if (cursor.moveToFirst())
        {
            do
            {
                result.add(createRecordDataFromCursor(cursor));
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return result;
    }

    public ArrayList<RecordData> getRecords(long groupId, long maxDate, boolean notificationsOnly)
    {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<RecordData> result = getRecords(db, groupId, maxDate, notificationsOnly);
        db.close();
        return result;
    }

    public long getUndoneTasksCount(GroupData group) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(" + StaticInfo.getRecordRowName(0) + ") from " + StaticInfo.getRecordTableName() +
                " where (" + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.GROUP_ID) + "=" + escapeStr(Long.toString(group.id)) + ")" +
                " and (" + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.IS_CHECKED) + "=" + escapeStr(Long.toString(0)) + ");", null);

        cursor.moveToFirst();
        long result = cursor.getLong(0);

        cursor.close();
        db.close();
        return result;
    }

    public long create(AbstractData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(insertGen(data));
        long id = getLastInsertRowid(db);
        db.close();
        return id;
    }

    public void deleteEmptyGroup(long id)
    {
        SQLiteDatabase db = getWritableDatabase();

        ArrayList<RecordData> records = getRecords(db, id, Long.MIN_VALUE, false);
        if (records.isEmpty())
        {
            db.execSQL("delete from " + StaticInfo.getGroupTableName() + " where " + StaticInfo.getGroupRowName(0) + "=" + escapeStr(Long.toString(id)) + ";");
        }

        db.close();
    }

    public RecordData getRecord(long id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + StaticInfo.getRecordTableName() + " where " + StaticInfo.getRecordRowName(0) + "=" + escapeStr(Long.toString(id)) + ";", null);

        try
        {
            if (cursor.moveToFirst())
            {
                return createRecordDataFromCursor(cursor);
            }
            return null;
        }
        finally
        {
            cursor.close();
            db.close();
        }
    }

    public static class RecordDataMin
    {
        public RecordDataMin(String groupName, String label) {
            this.groupName = groupName;
            this.label = label;
        }

        public String groupName;
        public String label;
    }

    public RecordDataMin getRecordMin(long id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + StaticInfo.getRecordTableName() + " inner join " + StaticInfo.getGroupTableName() +
                " on " + StaticInfo.getRecordRowName(StaticInfo.RecordRowId.GROUP_ID) + "=" + StaticInfo.getGroupRowName(0) +
                " where " + StaticInfo.getRecordRowName(0) + "=" + escapeStr(Long.toString(id)) + ";", null);

        try
        {
            if (cursor.moveToFirst())
            {
                return new RecordDataMin(
                        cursor.getString(cursor.getColumnIndexOrThrow(StaticInfo.getGroupRowName(StaticInfo.GroupRowId.NAME))),
                        cursor.getString(cursor.getColumnIndexOrThrow(StaticInfo.getRecordRowName(StaticInfo.RecordRowId.LABEL)))
                );
            }
            return null;
        }
        finally
        {
            cursor.close();
            db.close();
        }
    }

    public void update(AbstractData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(updateGen(data));
        db.close();
    }

    public void deleteRecord(long id)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + StaticInfo.getRecordTableName() + " where " + StaticInfo.getRecordRowName(0) + "=" + escapeStr(Long.toString(id)) + ";");
        db.close();
    }
}
