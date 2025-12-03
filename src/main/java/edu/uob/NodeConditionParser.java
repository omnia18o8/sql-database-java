package edu.uob;

import java.util.*;

public class NodeConditionParser {
    private final List<String> tokens;
    private int index;

    public NodeConditionParser(List<String> tokens) {
        this.tokens = tokens;
        this.index = 0;
    }

    public NodeQuery parseConditions() {
        return parseOrExpression();
    }

    private NodeQuery parseOrExpression() {
        NodeQuery left = parseAndExpression();

        while (index < tokens.size() && tokens.get(index).equalsIgnoreCase("OR")) {
            index++;
            NodeQuery right = parseAndExpression();
            left = new NodeQueryLogical("OR", left, right);
        }
        return left;
    }

    private NodeQuery parseAndExpression() {
        NodeQuery left = parseCondition();

        while (index < tokens.size() && tokens.get(index).equalsIgnoreCase("AND")) {
            index++;
            NodeQuery right = parseCondition();
            left = new NodeQueryLogical("AND", left, right);
        }

        return left;
    }

    private NodeQuery parseCondition() {
        if (tokens.get(index).equals("(")) {
            index++;
            NodeQuery condition = parseOrExpression();
            index++;
            return condition;
        }

        // Get column, operator, and value
        String column = tokens.get(index++);
        String operator = tokens.get(index++);
        String value = tokens.get(index++);

        return new NodeQueryCondition(column, operator, value);
    }
}
