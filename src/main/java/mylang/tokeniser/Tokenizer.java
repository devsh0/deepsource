package mylang.tokeniser;

import mylang.ErrorManager;
import mylang.Signal;

import java.util.List;
import java.util.Optional;

import static mylang.Utils.*;

public class Tokenizer {
    private static final List<String> KEYWORDS = List.of("if", "val");

    private final List<String> sourceLines;
    private final State state;
    private final ErrorManager errorManager;

    public static class State {
        private int lineCursor;
        private int columnCursor;
        private int lastTokenBeginIndex;

        State(int lineCursor, int columnCursor, int lastTokenBeginIndex) {
            this.lineCursor = lineCursor;
            this.columnCursor = columnCursor;
            this.lastTokenBeginIndex = lastTokenBeginIndex;
        }

        // 1-indexed.
        public int line() {
            return lineCursor + 1;
        }

        // 1-indexed.
        public int column() {
            return columnCursor + 1;
        }

        // 1-indexed.
        public int lastTokenBegin() {
            return lastTokenBeginIndex + 1;
        }
    }

    private Tokenizer(String source) {
        if (source == null)
            throw new RuntimeException("Invalid input!");
        sourceLines = List.of(source.split("\n"));
        state = new State(0, 0, 0);
        errorManager = new ErrorManager(state);
    }

    public boolean atEndOfFile() {
        boolean atLastLine = state.lineCursor == sourceLines.size() - 1;
        return atLastLine && atEndOfLine();
    }

    private boolean atEndOfLine() {
        return currentLine().length() == state.columnCursor;
    }

    private boolean isLineEmpty() {
        return sourceLines.get(state.lineCursor).isEmpty();
    }

    public void advanceLine() {
        if (atEndOfFile())
            errorManager.emitFatalError("Premature end-of-file!");

        state.lineCursor++;
        state.columnCursor = 0;

        if (isLineEmpty())
            advanceLine();
    }

    private String currentLine() {
        return sourceLines.get(state.lineCursor);
    }

    private char eatChar() {
        if (atEndOfFile())
            errorManager.emitFatalError("Premature end-of-file!");

        if (atEndOfLine()) {
            advanceLine();
            if (atEndOfFile())
                errorManager.emitFatalError("Premature end-of-file");
        }

        var oldCursor = state.columnCursor;
        state.columnCursor++;
        return currentLine().charAt(oldCursor);
    }

    private char peekChar() {
        int oldLineCursor = state.lineCursor;
        int oldColumnCursor = state.columnCursor;
        var nextChar = eatChar();
        state.lineCursor = oldLineCursor;
        state.columnCursor = oldColumnCursor;
        return nextChar;
    }

    private void eatWhitespaces() {
        while (isWhitespace(peekChar()))
            eatChar();
    }

    public ErrorManager errorManager() {
        return errorManager;
    }

    public Optional<Token> eatAndMatch(Type... candidates) {
        var nextToken = eatToken();
        for (var type : candidates) {
            if (type == nextToken.type())
                return Optional.of(nextToken);
        }

        var builder = new StringBuilder("Expected token of type ");
        int i = 0;
        for (; i < (candidates.length - 1); i++) {
            builder.append("`");
            builder.append(candidates[i].toString());
            builder.append("` or ");
        }

        builder.append("`");
        builder.append(candidates[i].toString());
        builder.append("`");

        errorManager.emitSyntaxError(builder.toString());
        return Optional.empty();
    }

    public Optional<Token> eatAndMatch(String str) {
        var nextToken = eatToken();
        if (!nextToken.value().equals(str)) {
            errorManager.emitSyntaxError("Expected `%s`", str);
            return Optional.empty();
        }
        return Optional.of(nextToken);
    }


    private void registerLastTokenBeginIndex() {
        state.lastTokenBeginIndex = state.columnCursor;
    }

    public Token eatToken() {
        // As soon as we have to eat the next token, re-enable error reporting.
        errorManager.enableErrorReporting();
        StringBuilder accumulator = new StringBuilder();
        eatWhitespaces();
        registerLastTokenBeginIndex();

        var nextChar = eatChar();
        accumulator.append(nextChar);

        if (isDigit(nextChar)) {
            while (!atEndOfFile() && isDigit(peekChar()))
                accumulator.append(eatChar());
            return new Token(Type.NUMBER, accumulator.toString());
        }

        if (isAlpha(nextChar)) {
            while (isAlpha(peekChar()))
                accumulator.append(eatChar());

            var string = accumulator.toString();
            var tokenType = KEYWORDS.contains(string) ? Type.KEYWORD : Type.NAME;
            return new Token(tokenType, string);
        }

        switch (accumulator.toString()) {
            case "(": return new Token(Type.LPAREN, "(");
            case ")": return new Token(Type.RPAREN, ")");
            case "{": return new Token(Type.LBRACE, "{");
            case "}": return new Token(Type.RBRACE, "}");
            case ">":
            case "<":
            case "=":
            case ",":
            case "!": {
                if (peekChar() == '=')
                    accumulator.append(eatChar());
                return new Token(Type.OPERATOR, accumulator.toString());
            }
            default:
                return new Token(Type.ERROR, accumulator.toString());
        }
    }

    public Token peekToken() {
        int oldLineCursor = state.lineCursor;
        int oldColumnCursor = state.columnCursor;
        var nextToken = eatToken();
        state.lineCursor = oldLineCursor;
        state.columnCursor = oldColumnCursor;
        return nextToken;
    }

    public static Signal<Tokenizer> getInstance(String source) {
        Signal<Tokenizer> signal;
        try {
            var tokenizer = new Tokenizer(source);
            signal = Signal.successInstance(tokenizer);
        } catch (RuntimeException e) {
            signal = Signal.failureInstance(e.getMessage());
        }
        return signal;
    }

}
