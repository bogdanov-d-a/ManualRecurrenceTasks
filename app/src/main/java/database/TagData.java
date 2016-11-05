package database;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class TagData implements AbstractData {
    public enum FilterMode {
        ONLY_ALL,
        DEFAULT_ALL,
        DEFAULT_FILTERED,
    }

    public static final ArrayList<FilterMode> ID_TO_FILTER_MODE;
    public static final ArrayList<String> ID_TO_FILTER_MODE_LABEL;
    public static final Map<FilterMode, Integer> FILTER_MODE_TO_ID;
    static {
        ID_TO_FILTER_MODE = new ArrayList<>();
        ID_TO_FILTER_MODE.add(FilterMode.ONLY_ALL);
        ID_TO_FILTER_MODE.add(FilterMode.DEFAULT_ALL);
        ID_TO_FILTER_MODE.add(FilterMode.DEFAULT_FILTERED);

        ID_TO_FILTER_MODE_LABEL = new ArrayList<>();
        ID_TO_FILTER_MODE_LABEL.add("Only all");
        ID_TO_FILTER_MODE_LABEL.add("Default all");
        ID_TO_FILTER_MODE_LABEL.add("Default filtered");

        FILTER_MODE_TO_ID = new TreeMap<>();
        for (int i = 0; i < ID_TO_FILTER_MODE.size(); ++i) {
            FILTER_MODE_TO_ID.put(ID_TO_FILTER_MODE.get(i), new Integer(i));
        }
    }

    public long id;
    public String name;
    public boolean isChecklist;
    public boolean isInbox;
    public boolean isNotification;
    public FilterMode filterMode;

    public TagData(long id, String name, boolean isChecklist, boolean isInbox, boolean isNotification, FilterMode filterMode) {
        this.id = id;
        this.name = name;
        this.isChecklist = isChecklist;
        this.isInbox = isInbox;
        this.isNotification = isNotification;
        this.filterMode = filterMode;
    }

    @Override
    public ArrayList<String> getStringList() {
        ArrayList<String> result = new ArrayList<>();
        result.add(name);
        result.add(Long.toString(isChecklist ? 1 : 0));
        result.add(Long.toString(isInbox ? 1 : 0));
        result.add(Long.toString(isNotification ? 1 : 0));
        result.add(Integer.toString(FILTER_MODE_TO_ID.get(filterMode)));
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
        public static final String IS_INBOX = "is_inbox";
        public static final String IS_NOTIFICATION = "is_notification";
        public static final String FILTER_MODE = "filter_mode";
    }

    protected static ArrayList<String> getTableRowsStatic() {
        ArrayList<String> result = new ArrayList<>();
        result.add(Rows.NAME);
        result.add(Rows.IS_CHECKLIST);
        result.add(Rows.IS_INBOX);
        result.add(Rows.IS_NOTIFICATION);
        result.add(Rows.FILTER_MODE);
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
        result.add("integer");
        result.add("integer");
        result.add("integer");
        return result;
    }

    public String getLabel() {
        StringBuilder label = new StringBuilder();

        label.append('[');
        label.append(isChecklist ? 'c' : '-');
        label.append(isInbox ? 'i' : '-');
        label.append(isNotification ? 'n' : '-');
        label.append(']');

        label.append(' ');

        label.append(name);

        return label.toString();
    }
}
