package edu.uob;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class QueryTokeniser {
    public String query;
    public String[] specialCharacters = {"(", ")", ",", ";"};
    public ArrayList<String> tokens = new ArrayList<>();

    public QueryTokeniser(String query) {
        this.query = query;
        setup();
    }

    private String[] tokenise(String input) {
        input = handleLessThanEquals(input);
        input = handleGreaterThanEquals(input);
        input = handleStandaloneLessThan(input);
        input = handleStandaloneGreaterThan(input);
        input = input.replaceAll("(?<![\\w])((==|!=))(?![\\w])", " $1 ");

        for (String specialCharacter : specialCharacters) {
            input = input.replace(specialCharacter, " " + specialCharacter + " ");
        }

        input = handleSingleEquals(input);
        while (input.contains("  ")) input = input.replace("  ", " ");
        input = input.trim();
        return input.split(" ");
    }

    private String handleSingleEquals(String input) {
        return input.replaceAll("(?<![=!<>])=(?![=<>])", " = ");
    }

    private String handleLessThanEquals(String input) {
        return input.replaceAll("(?<!<)<=", " <= ");
    }

    private String handleGreaterThanEquals(String input) {
        return input.replaceAll("(?<!>)>=", " >= ");
    }

    private String handleStandaloneLessThan(String input) {
        return input.replaceAll("(?<![=<>])<(?![=<>])", " < ");
    }

    private String handleStandaloneGreaterThan(String input) {
        return input.replaceAll("(?<![=<>])>(?![=<>])", " > ");
    }

    private void setup() {
        String[] fragments = query.split("'");

        for (int i = 0; i < fragments.length; i++) {
            if (i % 2 != 0) {
                tokens.add("'" + fragments[i] + "'");
            } else {
                String[] nextBatchOfTokens = tokenise(fragments[i]);
                tokens.addAll(Arrays.asList(nextBatchOfTokens));
            }
        }
    }

    public List<String> getTokens() {
        return new ArrayList<>(tokens);
    }
}
