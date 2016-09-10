package database;

import java.util.ArrayList;

public interface AbstractData {
    ArrayList<String> getStringList();
    String getTableName();
    ArrayList<String> getTableRows();
    long getId();
}
