package data;

public class RecordData {
    public RecordData(long id, long tagId, String label, long nextAppear, boolean needNotice) {
        this.id = id;
        this.tagId = tagId;
        this.label = label;
        this.nextAppear = nextAppear;
        this.needNotice = needNotice;
    }

    public long id;
    public long tagId;
    public String label;
    public long nextAppear;
    public boolean needNotice;
}
