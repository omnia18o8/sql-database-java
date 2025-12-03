package edu.uob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class QueryParser {
    private int index = 0;
    private final List<String> values = new ArrayList<>();

    private final List<TokenType> tokenTypes;
    private final List<String> tokens;
    private List<String> conditionTokens;
    private String alterType;

    private String tableName;
    private String databaseName;
    private List<String> columnNames = new ArrayList<>();
    private String table1;
    private String table2;
    private String column1;
    private String column2;

    public QueryParser(String query) {
        QueryTokeniser tokeniser = new QueryTokeniser(query);
        List<String> rawTokens = tokeniser.getTokens();

        QueryLexer lexer = new QueryLexer(rawTokens);
        this.tokenTypes = lexer.getTokenTypes();
        this.tokens = rawTokens;
        lexer.tokenize();
    }

    //-------------  GENERAL SHARED FUNCTIONS ---------------------
    private boolean matchTokens(TokenType expectedType, String... expectedValues) {
        if (index < tokenTypes.size() && tokenTypes.get(index) == expectedType) {
            String value = tokens.get(index);
            for (String expectedValue : expectedValues) {
                if (value.equalsIgnoreCase(expectedValue)) {
                    index++;
                    return true;
                }
            }
        }
        return false;
    }

    private String expect(TokenType... expectedTypes) throws IOException {
        TokenType token = tokenTypes.get(index);
        String value = tokens.get(index++);

        if (value.equals("*")) {return value;}
        if ("TRUE".equalsIgnoreCase(value) || "FALSE".equalsIgnoreCase(value)) {return value.toUpperCase();}

        for (TokenType expected : expectedTypes) {
            if (token == expected) {
                if (expected == TokenType.IDENTIFIER && !value.matches("[a-zA-Z0-9_]+")) {
                    throw new IOException("[ERROR] Invalid name format: " + value);
                }
                return value;
            }
        }
        throw new IOException("[ERROR] Unexpected token: " + value);
    }


    private void expectValue(TokenType expectedType, String... expectedValues) throws IOException {
        TokenType token = tokenTypes.get(index);
        String value = tokens.get(index++);
        if (token != expectedType) {
            throw new IOException("[ERROR] Expected " + expectedType + " but found " + token);
        }
        for (String expectedValue : expectedValues) {
            if (value.equalsIgnoreCase(expectedValue)) {
                return;
            }
        }
        throw new IOException("[ERROR] Unexpected value: " + value);
    }


    private void parseWhereConditions() throws IOException {
        conditionTokens = new ArrayList<>();
        Stack<String> parenthesesStack = new Stack<>();
        label:
        while (index < tokens.size()) {
            String token = tokens.get(index);

            switch (token) {
                case ";":
                    break label;
                case "(":
                    parenthesesStack.push(token);
                    break;
                case ")":
                    if (parenthesesStack.isEmpty()) {
                        throw new IOException("[ERROR] Unmatched closing parenthesis.");
                    }
                    parenthesesStack.pop();
                    break;
            }
            conditionTokens.add(token);
            index++;
        }
        if (!parenthesesStack.isEmpty()) {
            throw new IOException("[ERROR] Unmatched opening parenthesis.");
        }
    }

        private void validateEndSemicolon() throws IOException {
            if (index >= tokens.size() || !tokens.get(index).equals(";")) {
                throw new IOException("[ERROR] Missing semicolon at the end.");
            }
            index++;
        }

    // ------------------- GETTERS AND BOOLEANS ---------------------
    public String getDatabaseName() {return databaseName;}
    public String getTableName() {return tableName;}
    public List<String> getColumnNames() {return columnNames;}
    public List<String> getValues() {return values;}
    public String getAlterType() {return alterType;}
    public String getTable1() { return table1; }
    public String getTable2() { return table2; }
    public String getColumn1() { return column1; }
    public String getColumn2() { return column2; }
    public List<String> getConditionTokens() {return conditionTokens;}

    public boolean isDropTable() {return tableName != null;}
    public boolean hasWhereClause() {return conditionTokens != null && !conditionTokens.isEmpty();}

    // ------------------- PARSE USE ---------------------
    public String parseUse() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "USE")) {
            throw new IOException("Expected USE.");
        }
        databaseName = expect(TokenType.IDENTIFIER);
        validateEndSemicolon();
        return databaseName;
    }

    // ------------------- PARSE CREATE TABLE ---------------------
    public void parseCreateTable() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "CREATE")) throw new IOException("[ERROR] Expected CREATE.");
        if (!matchTokens(TokenType.KEYWORD, "TABLE")) throw new IOException("[ERROR] Expected TABLE.");

        tableName = expect(TokenType.IDENTIFIER);
        columnNames.clear();


        if (matchTokens(TokenType.SYMBOL, "(")) {
            do {
                String column = expect(TokenType.IDENTIFIER);
                if (columnNames.contains(column)) {
                    throw new IOException("[ERROR] Duplicate column name: " + column);
                }
                if (!columnNames.contains(TokenType.KEYWORD)) {
                    columnNames.add(column);
                } else {
                    throw new IOException("[ERROR] Cannot use keywords column name: " + column);
                }
            } while (matchTokens(TokenType.SYMBOL, ","));

            expectValue(TokenType.SYMBOL, ")");
        }

        validateEndSemicolon();
    }

    // ------------------- PARSE CREATE DATABASE ---------------------
    public String parseCreateDB() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "CREATE")) throw new IOException("[ERROR] Expected CREATE.");
        if (!matchTokens(TokenType.KEYWORD, "DATABASE")) throw new IOException("[ERROR] Expected DATABASE.");
        databaseName = expect(TokenType.IDENTIFIER);
        validateEndSemicolon();
        return databaseName;
    }

    // ------------------- PARSE INSERT ---------------------
    public void parseInsert() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "INSERT")) throw new IOException("[ERROR] Expected INSERT.");
        if (!matchTokens(TokenType.KEYWORD, "INTO")) throw new IOException("[ERROR] Expected INTO.");

        tableName = expect(TokenType.IDENTIFIER);
        if (!matchTokens(TokenType.KEYWORD, "VALUES")) throw new IOException("[ERROR] Expected VALUES.");

        expectValue(TokenType.SYMBOL, "(");
        values.clear();
        do {
            values.add(expect(TokenType.STRING, TokenType.NUMBER, TokenType.IDENTIFIER));

        } while (matchTokens(TokenType.SYMBOL, ","));

        expectValue(TokenType.SYMBOL, ")");
        validateEndSemicolon();
    }

    // ------------------- PARSE ALTER ---------------------
    public void parseAlter() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "ALTER")) throw new IOException("[ERROR] Expected ALTER.");
        if (!matchTokens(TokenType.KEYWORD, "TABLE")) throw new IOException("[ERROR] Expected TABLE.");

        tableName = expect(TokenType.IDENTIFIER);

        if (matchTokens(TokenType.KEYWORD, "ADD")) {
            alterType = "ADD";
        } else if (matchTokens(TokenType.KEYWORD, "DROP")) {
            alterType = "DROP";
        } else {
            throw new IOException("[ERROR] Expected ADD or DROP in ALTER TABLE.");
        }
        columnNames.clear();
        columnNames.add(expect(TokenType.IDENTIFIER));
        validateEndSemicolon();
    }


    // ------------------- PARSE DROP ---------------------
    public void parseDrop() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "DROP")) throw new IOException("[ERROR] Expected DROP.");
        String dropType = expect(TokenType.KEYWORD);
        if (!dropType.equalsIgnoreCase("TABLE") && !dropType.equalsIgnoreCase("DATABASE")) {
            throw new IOException("[ERROR] Expected TABLE or DATABASE in DROP command.");
        }

        if (dropType.equalsIgnoreCase("TABLE")) {
            tableName = expect(TokenType.IDENTIFIER);
        } else {
            databaseName = expect(TokenType.IDENTIFIER);
        }
        validateEndSemicolon();
    }

    // ------------------- PARSE JOIN ---------------------
    public void parseJoin() throws IOException {
        if (!expect(TokenType.KEYWORD).equalsIgnoreCase("JOIN")) {
            throw new IOException("[ERROR] Expected 'JOIN'");
        }
        this.table1 = expect(TokenType.IDENTIFIER);
        if (!expect(TokenType.LOGICAL_OPERATOR).equalsIgnoreCase("AND")) {
            throw new IOException("[ERROR] Expected 'AND' between table names");
        }
        this.table2 = expect(TokenType.IDENTIFIER);
        if (!expect(TokenType.KEYWORD).equalsIgnoreCase("ON")) {
            throw new IOException("[ERROR] Expected 'ON' before column names");
        }
        this.column1 = expect(TokenType.IDENTIFIER);
        if (!expect(TokenType.LOGICAL_OPERATOR).equalsIgnoreCase("AND")) {
            throw new IOException("[ERROR] Expected 'AND' between column names");
        }
        this.column2 = expect(TokenType.IDENTIFIER);
        validateEndSemicolon();
    }


    // ------------------- PARSE SELECT ---------------------
    public void parseSelect() throws IOException {
        if (!expect(TokenType.KEYWORD).equalsIgnoreCase("SELECT")) {
            throw new IOException("[ERROR] Expected 'SELECT'");
        }
        if (matchTokens(TokenType.SYMBOL, "*")) {
            columnNames = List.of("*");
            expectValue(TokenType.KEYWORD, "FROM");
        } else {
            parseColumnList();
            expectValue(TokenType.KEYWORD, "FROM");
        }
        tableName = expect(TokenType.IDENTIFIER);
        if (matchTokens(TokenType.KEYWORD, "WHERE")) {
            parseWhereConditions();
        }
        validateEndSemicolon();
    }
    private void parseColumnList() throws IOException {
        columnNames.clear();
        do {
            columnNames.add(expect(TokenType.IDENTIFIER));
        } while (matchTokens(TokenType.SYMBOL, ","));
    }

    // ------------------- PARSE UPDATE ---------------------
    public void parseUpdate() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "UPDATE")) {
            throw new IOException("[ERROR] Expected UPDATE.");
        }
        tableName = expect(TokenType.IDENTIFIER);
        if (tableName.equals("id")) {
            throw new IOException("[ERROR] Cannot update ID column.");
        }
        if (!matchTokens(TokenType.KEYWORD, "SET")) {
            throw new IOException("[ERROR] Expected SET.");
        }
        columnNames.clear();
        boolean moreColumns = true;
        while (moreColumns) {
            columnNames.add(expect(TokenType.IDENTIFIER));
            expectValue(TokenType.SYMBOL, "=");
            values.add(expect(TokenType.NUMBER, TokenType.STRING, TokenType.IDENTIFIER));

            moreColumns = matchTokens(TokenType.SYMBOL, ",");
        }
        if (matchTokens(TokenType.KEYWORD, "WHERE")) {
            parseWhereConditions();
        }
        validateEndSemicolon();
    }

    // ------------------- PARSE DELETE ---------------------
    public void parseDelete() throws IOException {
        if (!matchTokens(TokenType.KEYWORD, "DELETE")) {
            throw new IOException("[ERROR] Expected DELETE.");
        }
        if (!matchTokens(TokenType.KEYWORD, "FROM")) {
            throw new IOException("[ERROR] Expected FROM.");
        }
        tableName = expect(TokenType.IDENTIFIER);
        boolean hasWhereClause = matchTokens(TokenType.KEYWORD, "WHERE");
        if (hasWhereClause) {
            parseWhereConditions();
        }
        validateEndSemicolon();
    }
}
