package edu.uob;

public class NodeQueryCondition extends NodeQuery {
    String column;
    String operator;
    String value;

    public NodeQueryCondition(String column, String operator, String value) {
        this.column = column;
        this.operator = operator;
        this.value = value.startsWith("'") && value.endsWith("'") ? value.substring(1, value.length() - 1) : value;
    }

    @Override
    public boolean evaluate(String[] row, String[] firstColumnList) {
        int columnIndex = findColumnIndex(firstColumnList, column);
        if (columnIndex == -1) return false;

        String rowValue = row[columnIndex].trim();
        return compare(rowValue, operator, value);
    }

    private boolean compare(String rowValue, String operator, String value) {
        boolean isNumeric = rowValue.matches("-?\\d+(\\.\\d+)?") && value.matches("-?\\d+(\\.\\d+)?");

        if (isNumeric) {
            double rowNum = Double.parseDouble(rowValue);
            double condNum = Double.parseDouble(value);

            return switch (operator) {
                case "==" -> rowNum == condNum;
                case "!=" -> rowNum != condNum;
                case ">" -> rowNum > condNum;
                case "<" -> rowNum < condNum;
                case ">=" -> rowNum >= condNum;
                case "<=" -> rowNum <= condNum;
                default -> false;
            };
        } else {
            return switch (operator.toUpperCase()) {
                case "==" -> rowValue.equalsIgnoreCase(value);
                case "!=" -> !rowValue.equalsIgnoreCase(value);
                case "LIKE"-> rowValue.toLowerCase().contains(value.toLowerCase());
                default -> false;
            };
        }
    }

    private int findColumnIndex(String[] firstColumnList, String columnName) {
        for (int i = 0; i < firstColumnList.length; i++) {
            if (firstColumnList[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
