package mylang;

import mylang.ast.Statement;

import java.util.List;

public class CompilationResult {
    private final List<Problem> problems;
    private final Statement root;

    public CompilationResult(Statement root, List<Problem> problems) {
        this.root = root;
        this.problems = problems;
    }

    // Compilation fails only when we fail to yield an AST root.
    // Even when there are problems but the top-level statement is fine, we don't consider that a compilation failure.
    // Questionable design decision, but for now I'm gonna roll with it.
    public boolean failed() {
        return root == null;
    }

    public boolean hasProblems() {
        return problems.size() > 0;
    }

    public Statement astRoot() {
        return root;
    }

    public List<Problem> problems() {
        return problems;
    }
}
