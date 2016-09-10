package database;

import java.util.ArrayList;

public class RecordData implements AbstractData {
    public long id;
    public long tagId;
    public String label;
    public long nextAppear;
    public boolean needNotice;

    public RecordData(long id, long tagId, String label, long nextAppear, boolean needNotice) {
        this.id = id;
        this.tagId = tagId;
        this.label = label;
        this.nextAppear = nextAppear;
        this.needNotice = needNotice;
    }

    @Override
    public ArrayList<String> getStringList() {
        ArrayList<String> result = new ArrayList<>();
        result.add(Long.toString(tagId));
        result.add(label);
        result.add(Long.toString(nextAppear));
        result.add(Long.toString(needNotice ? 1 : 0));
        return result;
    }

    protected static String getTableNameStatic() {
        return "records";
    }

    @Override
    public String getTableName() {
        return getTableNameStatic();
    }

    protected static class Rows {
        public static final String TAG_ID = "tag_id";
        public static final String LABEL = "label";
        public static final String NEXT_APPEAR = "next_appear";
        public static final String NOTIFICATION = "notification";
    }

    protected static ArrayList<String> getTableRowsStatic() {
        ArrayList<String> result = new ArrayList<>();
        result.add(Rows.TAG_ID);
        result.add(Rows.LABEL);
        result.add(Rows.NEXT_APPEAR);
        result.add(Rows.NOTIFICATION);
        return result;
    }

    @Override
    public ArrayList<String> getTableRows() {
        return getTableRowsStatic();
    }

    @Override
    public long getId() {
        return id;
    }

    protected static ArrayList<String> getTableRowsTypes()
    {
        ArrayList<String> result = new ArrayList<>();
        result.add("integer references " + TagData.getTableNameStatic() + "(" + DatabaseHelper.ID_ROW + ")");
        result.add("text");
        result.add("integer");
        result.add("integer");
        return result;
    }
}
