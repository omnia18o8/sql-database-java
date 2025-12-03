package edu.uob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdInsert extends DBCmd {
    private final List<String> values;
    private final String tableName;
    private final ReadWrite readWrite;

    public CmdInsert(DBServer server, QueryParser parser) throws IOException {
        super(server);
        if (server.getDatabaseName() == null) {
            throw new IOException("[ERROR] No database selected.");
        }

        parser.parseInsert();
        this.values = parser.getValues();
        this.tableName = parser.getTableName();
        this.readWrite = new ReadWrite();
        readWrite.setCurrentDatabase(server.getDatabaseName());
    }

    @Override
    public String query(DBServer server) {
        List<String> tableContent = readWrite.readTableFromFile(tableName);
        int newId = server.getNextIdForTable(tableName);

        if (tableContent.isEmpty()) {
            List<String> headerRow = new ArrayList<>();
            headerRow.add("id");

            List<String> uniqueColumns = new ArrayList<>();
            for (String value : values) {
                if (!value.equalsIgnoreCase("id")) {
                    if (uniqueColumns.contains(value)) {
                        return "[ERROR] Duplicate column name detected: " + value;
                    }
                    uniqueColumns.add(value);
                }
            }
            headerRow.addAll(uniqueColumns);
            tableContent.add(String.join("\t", headerRow));
        }

        String[] columnList = tableContent.get(0).split("\t");
        int expectedColumnCount = columnList.length - 1;

        if (values.size() != expectedColumnCount) {
            return "[ERROR] Column count mismatch: Expected " + expectedColumnCount +
                    ", but got " + values.size() + ".";
        }

        List<String> cleanedValues = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            cleanedValues.add(value);
        }

        List<String> updatedRow = new ArrayList<>();
        updatedRow.add(String.valueOf(newId));
        updatedRow.addAll(cleanedValues);
        tableContent.add(String.join("\t", updatedRow));

        server.updateHighestIdForTable(tableName, newId);

        return readWrite.writeTableToDB(tableName, tableContent);
    }
}
