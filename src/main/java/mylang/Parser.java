package mylang;

import mylang.ast.*;
import mylang.ast.Number;
import mylang.tokeniser.Token;
import mylang.tokeniser.Tokenizer;
import mylang.tokeniser.Type;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Tokenizer tokenizer;
    private final ErrorManager errorManager;

    public Parser(String source) {
        var maybeTokeniser = Tokenizer.getInstance(source);
        if (maybeTokeniser.failure())
            // FIXME: Temporary...find a better way to handle errors.
            throw new RuntimeException(maybeTokeniser.message());
        tokenizer = maybeTokeniser.get();
        errorManager = tokenizer.errorManager();
    }

    private Signal<Object> tryParseNameOrNumber() {
        var result = tokenizer.eatAndMatch(Type.NAME, Type.NUMBER);
        if (result.failure())
            return Signal.fail(result.message());
        return Signal.of(result.get());
    }

    private Signal<Operator> tryParseOperator() {
        var result = tokenizer.eatAndMatch(Type.OPERATOR);
        if (result.failure())
            return Signal.fail(result.message());
        var operator = new Operator(result.get().value());
        return Signal.of(operator);
    }

    private Signal<ConditionExpression> tryParseConditionExpression() {
        var lhsResult = tryParseNameOrNumber();
        if (lhsResult.failure())
            return Signal.fail(lhsResult.message());

        var opResult = tryParseOperator();
        if (opResult.failure())
            return Signal.fail(opResult.message());

        var operator = opResult.get();
        var opStr = operator.string();
        if (opStr.equals("=") || opStr.equals(","))
            // '=' and ',' are the the only two operators not allowed in conditional expression.
            return Signal.fail(String.format("Unexpected operator `%s`", opStr));

        var rhsResult = tryParseNameOrNumber();
        if (rhsResult.failure())
            return Signal.fail(rhsResult.message());

        var conditionExpr = new ConditionExpression(lhsResult.get(), rhsResult.get(), operator);
        return Signal.of(conditionExpr);
    }

    private Signal<Statement> tryParseIfStatement() {
        tokenizer.eatToken(); // "if"

        var condResult = tryParseConditionExpression();
        if (condResult.failure())
            return Signal.fail(condResult.message());

        var lbraceResult = tokenizer.eatAndMatch(Type.LBRACE);
        if (lbraceResult.failure())
            return Signal.fail(lbraceResult.message());

        Signal<Token> nextTokenResult = tokenizer.peekToken();
        if (nextTokenResult.failure())
            return Signal.fail(nextTokenResult.message());

        List<Statement> statementList = new ArrayList<>();
        while (nextTokenResult.get().type() != Type.RBRACE) {
            var nextStmtResult = tryParseNextStatement();
            if (nextStmtResult.failure()) {
                if (!errorManager.emitSyntaxError(nextStmtResult.message()))
                    // Cannot recover from this error.
                    return Signal.fail(nextStmtResult.message());

                // Advance the line in the hope of seeing a new statement on the next line. Ideally, we would
                // skip chars until we see one that marks the beginning of a new statement. But for now, this will do.
                var advLineResult = tokenizer.advanceLine();
                if (advLineResult.failure()) {
                    // Cannot recover from this error.
                    errorManager.emitFatalError(advLineResult.message());
                    return Signal.fail(advLineResult.message());
                }
            } else {
                statementList.add(nextStmtResult.get());
            }

            nextTokenResult = tokenizer.peekToken();
            if (nextTokenResult.failure()) {
                // Cannot recover from this error.
                errorManager.emitFatalError(nextTokenResult.message());
                return Signal.fail(nextTokenResult.message());
            }
        }

        tokenizer.eatToken(); // "}"
        return Signal.of(new IfStatement(condResult.get(), statementList));
    }

    private Signal<Statement> tryParseDeclarationStatement() {
        tokenizer.eatToken(); // "val"

        var nameResult = tokenizer.eatAndMatch(Type.NAME);
        if (nameResult.failure())
            return Signal.fail(nameResult.message());

        var opResult = tokenizer.eatAndMatch("=");
        if (opResult.failure())
            return Signal.fail(opResult.message());

        var numberResult = tokenizer.eatAndMatch(Type.NUMBER);
        if (numberResult.failure())
            return Signal.fail(numberResult.message());

        var name = new Name(nameResult.get().value());
        var number = new Number(Integer.valueOf(numberResult.get().value()));
        return Signal.of(new DeclarationStatement(name, number));
    }

    private Signal<List<Object>> tryParseArgumentList() {
        var list = new ArrayList<>();

        var lparenResult = tokenizer.eatAndMatch(Type.LPAREN);
        if (lparenResult.failure())
            return Signal.fail(lparenResult.message());

        var nextTokenResult = tokenizer.peekToken();
        if (nextTokenResult.failure())
            return Signal.fail(nextTokenResult.message());

        var nextTokenType = nextTokenResult.get().type();
        if (nextTokenType != Type.RPAREN && nextTokenType != Type.NAME && nextTokenType != Type.NUMBER)
            return Signal.fail("Expected a name, number, or `)`");

        while (nextTokenResult.get().type() != Type.RPAREN) {
            var nameOrNumberResult = tryParseNameOrNumber();
            if (nameOrNumberResult.failure())
                return Signal.fail(nameOrNumberResult.message());

            var maybeComma = tokenizer.peekToken();
            if (maybeComma.failure())
                return Signal.fail(maybeComma.message());

            var nextToken = maybeComma.get();
            if (nextToken.value().equals(",")) {
                tokenizer.eatToken();
                list.add(nameOrNumberResult.get());
                nameOrNumberResult = tryParseNameOrNumber();
                if (nameOrNumberResult.failure())
                    return Signal.fail(nameOrNumberResult.message());
            }

            list.add(nameOrNumberResult.get());
            nextTokenResult = tokenizer.peekToken();
            if (nextTokenResult.failure())
                return Signal.fail(nextTokenResult.message());
        }

        tokenizer.eatToken(); // ")"
        return Signal.of(list);
    }

    private Signal<Statement> tryParseFunctionCallStatement() {
        var nameResult = tokenizer.eatAndMatch(Type.NAME);
        if (nameResult.failure())
            return Signal.fail(nameResult.message());

        var argListResult = tryParseArgumentList();
        if (argListResult.failure())
            return Signal.fail(argListResult.message());

        var name = new Name(nameResult.get().value());
        var arguments = argListResult.get();
        return Signal.of(new FunctionCallStatement(name, arguments));
    }

    private Signal<Statement> tryParseNextStatement() {
        if (!errorManager.canRecover())
            return Signal.fail("");

        var nextTokenResult = tokenizer.peekToken();
        if (nextTokenResult.failure())
            return Signal.fail(nextTokenResult.message());

        var nextToken = nextTokenResult.get();
        if (nextToken.type() == Type.NAME)
            return tryParseFunctionCallStatement();

        var tokenVal = nextToken.value();
        return switch (tokenVal) {
            case "if" -> tryParseIfStatement();
            case "val" -> tryParseDeclarationStatement();
            default -> {
                errorManager.emitSyntaxError("Unexpected token `%s`", tokenVal);
                yield Signal.fail("");
            }
        };
    }

    public CompilationResult parse() {
        // As per the grammar, only one top-level statement is allowed per program.
        var stmtResult = tryParseNextStatement();
        if (stmtResult.failure()) {
            var message = stmtResult.message();
            errorManager.emitFatalError(message);
            return new CompilationResult(null, errorManager.problems());
        }

        return new CompilationResult(stmtResult.get(), errorManager.problems());
    }
}
