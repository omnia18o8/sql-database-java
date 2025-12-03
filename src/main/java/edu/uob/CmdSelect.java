package edu.uob;

import java.io.IOException;
import java.util.*;

public class CmdSelect extends DBCmd {
    private final String tableName;
    private final List<String> selectedColumns;
    private NodeQuery conditionRoot;
    private final ReadWrite readWrite;

    public CmdSelect(DBServer server, QueryParser parser) throws IOException {
        super(server);
        parser.parseSelect();
        this.tableName = parser.getTableName();
        this.selectedColumns = parser.getColumnNames();
        this.readWrite = new ReadWrite();
        this.readWrite.setCurrentDatabase(server.getDatabaseName());

        if (parser.hasWhereClause()) {
            NodeConditionParser nodeConditionParser = new NodeConditionParser(parser.getConditionTokens());
            conditionRoot = nodeConditionParser.parseConditions();
        }
    }

    @Override
    public String query(DBServer server) {
        try {
            List<String> tableContent = readWrite.readTableFromFile(tableName);
            if (tableContent.isEmpty()) {
                return "[ERROR] Table '" + tableName + "' is empty.";
            }

            String[] firstColumnList = tableContent.get(0).split("\t");

            List<Integer> selectedColumnIndexes = getColumnIndexes(firstColumnList);

            StringBuilder result = new StringBuilder("[OK]\n");
            result.append(formatfirstColumnList(firstColumnList, selectedColumnIndexes)).append("\n");

            for (int i = 1; i < tableContent.size(); i++) {
                String[] rowValues = tableContent.get(i).split("\t");

                if (conditionRoot == null || conditionRoot.evaluate(rowValues, firstColumnList)) {
                    result.append(formatRow(rowValues, selectedColumnIndexes)).append("\n");
                }
            }

            return result.toString().trim();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private List<Integer> getColumnIndexes(String[] firstColumnList) throws IOException {
        List<Integer> indexes = new ArrayList<>();

        if (selectedColumns.contains("*")) {
            for (int i = 0; i < firstColumnList.length; i++) {
                indexes.add(i);
            }
        } else {
            for (String column : selectedColumns) {
                int index = findColumnIndex(firstColumnList, column);

                if (index == -1) {
                    throw new IOException("[ERROR] Column '" + column + "' does not exist in table.");
                }

                indexes.add(index);
            }
        }
        return indexes;
    }


    private String formatfirstColumnList(String[] firstColumnList, List<Integer> selectedIndexes) {
        List<String> selectedHeaderValues = new ArrayList<>();
        for (int index : selectedIndexes) {
            selectedHeaderValues.add(firstColumnList[index]);
        }
        return String.join("\t", selectedHeaderValues);
    }

    private String formatRow(String[] rowValues, List<Integer> selectedIndexes) {
        List<String> selectedValues = new ArrayList<>();
        for (int index : selectedIndexes) {
            selectedValues.add(rowValues[index]);
        }
        return String.join("\t", selectedValues);
    }
}
