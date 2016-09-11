package database;

import java.util.ArrayList;

public class TagData implements AbstractData {
    public long id;
    public String name;
    public boolean isChecklist;

    public TagData(long id, String name, boolean isChecklist) {
        this.id = id;
        this.name = name;
        this.isChecklist = isChecklist;
    }

    @Override
    public ArrayList<String> getStringList() {
        ArrayList<String> result = new ArrayList<>();
        result.add(name);
        result.add(Long.toString(isChecklist ? 1 : 0));
        return result;
    }

    protected static String getTableNameStatic() {
        return "tags";
    }

    @Override
    public String getTableName() {
        return getTableNameStatic();
    }

    protected static class Rows {
        public static final String NAME = "name";
        public static final String IS_CHECKLIST = "is_checklist";
    }

    protected static ArrayList<String> getTableRowsStatic() {
        ArrayList<String> result = new ArrayList<>();
        result.add(Rows.NAME);
        result.add(Rows.IS_CHECKLIST);
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
        result.add("text");
        result.add("integer");
        return result;
    }
}
