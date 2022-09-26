package mylang.ast;

import java.util.List;
import java.util.Objects;

public class IfStatement implements Statement {
    private final ConditionExpression conditionExpression;
    private final List<Statement> statements;

    public IfStatement(ConditionExpression conditionExpression, List<Statement> stmts) {
        this.conditionExpression = conditionExpression;
        statements = Objects.requireNonNull(stmts);
    }

    public ConditionExpression getConditionExpression() {
        return conditionExpression;
    }
}
