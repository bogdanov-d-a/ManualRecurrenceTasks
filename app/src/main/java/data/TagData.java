package data;

public class TagData {
    public enum TimeMode {
        NO_TIMES,
        DUE_TIMES_ONLY,
        NOTIFICATIONS
    }

    public static TimeMode LongToTimeMode(long tm)
    {
        if (tm == 0)
            return TimeMode.NO_TIMES;
        if (tm == 1)
            return TimeMode.DUE_TIMES_ONLY;
        if (tm == 2)
            return TimeMode.NOTIFICATIONS;

        // TODO: report error here
        return TimeMode.NO_TIMES;
    }

    public static long TimeModeToLong(TimeMode tm)
    {
        switch (tm)
        {
            case NO_TIMES:
                return 0;
            case DUE_TIMES_ONLY:
                return 1;
            case NOTIFICATIONS:
                return 2;
            default:
                // TODO: report error here
                return -1;
        }
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
}
