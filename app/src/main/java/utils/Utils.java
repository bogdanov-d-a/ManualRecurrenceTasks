package utils;

import android.content.Context;

import java.util.ArrayList;

import database.AbstractData;
import database.DatabaseHelper;
import database.RecordData;
import database.TagData;

public class Utils {
    public interface AbstractDataSource {
        int size();
        AbstractData get(int pos);
    }

    public static int getPositionById(AbstractDataSource list, long id) {
        for (int pos = 0; pos < list.size(); ++pos)
        {
            if (list.get(pos).getId() == id)
                return pos;
        }
        return -1;
    }

    public static int getPositionById(final ArrayList<TagData> tags, long id) {
        return Utils.getPositionById(
                new Utils.AbstractDataSource() {
                    @Override
                    public int size() {
                        return tags.size();
                    }
                    @Override
                    public AbstractData get(int pos) {
                        return tags.get(pos);
                    }
                },
                id
        );
    }

    public static boolean recordCanBeChecked(final ArrayList<TagData> tags, long recordTagId) {
        return tags.get(getPositionById(tags, recordTagId)).isChecklist;
    }

    public static boolean tagHasCheckedRecords(Context context, long tagId) {
        ArrayList<RecordData> records = DatabaseHelper.getInstance(context).getRecords(tagId, Long.MIN_VALUE, false);

        for (RecordData record : records) {
            if (record.isChecked)
                return true;
        }

        return false;
    }
}
