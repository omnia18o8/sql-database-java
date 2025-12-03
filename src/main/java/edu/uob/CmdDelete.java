package edu.uob;

import java.io.IOException;
import java.util.*;

public class CmdDelete extends DBCmd {
    private final String tableName;
    private NodeQuery conditionRoot;
    private final ReadWrite readWrite;

    public CmdDelete(DBServer server, QueryParser parser) throws IOException {
        super(server);
        parser.parseDelete();

        this.tableName = parser.getTableName();
        this.readWrite = new ReadWrite();
        readWrite.setCurrentDatabase(server.getDatabaseName());

        if (parser.hasWhereClause()) {
            NodeConditionParser nodeConditionParser = new NodeConditionParser(parser.getConditionTokens());
            conditionRoot = nodeConditionParser.parseConditions();
        }
    }

    @Override
    public String query(DBServer server) {
        List<String> tableContent = readWrite.readTableFromFile(tableName);
        if (tableContent.isEmpty()) {
            return "[ERROR] Table '" + tableName + "' is empty.";
        }

        String[] firstColumnList = tableContent.get(0).split("\t");
        List<String> updatedTable = new ArrayList<>();
        updatedTable.add(String.join("\t", firstColumnList));

        for (int i = 1; i < tableContent.size(); i++) {
            String[] rowValues = tableContent.get(i).split("\t");

            boolean shouldDelete = (conditionRoot != null && conditionRoot.evaluate(rowValues, firstColumnList));

            if (!shouldDelete) {
                updatedTable.add(String.join("\t", rowValues));
            }
        }
        return readWrite.writeTableToDB(tableName, updatedTable);
    }
}
