package database;

import java.util.ArrayList;

public class TagData implements AbstractData {
    public enum TimeMode {
        NO_TIMES,
        DUE_TIMES_ONLY,
        NOTIFICATIONS,
        FORCE_NOTIFICATIONS,
    }

    public static TimeMode longToTimeMode(long tm)
    {
        if (tm == 0)
            return TimeMode.NO_TIMES;
        if (tm == 1)
            return TimeMode.DUE_TIMES_ONLY;
        if (tm == 2)
            return TimeMode.NOTIFICATIONS;
        if (tm == 3)
            return TimeMode.FORCE_NOTIFICATIONS;

        // TODO: report error here
        return TimeMode.NO_TIMES;
    }

    public static long timeModeToLong(TimeMode tm)
    {
        switch (tm)
        {
            case NO_TIMES:
                return 0;
            case DUE_TIMES_ONLY:
                return 1;
            case NOTIFICATIONS:
                return 2;
            case FORCE_NOTIFICATIONS:
                return 3;
            default:
                // TODO: report error here
                return -1;
        }
    }

    public static ArrayList<String> getTagNames() {
        ArrayList<String> result = new ArrayList<>();
        result.add("No times");
        result.add("Due times only");
        result.add("Notifications");
        result.add("Force notifications");
        return result;
    }

    public long id;
    public String name;
    public boolean isChecklist;
    public TimeMode timeMode;

    public TagData(long id, String name, boolean isChecklist, TimeMode timeMode) {
        this.id = id;
        this.name = name;
        this.isChecklist = isChecklist;
        this.timeMode = timeMode;
    }

    @Override
    public ArrayList<String> getStringList() {
        ArrayList<String> result = new ArrayList<>();
        result.add(name);
        result.add(Long.toString(isChecklist ? 1 : 0));
        result.add(Long.toString(TagData.timeModeToLong(timeMode)));
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
        public static final String TIME_MODE = "time_mode";
    }

    protected static ArrayList<String> getTableRowsStatic() {
        ArrayList<String> result = new ArrayList<>();
        result.add(Rows.NAME);
        result.add(Rows.IS_CHECKLIST);
        result.add(Rows.TIME_MODE);
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
        return result;
    }
}
