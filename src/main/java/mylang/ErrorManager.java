package mylang;

import mylang.tokeniser.Tokenizer;
import mylang.tokeniser.Type;

import java.util.ArrayList;
import java.util.List;

import static mylang.Utils.die;

public class ErrorManager {
    private boolean shouldReportError = true;
    private final Tokenizer.State tokenizerState;
    private final List<Problem> problems = new ArrayList<>();

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
            var problem = new Problem(tokenizerState, error);
            problems.add(problem);
            disableErrorReporting();
        }
        die(error);
    }

    public void emitSyntaxError(String errorFmt, Object... args) {
        if (shouldReportError) {
            var problem = new Problem(tokenizerState, String.format(errorFmt, args));
            problems.add(problem);
            disableErrorReporting();
        }
    }

    public List<Problem> problems() {
        return List.copyOf(problems);
    }

    public static String buildExpectedTokenTypeMessage(Type... candidates) {
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
        return builder.toString();
    }
}
