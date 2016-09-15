package database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Tasks.db";

    protected static final String ID_ROW = "id";

    private static String createGen(String name, ArrayList<String> rows, ArrayList<String> types) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table " + name + " (");

        sb.append(ID_ROW + " integer primary key");

        for (int i = 0; i < rows.size(); ++i)
        {
            sb.append(",");
            sb.append(rows.get(i) + " " + types.get(i));
        }

        sb.append(");");
        return sb.toString();
    }

    private static String insertGen(String name, ArrayList<String> rows, ArrayList<String> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into " + name + " (");

        for (int i = 0; i < rows.size(); ++i)
        {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(rows.get(i));
        }

        sb.append(") values (");

        for (int i = 0; i < data.size(); ++i)
        {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(escapeStr(data.get(i)));
        }

        sb.append(");");
        return sb.toString();
    }

    private static String updateGen(String name, ArrayList<String> rows, long id, ArrayList<String> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("update " + name + " set ");

        for (int i = 0; i < rows.size(); ++i)
        {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(rows.get(i) + "=" + escapeStr(data.get(i)));
        }

        sb.append(" where " + ID_ROW + "=" + escapeStr(Long.toString(id)) + ";");
        return sb.toString();
    }

    private static RecordData createRecordDataFromCursor(Cursor cursor) {
        return new RecordData(
                cursor.getLong(cursor.getColumnIndexOrThrow(ID_ROW)),
                cursor.getLong(cursor.getColumnIndexOrThrow(RecordData.Rows.TAG_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(RecordData.Rows.LABEL)),
                cursor.getLong(cursor.getColumnIndexOrThrow(RecordData.Rows.NEXT_APPEAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(RecordData.Rows.NOTIFICATION)) != 0,
                cursor.getLong(cursor.getColumnIndexOrThrow(RecordData.Rows.IS_CHECKED)) != 0
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
        db.execSQL(createGen(TagData.getTableNameStatic(), TagData.getTableRowsStatic(), TagData.getTableRowsTypes()));
        db.execSQL(createGen(RecordData.getTableNameStatic(), RecordData.getTableRowsStatic(), RecordData.getTableRowsTypes()));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int curVersion = oldVersion;

        while (curVersion != newVersion) {
            switch (curVersion) {
                case 1:
                    db.execSQL("alter table " + TagData.getTableNameStatic() + " add column " + TagData.Rows.IS_CHECKLIST + " integer;");
                    db.execSQL("alter table " + RecordData.getTableNameStatic() + " add column " + RecordData.Rows.IS_CHECKED + " integer;");
                    break;

                case 2:
                    // TODO: upgrade db
                    break;

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

    public ArrayList<TagData> getTags()
    {
        ArrayList<TagData> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TagData.getTableNameStatic() + " order by " + TagData.Rows.NAME + " asc;", null);
        if (cursor.moveToFirst())
        {
            do
            {
                result.add(new TagData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ID_ROW)),
                        cursor.getString(cursor.getColumnIndexOrThrow(TagData.Rows.NAME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(TagData.Rows.IS_CHECKLIST)) != 0,
                        cursor.getLong(cursor.getColumnIndexOrThrow(TagData.Rows.IS_INBOX)) != 0
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

    public ArrayList<RecordData> getRecords(long tagId, long maxDate, boolean notificationOnly)
    {
        ArrayList<RecordData> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor;
        {
            String tagIdExpr = "1";
            if (tagId != Long.MIN_VALUE)
                tagIdExpr = RecordData.Rows.TAG_ID + "=" + escapeStr(Long.toString(tagId));

            String maxDateExpr = "1";
            if (maxDate > Long.MIN_VALUE)
                maxDateExpr = RecordData.Rows.NEXT_APPEAR + "<" + escapeStr(Long.toString(maxDate));

            String notificationOnlyExpr = "1";
            if (notificationOnly)
                notificationOnlyExpr = RecordData.Rows.NOTIFICATION + "=" + escapeStr(Long.toString(1));

            cursor = db.rawQuery("select * from " + RecordData.getTableNameStatic() + " where (" + tagIdExpr + ") and (" + maxDateExpr + ") and (" + notificationOnlyExpr + ") order by " + RecordData.Rows.NEXT_APPEAR + " asc;", null);
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
        db.close();

        return result;
    }

    public long add(AbstractData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(insertGen(data.getTableName(), data.getTableRows(), data.getStringList()));
        long id = getLastInsertRowid(db);
        db.close();
        return id;
    }

    public void deleteTag(long id)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + RecordData.getTableNameStatic() + " where " + RecordData.Rows.TAG_ID + "=" + escapeStr(Long.toString(id)) + ";");
        db.execSQL("delete from " + TagData.getTableNameStatic() + " where " + ID_ROW + "=" + escapeStr(Long.toString(id)) + ";");
        db.close();
    }

    public RecordData getRecord(long id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + RecordData.getTableNameStatic() + " where " + ID_ROW + "=" + escapeStr(Long.toString(id)) + ";", null);

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
        public RecordDataMin(String tagName, String label) {
            this.tagName = tagName;
            this.label = label;
        }

        public String tagName;
        public String label;
    }

    public RecordDataMin getRecordMin(long id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + RecordData.getTableNameStatic() + " inner join " + TagData.getTableNameStatic() + " on " + RecordData.Rows.TAG_ID + "=" + TagData.getTableNameStatic() + "." + ID_ROW + " where " + RecordData.getTableNameStatic() + "." + ID_ROW + "=" + escapeStr(Long.toString(id)) + ";", null);

        try
        {
            if (cursor.moveToFirst())
            {
                return new RecordDataMin(
                        cursor.getString(cursor.getColumnIndexOrThrow(TagData.Rows.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RecordData.Rows.LABEL))
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
        db.execSQL(updateGen(data.getTableName(), data.getTableRows(), data.getId(), data.getStringList()));
        db.close();
    }

    public void deleteRecord(long id)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + RecordData.getTableNameStatic() + " where " + ID_ROW + "=" + escapeStr(Long.toString(id)) + ";");
        db.close();
    }
}
