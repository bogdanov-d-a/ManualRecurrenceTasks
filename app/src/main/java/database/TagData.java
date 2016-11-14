package database;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public final class TagData extends AbstractData {
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

    public String name;
    public boolean isChecklist;
    public boolean isInbox;
    public boolean isNotification;
    public FilterMode filterMode;

    public TagData(long id, String name, boolean isChecklist, boolean isInbox, boolean isNotification, FilterMode filterMode) {
        super(id);
        this.name = name;
        this.isChecklist = isChecklist;
        this.isInbox = isInbox;
        this.isNotification = isNotification;
        this.filterMode = filterMode;
    }

    @Override
    public StaticInfo.Type getType() {
        return StaticInfo.Type.TAG;
    }

    @Override
    protected String getDataStringAux(int id) {
        switch (id) {
            case 1:
                return name;
            case 2:
                return Long.toString(isChecklist ? 1 : 0);
            case 3:
                return Long.toString(isInbox ? 1 : 0);
            case 4:
                return Long.toString(isNotification ? 1 : 0);
            case 5:
                return Integer.toString(FILTER_MODE_TO_ID.get(filterMode));
            default:
                return null;
        }
    }

    public final String getLabel() {
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
