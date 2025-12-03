package edu.uob;

public class NodeQueryLogical extends NodeQuery {
    String logicalOperator;
    NodeQuery left;
    NodeQuery right;

    public NodeQueryLogical(String logicalOperator, NodeQuery left, NodeQuery right) {
        this.logicalOperator = logicalOperator;
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(String[] row, String[] firstColumnList) {
        boolean leftResult = left.evaluate(row, firstColumnList);
        boolean rightResult = right.evaluate(row, firstColumnList);

        if (logicalOperator.equalsIgnoreCase("AND")) {
            return leftResult && rightResult;
        } else if (logicalOperator.equalsIgnoreCase("OR")) {
            return leftResult || rightResult;
        }
        return false;
    }
}
