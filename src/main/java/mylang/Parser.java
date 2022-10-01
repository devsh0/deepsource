package mylang;

import mylang.ast.*;
import mylang.ast.Number;
import mylang.tokeniser.Tokenizer;
import mylang.tokeniser.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static mylang.Utils.die;

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

    private Optional<Object> maybeParseNameOrNumber() {
        var maybeNextToken = tokenizer.eatAndMatch(Type.NAME, Type.NUMBER);
        return maybeNextToken.isEmpty() ? Optional.empty() : Optional.of(maybeNextToken.get());
    }

    private Optional<Operator> maybeParseOperator() {
        var maybeOperator = tokenizer.eatAndMatch(Type.OPERATOR);
        return maybeOperator.isEmpty() ? Optional.empty() : Optional.of(new Operator(maybeOperator.get().value()));
    }

    private Optional<ConditionExpression> maybeParseConditionExpression() {
        var maybeLhs = maybeParseNameOrNumber();
        if (maybeLhs.isEmpty())
            return Optional.empty();

        var maybeOp = maybeParseOperator();
        if (maybeOp.isEmpty())
            return Optional.empty();

        var operator = maybeOp.get();
        if (operator.string().equals("=")) {
            // '=' is the only operator not allowed in conditional expression.
            errorManager.emitSyntaxError("Unexpected token `=`");
            return Optional.empty();
        }

        var maybeRhs = maybeParseNameOrNumber();
        if (maybeRhs.isEmpty())
            return Optional.empty();

        return Optional.of(new ConditionExpression(maybeLhs.get(), maybeRhs.get(), maybeOp.get()));
    }

    private Optional<Statement> maybeParseIfStatement() {
        tokenizer.eatToken(); // "if"

        var maybeCondExpr = maybeParseConditionExpression();
        if (maybeCondExpr.isEmpty())
            return Optional.empty();

        if (tokenizer.eatAndMatch(Type.LBRACE).isEmpty())
            return Optional.empty();

        List<Statement> statementList = new ArrayList<>();
        outer:
        while (tokenizer.peekToken().type() != Type.RBRACE) {
            var maybeNextStmt = maybeParseNextStatement();
            while (maybeNextStmt.isEmpty()) {
                tokenizer.advanceLine();
                continue outer;
            }
            statementList.add(maybeNextStmt.get());
        }

        tokenizer.eatToken(); // "}"
        return Optional.of(new IfStatement(maybeCondExpr.get(), statementList));
    }

    private Optional<Statement> maybeParseDeclarationStatement() {
        tokenizer.eatToken(); // "val"

        var maybeName = tokenizer.eatAndMatch(Type.NAME);
        if (maybeName.isEmpty())
            return Optional.empty();

        var maybeAssignOp = tokenizer.eatAndMatch("=");
        if (maybeAssignOp.isEmpty())
            return Optional.empty();

        var maybeNumber = tokenizer.eatAndMatch(Type.NUMBER);
        if (maybeNumber.isEmpty())
            return Optional.empty();

        var name = new Name(maybeName.get().value());
        var number = new Number(Integer.valueOf(maybeNumber.get().value()));
        return Optional.of(new DeclarationStatement(name, number));
    }

    private Optional<List<Object>> maybeParseArgumentList() {
        var list = new ArrayList<>();
        if (tokenizer.eatAndMatch(Type.LPAREN).isEmpty())
            return Optional.empty();

        while (tokenizer.peekToken().type() != Type.RPAREN) {
            var maybeNameOrNumber = maybeParseNameOrNumber();
            if (maybeNameOrNumber.isEmpty())
                return Optional.empty();

            var nextToken = tokenizer.peekToken();
            if (nextToken.value().equals(",")) {
                tokenizer.eatToken();
                list.add(maybeNameOrNumber.get());
                maybeNameOrNumber = maybeParseNameOrNumber();
                if (maybeNameOrNumber.isEmpty())
                    return Optional.empty();
            }
            list.add(maybeNameOrNumber.get());
        }

        tokenizer.eatToken(); // ")"
        return Optional.of(list);
    }

    private Optional<Statement> maybeParseFunctionCallStatement() {
        var maybeName = tokenizer.eatAndMatch(Type.NAME);
        if (maybeName.isEmpty())
            return Optional.empty();

        var maybeArgList = maybeParseArgumentList();
        if (maybeArgList.isEmpty())
            return Optional.empty();

        var name = new Name(maybeName.get().value());
        var arguments = maybeArgList.get();
        return Optional.of(new FunctionCallStatement(name, arguments));
    }

    private Optional<Statement> maybeParseNextStatement() {
        var nextToken = tokenizer.peekToken();
        if (nextToken.type() == Type.NAME)
            return maybeParseFunctionCallStatement();

        var tokenVal = nextToken.value();
        return switch (tokenVal) {
            case "if" -> maybeParseIfStatement();
            case "val" -> maybeParseDeclarationStatement();
            default -> {
                errorManager.emitSyntaxError("Unexpected token `%s`", tokenVal);
                yield Optional.empty();
            }
        };
    }

    public Statement parse() {
        if (tokenizer.atEndOfFile())
            errorManager.emitFatalError("Expected a variable declaration, if, or function call statement");

        // As per the grammar, only one top-level statement is allowed per program.
        var maybeNextStmt = maybeParseNextStatement();
        if (maybeNextStmt.isEmpty())
            die("");

        return maybeNextStmt.get();
    }
}
