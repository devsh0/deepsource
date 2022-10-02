package mylang;

import mylang.ast.DeclarationStatement;
import mylang.ast.FunctionCallStatement;
import mylang.ast.IfStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {
    private static final boolean debug = true;

    public void dbgprint(String message) {
        if (debug)
            System.out.println(message);
    }

    @Test
    public void testEmptySource() {
        String source = "";
        try {
            var parser = new Parser(source);
            parser.parse();
        } catch (RuntimeException error) {
            assertTrue(error.getMessage().startsWith("Invalid input"));
        }
    }

    @Test
    public void testNullSource() {
        try {
            var parser = new Parser(null);
            parser.parse();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Invalid input"));
            return;
        }
        fail();
    }

    @Test
    public void testIfStmtHasEmptyBody() {
        String source = "if name == 10 {}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());
        assertTrue(result.astRoot() instanceof IfStatement);
    }

    @Test
    public void testIfStmtHasInvalidOp() {
        String source = "if name = 10 {}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Unexpected operator `=`"));
        assertEquals(1, problem.line());
        assertEquals(9, problem.column());

        dbgprint(problem.prettyError());
    }


    @Test
    public void testIfStmtMissingLbrace() {
        String source = "if name == 10 }";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Expected token of type `LBRACE`"));
        assertEquals(1, problem.line());
        assertEquals(15, problem.column());

        dbgprint(problem.prettyError());
    }

    @Test
    public void testIfStmtMissingRbrace() {
        String source = "if name == 10 { call()";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());
        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Premature end-of-file"));
    }

    @Test
    public void testIfStmtMissingOp() {
        String source = "if name 10 {}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Expected token of type `OPERATOR`"));
        assertEquals(1, problem.line());
        assertEquals(9, problem.column());

        dbgprint(problem.prettyError());
    }

    @Test
    public void testIfStmtHasNonEmptyBody() {
        String source = "if name == 10 {\n" +
                "callfun()\n" +
                "val name = 20\n" +
                "if fame == 0 {}\n" +
                "}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var stmt = (IfStatement) result.astRoot();
        assertEquals(3, stmt.statements().size());
    }

    @Test
    public void testIfStmtWithNumericLhsAndRhs() {
        String source = "if 10 == 10 {}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());
        var ifStatement = (IfStatement) result.astRoot();
        assertTrue(ifStatement.getConditionExpression().lhs().toString().contains("10"));
        assertTrue(ifStatement.getConditionExpression().rhs().toString().contains("10"));
    }

    @Test
    public void testIfStmtWithVariableLhsAndRhs() {
        String source = "if value > othervalue {}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());
        var ifStatement = (IfStatement) result.astRoot();
        assertTrue(ifStatement.getConditionExpression().lhs().toString().contains("value"));
        assertTrue(ifStatement.getConditionExpression().rhs().toString().contains("othervalue"));
    }

    @Test
    public void testFunctionCallWithEmptyArgs() {
        String source = "function()";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var call = (FunctionCallStatement) result.astRoot();
        assertEquals("function", call.name().name());
    }

    @Test
    public void testFunctionCallMissingLparen() {
        String source = "fun10)";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertEquals(1, problem.line());
        assertEquals(4, problem.column());

        dbgprint(problem.prettyError());
    }

    @Test
    public void testFunctionCallMissingRparen() {
        String source = "fun(10";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Premature end-of-file"));
        assertEquals(1, problem.line());
        assertEquals(7, problem.column());
    }

    @Test
    public void testFunctionCallWithNonEmptyArgs() {
        String source = "function(30, 40)";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var fn = (FunctionCallStatement) result.astRoot();
        assertEquals("function", fn.name().name());
        assertEquals(2, fn.arguments().size());
        assertTrue(fn.arguments().get(0).toString().contains("30"));
        assertTrue(fn.arguments().get(1).toString().contains("40"));
    }

    @Test
    public void testVariableDeclarationHasInvalidName() {
        String source = "val 0wesome = 10";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Expected token of type `NAME`"));
        assertEquals(1, problem.line());
        assertEquals(5, problem.column());
    }

    @Test
    public void testVariableDeclaration() {
        String source = "val name = 10";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var stmt = (DeclarationStatement) result.astRoot();
        assertEquals("name", stmt.name().name());
        assertTrue(stmt.number().toString().contains("10"));
    }

    @Test
    public void testVariableDeclarationHasInvalidValue() {
        String source = "val name = if";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Premature end-of-file"));
    }

    @Test
    public void testVariableDeclarationHasInvalidOp() {
        String source = "val value == 20";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Expected `=`"));
        assertEquals(1, problem.line());
        assertEquals(11, problem.column());

        dbgprint(problem.prettyError());
    }

    @Test
    public void testVariableDeclarationMissingOp() {
        String source = "val value 20";
        var parser = new Parser(source);
        var result = parser.parse();
        assertTrue(result.failed());

        var problem = result.problems().get(0);
        assertTrue(problem.description().startsWith("Expected `=`"));
        assertEquals(1, problem.line());
        assertEquals(11, problem.column());

        dbgprint(problem.prettyError());
    }

    @Test
    public void testEmitMultipleErrors() {
        String source = "if value == 10 {\n" +
                "val == 10\n" +
                "if blah = 10{}\n" +
                "val = 20\n" +
                "}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var problems = result.problems();
        assertEquals(3, problems.size());

        var p1 = problems.get(0);
        assertTrue(p1.description().startsWith("Expected token of type `NAME`"));
        assertEquals(2, p1.line());
        assertEquals(5, p1.column());
        dbgprint(p1.prettyError());

        var p2 = problems.get(1);
        assertTrue(p2.description().startsWith("Unexpected operator `=`"));
        assertEquals(3, p2.line());
        assertEquals(9, p2.column());
        dbgprint(p2.prettyError());

        var p3 = problems.get(2);
        assertTrue(p3.description().startsWith("Expected token of type `NAME`"));
        assertEquals(4, p3.line());
        assertEquals(5, p3.column());
        dbgprint(p3.prettyError());
    }

    @Test
    public void multipleStmtInSameLine() {
        String source = "if name == 10 {\n" +
                "val name = 10 call()\n" +
                "}";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var ifStmt = (IfStatement) result.astRoot();
        var statements = ifStmt.statements();
        assertEquals(2, statements.size());

        var decl = (DeclarationStatement) statements.get(0);
        assertEquals("name", decl.name().name());
        assertTrue(decl.number().toString().contains("10"));

        var call = (FunctionCallStatement) statements.get(1);
        assertEquals("call", call.name().name());
    }

    @Test
    public void testStatementsSpanningMultipleLines() {
        String source = "if value == 10 {\n" +
                "val variable =\n" +
                "20\n" +
                "callthis(\n" +
                "50, 60\n" +
                ")\n" +
                "}";

        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var ifStmt = (IfStatement) result.astRoot();
        var stmts = ifStmt.statements();

        var declStmt = (DeclarationStatement) stmts.get(0);
        assertEquals("variable", declStmt.name().name());
        assertEquals(20, declStmt.number().number().intValue());

        var fnCall = (FunctionCallStatement) stmts.get(1);
        assertEquals("callthis", fnCall.name().name());
        assertTrue(fnCall.arguments().get(0).toString().contains("50"));
        assertTrue(fnCall.arguments().get(1).toString().contains("60"));
    }

    @Test
    public void testErrorEmittedForStatementsSpanningMultipleLines() {
        String source = "if value == 10 {\n\n" +
                "val name == 10\n" +
                "call\n" +
                "(" +
                "\n}\n";
        var parser = new Parser(source);
        var result = parser.parse();
        assertFalse(result.failed());

        var problems = result.problems();
        var p1 = problems.get(0);
        assertTrue(p1.description().startsWith("Expected `=`"));
        assertEquals(3, p1.line());
        assertEquals(10, p1.column());

        var p2 = problems.get(1);
        assertTrue(p2.description().startsWith("Expected a name, number, or `)`"));
        assertEquals(5, p2.line());
        assertEquals(2, p2.column());
    }
}
