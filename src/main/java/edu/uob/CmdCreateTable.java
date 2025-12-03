package edu.uob;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdCreateTable extends DBCmd {
    private final List<String> columnNames;
    private final String tableName;

    public CmdCreateTable(DBServer server, QueryParser parser) throws IOException {
        super(server);
        if (server.getDatabaseName() == null) {
            throw new IOException("[ERROR] No database selected.");
        }

        ReadWrite readWrite = new ReadWrite();
        readWrite.setCurrentDatabase(server.getDatabaseName());

        parser.parseCreateTable();
        this.columnNames = parser.getColumnNames();
        this.tableName = parser.getTableName();
        columnNames.remove("id");
    }

    @Override
    public String query(DBServer server) {
        String tablePath = server.getStorageFolderPath() + File.separator + server.getDatabaseName() + File.separator + tableName + ".tab";
        File tableFile = new File(tablePath);

        if(server.doesTableExist(tableName) || tableFile.exists()) {
            return "[ERROR] Table " + tableName + " already exists.";
        }

        try {

            FileWriter writer = new FileWriter(tableFile);
            List<String> updatedColumns = new ArrayList<>();

            if (!columnNames.isEmpty()) {
                updatedColumns.add("id");
                updatedColumns.addAll(columnNames);
            }

            if (!updatedColumns.isEmpty()) {
                writer.write(String.join("\t", updatedColumns) + "\n");
            }

            writer.close();
            server.addTable(tableName);
            return "[OK]";
        } catch (IOException e) {
            return "[ERROR] Could not create table.";
        }
    }

}

