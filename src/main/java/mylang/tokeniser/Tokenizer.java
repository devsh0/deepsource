package mylang.tokeniser;

import mylang.ErrorManager;
import mylang.Signal;

import java.util.List;

import static mylang.Utils.*;

public class Tokenizer {
    private static final List<String> KEYWORDS = List.of("if", "val");

    private final State state;
    private final ErrorManager errorManager;

    public static class State {
        private final List<String> sourceLines;
        private int lineCursor;
        private int columnCursor;
        private int lastTokenBeginIndex;

        State(String source, int lineCursor, int columnCursor, int lastTokenBeginIndex) {
            this.sourceLines = List.of(source.split("\n"));
            this.lineCursor = lineCursor;
            this.columnCursor = columnCursor;
            this.lastTokenBeginIndex = lastTokenBeginIndex;
        }

        // 1-indexed.
        public int lineNumber() {
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

        public boolean atEndOfFile() {
            boolean atLastLine = lineCursor == sourceLines.size() - 1;
            return atLastLine && atEndOfLine();
        }

        private String currentLine() {
            return sourceLines.get(lineCursor);
        }

        private boolean atEndOfLine() {
            return currentLine().length() == columnCursor;
        }

        private boolean isLineEmpty() {
            return sourceLines.get(lineCursor).isEmpty();
        }
    }

    private Tokenizer(String source) {
        if (source == null || source.isEmpty())
            throw new RuntimeException("Invalid input!");
        state = new State(source, 0, 0, 0);
        errorManager = new ErrorManager(state);
    }

    public Signal<Void> advanceLine() {
        if (state.atEndOfFile())
            return Signal.fail("Premature end-of-file!");

        state.lineCursor++;
        state.columnCursor = 0;
        if (state.isLineEmpty())
            return advanceLineIfNecessary();
        return Signal.of(null);
    }

    // Advances the line cursor only if we are at the end of the current line.
    public Signal<Void> advanceLineIfNecessary() {
        if (!state.atEndOfLine())
            return Signal.of(null);
        return advanceLine();
    }

    private Signal<Character> eatChar() {
        var result = advanceLineIfNecessary();
        if (result.failure())
            return Signal.fail(result.message());
        Character nextChar = state.currentLine().charAt(state.columnCursor++);
        return Signal.of(nextChar);
    }

    private Signal<Character> peekChar() {
        int oldLineCursor = state.lineCursor;
        int oldColumnCursor = state.columnCursor;
        var nextChar = eatChar();
        state.lineCursor = oldLineCursor;
        state.columnCursor = oldColumnCursor;
        return nextChar;
    }

    private Signal<Void> eatWhitespaces() {
        var result = peekChar();
        if (result.failure())
            return Signal.fail(result.message());
        char nextChar = result.get();
        if (isWhitespace(nextChar)) {
            eatChar();
            return eatWhitespaces();
        }
        return Signal.of(null);
    }

    public ErrorManager errorManager() {
        return errorManager;
    }

    public Signal<Token> eatAndMatch(Type... candidates) {
        var result = eatToken();
        if (result.failure())
            return Signal.fail(result.message());

        var nextToken = result.get();
        for (var type : candidates) {
            if (type == nextToken.type())
                return Signal.of(nextToken);
        }

        return Signal.fail(ErrorManager.buildExpectedTokenTypeMessage(candidates));
    }

    public Signal<Token> eatAndMatch(String str) {
        var result = eatToken();
        if (result.failure())
            return Signal.fail(result.message());

        var nextToken = result.get();
        if (!str.equals(nextToken.value()))
            return Signal.fail(String.format("Expected `%s`", str));
        return Signal.of(nextToken);
    }

    private void registerLastTokenBeginIndex() {
        state.lastTokenBeginIndex = state.columnCursor;
    }

    private Signal<Token> eatNumberToken() {
        char nextChar = peekChar().get();
        var accumulator = new StringBuilder();
        while (isDigit(nextChar)) {
            eatChar();
            accumulator.append(nextChar);

            if (state.atEndOfFile())
                return Signal.of(new Token(Type.NUMBER, accumulator.toString()));

            var maybeNextChar = peekChar();
            if (maybeNextChar.failure())
                return Signal.fail(maybeNextChar.message());
            nextChar = maybeNextChar.get();
        }
        return Signal.of(new Token(Type.NUMBER, accumulator.toString()));
    }

    private Signal<Token> eatNameOrKeywordToken() {
        char nextChar = peekChar().get();
        var accumulator = new StringBuilder();
        while (isAlpha(nextChar)) {
            eatChar();
            accumulator.append(nextChar);
            var maybeNextChar = peekChar();
            if (maybeNextChar.failure())
                return Signal.fail(maybeNextChar.message());
            nextChar = maybeNextChar.get();
        }

        var value = accumulator.toString();
        var type = KEYWORDS.contains(value) ? Type.KEYWORD : Type.NAME;
        return Signal.of(new Token(type, value));

    }

    private Signal<Token> eatOperatorToken() {
        char nextChar = eatChar().get();
        var accumulator = new StringBuilder();
        accumulator.append(nextChar);
        switch (nextChar) {
            case '(': return Signal.of(new Token(Type.LPAREN, "("));
            case ')': return Signal.of(new Token(Type.RPAREN, ")"));
            case '{': return Signal.of(new Token(Type.LBRACE, "{"));
            case '}': return Signal.of(new Token(Type.RBRACE, "}"));
            case ',': return Signal.of(new Token(Type.OPERATOR, ","));
            case '>':
            case '<':
            case '=':
            case '!': {
                var result = peekChar();
                if (result.failure())
                    return Signal.fail(result.message());
                nextChar = result.get();
                if (nextChar == '=')
                    accumulator.append(eatChar().get());
                return Signal.of(new Token(Type.OPERATOR, accumulator.toString()));
            }
            default:
                return Signal.fail(String.format("Unexpected symbol `%s`", accumulator));
        }

    }

    public Signal<Token> eatToken() {
        // As soon as we have to eat the next token, re-enable error reporting.
        errorManager.enableErrorReporting();
        eatWhitespaces();
        registerLastTokenBeginIndex();

        var maybeNextChar = peekChar();
        if (maybeNextChar.failure())
            return Signal.fail(maybeNextChar.message());

        char nextChar = maybeNextChar.get();
        if (isDigit(nextChar))
            return eatNumberToken();
        if (isAlpha(nextChar))
            return eatNameOrKeywordToken();
        return eatOperatorToken();
    }

    public Signal<Token> peekToken() {
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
            signal = Signal.of(tokenizer);
        } catch (RuntimeException e) {
            signal = Signal.fail(e.getMessage());
        }
        return signal;
    }
}
