package database;

public final class RecordData extends AbstractData {
    public long groupId;
    public String label;
    public long nextAppear;
    public boolean isChecked;

    public RecordData(long id, long groupId, String label, long nextAppear, boolean isChecked) {
        super(id);
        this.groupId = groupId;
        this.label = label;
        this.nextAppear = nextAppear;
        this.isChecked = isChecked;
    }

    @Override
    public StaticInfo.Type getType() {
        return StaticInfo.Type.RECORD;
    }

    @Override
    protected String getDataStringAux(int id) {
        switch (id) {
            case 1:
                return Long.toString(groupId);
            case 2:
                return label;
            case 3:
                return Long.toString(nextAppear);
            case 4:
                return Long.toString(isChecked ? 1 : 0);
            default:
                return null;
        }
    }

    public boolean equalsRecord(RecordData o) {
        return id == o.id &&
                groupId == o.groupId &&
                label.equals(o.label) &&
                nextAppear == o.nextAppear &&
                isChecked == o.isChecked;
    }
}
