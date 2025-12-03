package edu.uob;

import java.io.IOException;
import java.util.List;

public class CmdJoin extends DBCmd {
    private final String table1;
    private final String table2;
    private final String column1;
    private final String column2;
    private final ReadWrite readWrite;

    public CmdJoin(DBServer server, QueryParser parser) throws IOException {
        super(server);
        parser.parseJoin();
        this.table1 = parser.getTable1();
        this.table2 = parser.getTable2();
        this.column1 = parser.getColumn1();
        this.column2 = parser.getColumn2();
        this.readWrite = new ReadWrite();
        this.readWrite.setCurrentDatabase(server.getDatabaseName());
    }

    @Override
    public String query(DBServer server) {
        List<String> table1Content = readWrite.readTableFromFile(table1);
        List<String> table2Content = readWrite.readTableFromFile(table2);
        if (table1Content.isEmpty() || table2Content.isEmpty()) {
            return "[ERROR] One or both tables are empty.";
        }

        String[] firstColumnList1 = table1Content.get(0).split("\t");
        String[] firstColumnList2 = table2Content.get(0).split("\t");
        int index1 = findColumnIndex(firstColumnList1, column1);
        int index2 = findColumnIndex(firstColumnList2, column2);

        if (index1 == -1 || index2 == -1) {
            return "[ERROR] One or both columns not found.";
        }

        StringBuilder result = new StringBuilder("[OK]\n");
        result.append("id\t");
        for (String s : firstColumnList1) {
            if (!s.equalsIgnoreCase("id") && !s.equalsIgnoreCase(column1)) {
                result.append(table1).append(".").append(s).append("\t");
            }
        }

        for (int i = 0; i < firstColumnList2.length; i++) {
            if (i != index2) {
                result.append(table2).append(".").append(firstColumnList2[i]).append("\t");
            }
        }
        result.append("\n");

        for (int i = 1; i < table1Content.size(); i++) {
            String[] row1 = table1Content.get(i).split("\t");

            for (int j = 1; j < table2Content.size(); j++) {
                String[] row2 = table2Content.get(j).split("\t");
                String value1 = row1[index1].trim();
                String value2 = row2[index2].trim();



                boolean match;
                if (value1.matches("\\d+") && value2.matches("\\d+")) {
                    match = Integer.parseInt(value1) == Integer.parseInt(value2);
                } else {
                    match = value1.equalsIgnoreCase(value2);
                }

                if (match) {
                    result.append(row1[0]).append("\t");
                    for (int k = 0; k < row1.length; k++) {
                        if (k != 0 && k != index1) {
                            result.append(row1[k]).append("\t");
                        }
                    }
                    for (int k = 0; k < row2.length; k++) {
                        if (k != index2) {
                            result.append(row2[k]).append("\t");
                        }
                    }
                    result.append("\n");
                }
            }
        }
        return result.toString().trim();
    }
}
