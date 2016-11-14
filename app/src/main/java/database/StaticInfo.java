package database;

import java.util.Arrays;
import java.util.List;

public final class StaticInfo {
    public enum Type {
        TAG,
        RECORD
    }

    public static String getTableName(Type type) {
        switch (type) {
            case TAG:
                return "tags";
            case RECORD:
                return "records";
            default:
                return null;
        }
    }

    public static String getTagTableName() {
        return getTableName(Type.TAG);
    }

    public static String getRecordTableName() {
        return getTableName(Type.RECORD);
    }

    protected static final String ID_ROW = "id";

    private static final List<String> tagRows = Arrays.asList("name", "is_checklist", "is_inbox", "is_notification", "filter_mode");
    private static final List<String> recordRows = Arrays.asList("tag_id", "label", "next_appear", "is_checked");

    private static final List<String> tagRowTypes = Arrays.asList("text", "integer", "integer", "integer", "integer");
    private static final List<String> recordRowTypes = Arrays.asList("integer references " + getTagTableName() + "(" + getTagRowName(0) + ")", "text", "integer", "integer");

    public static final class TagRowId {
        public static final int NAME = 1;
        public static final int IS_CHECKLIST = 2;
        public static final int IS_INBOX = 3;
        public static final int IS_NOTIFICATION = 4;
        public static final int FILTER_MODE = 5;
    }

    public static final class RecordRowId {
        public static final int TAG_ID = 1;
        public static final int LABEL = 2;
        public static final int NEXT_APPEAR = 3;
        public static final int IS_CHECKED = 4;
    }

    private static int getRowCountWithoutId(Type type) {
        switch (type) {
            case TAG:
                return tagRows.size();
            case RECORD:
                return recordRows.size();
            default:
                return 0;
        }
    }

    public static int getRowCount(Type type) {
        return getRowCountWithoutId(type) + 1;
    }

    public static int getTagRowCount() {
        return getRowCount(Type.TAG);
    }

    public static int getRecordRowCount() {
        return getRowCount(Type.RECORD);
    }

    private static String getRowNameImpl(Type type, int id) {
        if (id == 0)
            return ID_ROW;

        switch (type) {
            case TAG:
                return tagRows.get(id - 1);
            case RECORD:
                return recordRows.get(id - 1);
            default:
                return null;
        }
    }

    public static String getRowName(Type type, int id) {
        return getTableName(type) + "__" + getRowNameImpl(type, id);
    }

    public static String getTagRowName(int id) {
        return getRowName(Type.TAG, id);
    }

    public static String getRecordRowName(int id) {
        return getRowName(Type.RECORD, id);
    }

    public static String getRowType(Type type, int id) {
        if (id == 0)
            return "integer primary key";

        switch (type) {
            case TAG:
                return tagRowTypes.get(id - 1);
            case RECORD:
                return recordRowTypes.get(id - 1);
            default:
                return null;
        }
    }
}
