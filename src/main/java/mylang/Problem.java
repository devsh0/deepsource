package mylang;

import mylang.tokeniser.Tokenizer;

public class Problem {
    private final int lineNumber;
    private final int column;
    private final String line;
    private final String description;
    private String decorated;

    public Problem(Tokenizer.State state, String description) {
        if (state == null || description == null || description.isEmpty())
            throw new RuntimeException("Invalid state");
        this.lineNumber = state.lineNumber();
        this.column = state.lastTokenBegin();
        this.line = state.currentLine();
        this.description = description;
        this.decorated = description;
        decorate();
    }

    private String generatePrettyError() {
        var builder = new StringBuilder("\t").append(line).append("\n");
        builder.append("\t");
        builder.append(" ".repeat(Math.max(0, column - 1)));
        builder.append("^~~~ here\n");
        return builder.toString();
    }

    private void decorate() {
        var builder = new StringBuilder(description);
        builder.append(" @(Line=").append(lineNumber).append(", Column=").append(column).append(")\n");
        if (column <= line.length()) {
            var prettyError = generatePrettyError();
            builder.append(prettyError);
        }
        decorated = builder.toString();
    }

    public String description() {
        return description;
    }

    public int line() {
        return lineNumber;
    }

    public int column() {
        return column;
    }

    public String prettyError() {
        return decorated;
    }
}
