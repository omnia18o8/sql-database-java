package edu.uob;

import java.io.IOException;
import java.util.*;

public class CmdUpdate extends DBCmd {
    private String tableName;
    private List<String> columnNames;
    private List<String> values;
    private NodeQuery conditionRoot;
    private ReadWrite readWrite;

    public CmdUpdate(DBServer server, QueryParser parser) throws IOException {
        super(server);
        parser.parseUpdate();

        this.tableName = parser.getTableName();
        this.columnNames = parser.getColumnNames();
        this.values = parser.getValues();
        this.readWrite = new ReadWrite();
        this.readWrite.setCurrentDatabase(server.getDatabaseName());

        if (parser.hasWhereClause()) {
            NodeConditionParser nodeConditionParser = new NodeConditionParser(parser.getConditionTokens());
            conditionRoot = nodeConditionParser.parseConditions();
        }
    }

    @Override
    public String query(DBServer server) throws IOException {
        List<String> tableContent = readWrite.readTableFromFile(tableName);
        if (tableContent.isEmpty()) {
            return "[ERROR] Table '" + tableName + "' is empty.";
        }

        String[] firstColumnList = tableContent.get(0).split("\t");
        List<Integer> columnIndexes = getColumnIndexes(firstColumnList);

        if (columnIndexes == null) {
            return "[ERROR] One or more columns do not exist.";
        }

        List<String> updatedTable = new ArrayList<>();
        updatedTable.add(String.join("\t", firstColumnList));

        for (int i = 1; i < tableContent.size(); i++) {
            String[] rowValues = tableContent.get(i).split("\t");

            boolean shouldUpdate = (conditionRoot == null || conditionRoot.evaluate(rowValues, firstColumnList));
            List<String> updatedRow = new ArrayList<>(Arrays.asList(rowValues));

            if (shouldUpdate) {
                for (int j = 0; j < columnIndexes.size(); j++) {
                    String noQuotesValue = values.get(j).replaceAll("^'(.*)'$", "$1");
                    updatedRow.set(columnIndexes.get(j), noQuotesValue);
                }
            }
            updatedTable.add(String.join("\t", updatedRow));
        }
        return readWrite.writeTableToDB(tableName, updatedTable);
    }

    private List<Integer> getColumnIndexes(String[] firstColumnList) {
        List<Integer> indexes = new ArrayList<>();
        for (String column : columnNames) {
            int index = findColumnIndex(firstColumnList, column);
            if (index == -1) {
                System.err.println("[ERROR] Column '" + column + "' does not exist in table.");
                return null;
            }
            indexes.add(index);
        }
        return indexes;
    }
}
