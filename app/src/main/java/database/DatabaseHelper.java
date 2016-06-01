package database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import data.RecordData;
import data.TagData;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Tasks.db";

    private static final String TAGS_TABLE = "tags";
    private static final String RECORDS_TABLE = "records";

    private static final String ID_ROW = "id";

    private static class TagsRows {
        public static final String NAME = "name";
    }

    private static class RecordsRows {
        public static final String TAG_ID = "tag_id";
        public static final String LABEL = "label";
        public static final String NEXT_APPEAR = "next_appear";
        public static final String NOTIFICATION = "notification";
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
        db.execSQL("create table " + TAGS_TABLE + " (" +
                ID_ROW + " integer primary key," +
                TagsRows.NAME + " text);");

        db.execSQL("create table " + RECORDS_TABLE + " (" +
                ID_ROW + " integer primary key," +
                RecordsRows.TAG_ID + " integer references " + TAGS_TABLE + "(" + ID_ROW + ")," +
                RecordsRows.LABEL + " text," +
                RecordsRows.NEXT_APPEAR + " integer," +
                RecordsRows.NOTIFICATION + " integer);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
        Cursor cursor = db.rawQuery("select * from " + TAGS_TABLE + ";", null);
        if (cursor.moveToFirst())
        {
            do
            {
                result.add(new TagData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ID_ROW)),
                        cursor.getString(cursor.getColumnIndexOrThrow(TagsRows.NAME))
                ));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return result;
    }

    public ArrayList<RecordData> getRecords(long tagId, long maxDate)
    {
        ArrayList<RecordData> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor;
        {
            String tagIdExpr = "1";
            if (tagId != Long.MIN_VALUE)
                tagIdExpr = RecordsRows.TAG_ID + "='" + tagId + "'";

            String maxDateExpr = "1";
            if (maxDate > Long.MIN_VALUE)
                maxDateExpr = RecordsRows.NEXT_APPEAR + "<'" + maxDate + "'";

            cursor = db.rawQuery("select * from " + RECORDS_TABLE + " where (" + tagIdExpr + ") and (" + maxDateExpr + ") order by " + RecordsRows.NEXT_APPEAR + " asc;", null);
        }

        if (cursor.moveToFirst())
        {
            do
            {
                result.add(new RecordData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ID_ROW)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.TAG_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RecordsRows.LABEL)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.NEXT_APPEAR)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.NOTIFICATION)) != 0
                ));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return result;
    }

    public long addTag(TagData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into " + TAGS_TABLE + " (" +
                TagsRows.NAME + ") values ('" +
                data.name + "');");
        long id = getLastInsertRowid(db);
        db.close();
        return id;
    }

    public long addRecord(RecordData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into " + RECORDS_TABLE + " (" +
                RecordsRows.TAG_ID + "," +
                RecordsRows.LABEL + "," +
                RecordsRows.NEXT_APPEAR + "," +
                RecordsRows.NOTIFICATION + ") values ('" +
                data.tagId + "','" +
                data.label + "','" +
                data.nextAppear + "','" +
                (data.needNotice ? 1 : 0) + "');");
        long id = getLastInsertRowid(db);
        db.close();
        return id;
    }

    public void deleteTag(long id)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + RECORDS_TABLE + " where " + RecordsRows.TAG_ID + "=" + id + ";");
        db.execSQL("delete from " + TAGS_TABLE + " where " + ID_ROW + "=" + id + ";");
        db.close();
    }

    public RecordData getRecord(long id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + RECORDS_TABLE + " where " + ID_ROW + "=" + id + ";", null);

        try
        {
            if (cursor.moveToFirst())
            {
                return new RecordData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ID_ROW)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.TAG_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RecordsRows.LABEL)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.NEXT_APPEAR)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(RecordsRows.NOTIFICATION)) != 0
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
        Cursor cursor = db.rawQuery("select * from " + RECORDS_TABLE + " inner join " + TAGS_TABLE + " on " + RecordsRows.TAG_ID + "=" + TAGS_TABLE + "." + ID_ROW + " where " + RECORDS_TABLE + "." + ID_ROW + "=" + id + ";", null);

        try
        {
            if (cursor.moveToFirst())
            {
                return new RecordDataMin(
                        cursor.getString(cursor.getColumnIndexOrThrow(TagsRows.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(RecordsRows.LABEL))
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

    public void updateRecord(RecordData data)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("update " + RECORDS_TABLE + " set " +
                RecordsRows.TAG_ID + "='" + data.tagId + "'," +
                RecordsRows.LABEL + "='" + data.label + "'," +
                RecordsRows.NEXT_APPEAR + "='" + data.nextAppear + "'," +
                RecordsRows.NOTIFICATION + "='" + (data.needNotice ? 1 : 0) + "'" +
                " where " + ID_ROW + "=" + data.id + ";");
        db.close();
    }

    public void deleteRecord(long id)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + RECORDS_TABLE + " where " + ID_ROW + "=" + id + ";");
        db.close();
    }
}
