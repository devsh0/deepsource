package mylang;

import mylang.ast.*;
import mylang.ast.Number;
import mylang.tokeniser.Tokeniser;
import mylang.tokeniser.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static mylang.Utils.die;

public class Parser {
    private final Tokeniser tokeniser;

    public Parser(String source) {
        tokeniser = new Tokeniser(source);
    }

    private Optional<Object> maybeParseNameOrNumber() {
        var maybeNextToken = tokeniser.eatAndMatch(Type.NAME, Type.NUMBER);
        return maybeNextToken.isEmpty() ? Optional.empty() : Optional.of(maybeNextToken.get());
    }

    private Optional<Operator> maybeParseOperator() {
        var maybeOperator = tokeniser.eatAndMatch(Type.OPERATOR);
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
            tokeniser.emitSyntaxError("Unexpected token `=`");
            return Optional.empty();
        }

        var maybeRhs = maybeParseNameOrNumber();
        if (maybeRhs.isEmpty())
            return Optional.empty();

        return Optional.of(new ConditionExpression(maybeLhs.get(), maybeRhs.get(), maybeOp.get()));
    }

    private Optional<Statement> maybeParseIfStatement() {
        tokeniser.eatToken(); // "if"

        var maybeCondExpr = maybeParseConditionExpression();
        if (maybeCondExpr.isEmpty())
            return Optional.empty();

        if (tokeniser.eatAndMatch(Type.LBRACE).isEmpty())
            return Optional.empty();

        List<Statement> statementList = new ArrayList<>();
        while (tokeniser.peekToken().type() != Type.RBRACE) {
            var maybeNextStmt = maybeParseNextStatement();
            if (maybeNextStmt.isEmpty())
                return Optional.empty();
            statementList.add(maybeNextStmt.get());
        }

        tokeniser.eatToken(); // "}"
        return Optional.of(new IfStatement(maybeCondExpr.get(), statementList));
    }

    private Optional<Statement> maybeParseDeclarationStatement() {
        tokeniser.eatToken(); // "val"

        var maybeName = tokeniser.eatAndMatch(Type.NAME);
        if (maybeName.isEmpty())
            return Optional.empty();

        var maybeAssignOp = tokeniser.eatAndMatch("=");
        if (maybeAssignOp.isEmpty())
            return Optional.empty();

        var maybeNumber = tokeniser.eatAndMatch(Type.NUMBER);
        if (maybeNumber.isEmpty())
            return Optional.empty();

        var name = new Name(maybeName.get().value());
        var number = new Number(Integer.valueOf(maybeNumber.get().value()));
        return Optional.of(new DeclarationStatement(name, number));
    }

    private Optional<List<Object>> maybeParseArgumentList() {
        var list = new ArrayList<>();
        if (tokeniser.eatAndMatch(Type.LPAREN).isEmpty())
            return Optional.empty();

        while (tokeniser.peekToken().type() != Type.RPAREN) {
            var maybeNameOrNumber = maybeParseNameOrNumber();
            if (maybeNameOrNumber.isEmpty())
                return Optional.empty();

            var nextToken = tokeniser.peekToken();
            if (nextToken.value().equals(",")) {
                tokeniser.eatToken();
                list.add(maybeNameOrNumber.get());
                maybeNameOrNumber = maybeParseNameOrNumber();
                if (maybeNameOrNumber.isEmpty())
                    return Optional.empty();
            }
            list.add(maybeNameOrNumber.get());
        }

        tokeniser.eatToken(); // ")"
        return Optional.of(list);
    }

    private Optional<Statement> maybeParseFunctionCallStatement() {
        var maybeName = tokeniser.eatAndMatch(Type.NAME);
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
        var nextToken = tokeniser.peekToken();
        if (nextToken.type() == Type.NAME)
            return maybeParseFunctionCallStatement();

        var tokenVal = nextToken.value();
        return switch (tokenVal) {
            case "if" -> maybeParseIfStatement();
            case "val" -> maybeParseDeclarationStatement();
            default -> {
                tokeniser.emitSyntaxError("Unexpected token `%s`", tokenVal);
                yield Optional.empty();
            }
        };
    }

    public Statement parse() {
        if (tokeniser.atEndOfFile())
            tokeniser.emitFatalError("Expected a variable declaration, if, or function call statement");

        // As per the grammar, only one top-level statement is allowed per program.
        var maybeNextStmt = maybeParseNextStatement();
        if (maybeNextStmt.isEmpty())
            die("");

        return maybeNextStmt.get();
    }
}
