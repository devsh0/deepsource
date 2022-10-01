package mylang;

import mylang.tokeniser.Tokenizer;

import static mylang.Utils.die;

public class ErrorManager {
    private boolean shouldReportError = true;
    private final Tokenizer.State tokenizerState;

    public ErrorManager(Tokenizer.State tokenizerState) {
        this.tokenizerState = tokenizerState;
    }

    public void enableErrorReporting() {
        shouldReportError = true;
    }

    public void disableErrorReporting() {
        shouldReportError = false;
    }

    public void emitFatalError(String errorFmt, Object... args) {
        var error = String.format(errorFmt, args);
        if (shouldReportError) {
            System.err.println(error);
            disableErrorReporting();
        }
        die(error);
    }

    private String buildErrorMessage(String originalError, Object[] args) {
        args = args == null ? new Object[0] : args;
        String errWithLineInfo = originalError + " @(Line=%d, Column=%d)";
        var newSize = args.length + 2;
        var newArgs = new Object[newSize];
        int i = 0;
        for (; i < args.length; i++)
            newArgs[i] = args[i];
        newArgs[i++] = tokenizerState.line();
        newArgs[i] = tokenizerState.lastTokenBegin();
        return String.format(errWithLineInfo, newArgs);
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
}