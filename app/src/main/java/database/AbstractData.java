package database;

public abstract class AbstractData {
    public long id;

    public AbstractData(long id) {
        this.id = id;
    }

    public abstract StaticInfo.Type getType();

    public final String getTableName() {
        return StaticInfo.getTableName(getType());
    }

    public final int getRowCount() {
        return StaticInfo.getRowCount(getType());
    }

    public final String getRowName(int id) {
        return StaticInfo.getRowName(getType(), id);
    }

    public final String getDataString(int id) {
        if (id == 0)
            return Long.toString(this.id);
        return getDataStringAux(id);
    }

    protected abstract String getDataStringAux(int id);
}
