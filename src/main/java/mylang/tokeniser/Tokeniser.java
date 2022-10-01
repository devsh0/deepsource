package mylang.tokeniser;

import java.util.List;
import java.util.Optional;

import static mylang.Utils.*;

// FIXME: We should really decouple error reporting and tokenisation.
public class Tokeniser {
    private static final List<String> KEYWORDS = List.of("if", "val");

    private final List<String> sourceLines;
    private int lineCursor;
    private int columnCursor;
    private int lastTokenBeginIndex;
    private boolean shouldReportError = true;

    public Tokeniser(String source) {
        if (source == null) {
            emitFatalError("Invalid input!");
            sourceLines = null;
            return;
        }
        sourceLines = List.of(source.split("\n"));
        lineCursor = 0;
        columnCursor = lastTokenBeginIndex = 0;
    }

    public boolean atEndOfFile() {
        boolean atLastLine = lineCursor == sourceLines.size() - 1;
        return atLastLine && atEndOfLine();
    }

    private void enableErrorReporting() {
        shouldReportError = true;
    }

    private void disableErrorReporting() {
        shouldReportError = false;
    }

    private boolean atEndOfLine() {
        return currentLine().length() == columnCursor;
    }

    private String buildErrorMessage(String originalError, Object[] args) {
        args = args == null ? new Object[0] : args;
        String errWithLineInfo = originalError + " @(Line=%d, Column=%d)";
        var newSize = args.length + 2;
        var newArgs = new Object[newSize];
        int i = 0;
        for (; i < args.length; i++)
            newArgs[i] = args[i];
        newArgs[i++] = line() + 1;
        newArgs[i] = lastTokenStart() + 1;
        return String.format(errWithLineInfo, newArgs);
    }

    public void emitFatalError(String errorFmt, Object... args) {
        var error = String.format(errorFmt, args);
        if (shouldReportError) {
            System.err.println(error);
            disableErrorReporting();
        }
        die(error);
    }

    public void emitSyntaxError(String errorFmt, Object... args) {
        if (shouldReportError) {
            String error = buildErrorMessage(errorFmt, args);
            System.err.println(error);
            // A single error may be reported twice because, say, when we fail parsing the outermost if statement, we
            // emit a syntax error immediately. Then if this `if` statement happens to be the last statement then
            // the parser will again try to emit a fatal error because parsing the only statement has failed.
            // By maintaining a flag we make sure that we don't report two errors for a single issue. None of this would
            // happen if we'd decouple error reporting and parsing/tokenization.
            disableErrorReporting();
        }
    }

    private boolean isLineEmpty() {
        return sourceLines.get(lineCursor).isEmpty();
    }

    public void advanceLine() {
        if (atEndOfFile())
            emitFatalError("Premature end-of-file!");

        lineCursor++;
        columnCursor = 0;

        if (isLineEmpty())
            advanceLine();
    }

    private String currentLine() {
        return sourceLines.get(lineCursor);
    }

    private char eatChar() {
        if (atEndOfFile())
            emitFatalError("Premature end-of-file!");

        if (atEndOfLine()) {
            advanceLine();
            if (atEndOfFile())
                emitFatalError("Premature end-of-file");
        }

        var oldCursor = columnCursor;
        columnCursor += 1;
        return currentLine().charAt(oldCursor);
    }

    private char peekChar() {
        int oldLineCursor = lineCursor;
        int oldColumnCursor = columnCursor;
        var nextChar = eatChar();
        lineCursor = oldLineCursor;
        columnCursor = oldColumnCursor;
        return nextChar;
    }

    private void eatWhitespaces() {
        while (isWhitespace(peekChar()))
            eatChar();
    }

    private int line() {
        return lineCursor;
    }

    private int column() {
        return columnCursor;
    }

    private int lastTokenStart() {
        return lastTokenBeginIndex;
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

        emitSyntaxError(builder.toString());
        return Optional.empty();
    }

    public Optional<Token> eatAndMatch(String str) {
        var nextToken = eatToken();
        if (!nextToken.value().equals(str)) {
            emitSyntaxError("Expected `%s`", str);
            return Optional.empty();
        }
        return Optional.of(nextToken);
    }


    private void registerLastTokenBeginIndex() {
        lastTokenBeginIndex = columnCursor;
    }

    public Token eatToken() {
        // As soon as we have to eat the next token, re-enable error reporting.
        enableErrorReporting();
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
        int oldLineCursor = lineCursor;
        int oldColumnCursor = columnCursor;
        var nextToken = eatToken();
        lineCursor = oldLineCursor;
        columnCursor = oldColumnCursor;
        return nextToken;
    }
}
