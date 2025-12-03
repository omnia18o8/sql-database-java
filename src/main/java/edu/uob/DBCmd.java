package edu.uob;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class DBCmd {
    String databaseName;
    List<String> tableNames;
    List<String> columnNames;
    public int id = 1;

    public DBCmd(DBServer server) {
        this.databaseName = server.getDatabaseName();
        this.tableNames = new ArrayList<>();
        this.columnNames = new ArrayList<>();
    }

    public abstract String query(DBServer server) throws IOException;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    int findColumnIndex(String[] firstColumnList, String columnName) {
        for (int i = 0; i < firstColumnList.length; i++) {
            if (firstColumnList[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
