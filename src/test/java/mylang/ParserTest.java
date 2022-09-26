package mylang;

import mylang.ast.FunctionCallStatement;
import mylang.ast.IfStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testEmptySource() {
        String source = "";
        try {
            var parser = new Parser(source);
            parser.parse();
        } catch (RuntimeException error) {
            assertTrue(error.getMessage().startsWith("Expected a variable"));
        }
    }

    @Test
    public void testIfStmtHasEmptyBody() {
        String source = "if name == 10 {}";
        var parser = new Parser(source);
        var stmt = parser.parse();
        assertTrue(stmt instanceof IfStatement);
    }

    @Test
    public void testIfStmtHasInvalidOp() {
        String source = "if name = 10 {}";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }


    @Test
    public void testIfStmtMissingLbrace() {
        String source = "if name == 10 }";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testIfStmtMissingRbrace() {
        String source = "if name == 10 { call()";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testIfStmtMissingOp() {
        String source = "if name 10 {}";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testIfStmtHasNonEmptyBody() {
        String source = "if name == 10 {\n" +
                "callfun()\n" +
                "val name = 20\n" +
                "if fame == 0 {}\n" +
                "}";
        var parser = new Parser(source);
        assertTrue(parser.parse() instanceof IfStatement);
    }

    @Test
    public void testIfStmtWithNumericLhsAndRhs() {
        String source = "if 10 == 10 {}";
        var parser = new Parser(source);
        if (parser.parse() instanceof IfStatement ifStatement) {
            assertTrue(ifStatement.getConditionExpression().lhs().toString().contains("10"));
            assertTrue(ifStatement.getConditionExpression().rhs().toString().contains("10"));
        } else fail();
    }

    @Test
    public void testIfStmtWithVariableLhsAndRhs() {
        String source = "if value > othervalue {}";
        var parser = new Parser(source);
        if (parser.parse() instanceof IfStatement ifStatement) {
            assertTrue(ifStatement.getConditionExpression().lhs().toString().contains("value"));
            assertTrue(ifStatement.getConditionExpression().rhs().toString().contains("othervalue"));
        }
    }

    @Test
    public void testFunctionCallWithEmptyArgs() {
        String source = "function()";
        var parser = new Parser(source);
        var stmt = parser.parse();
        assertTrue(stmt instanceof FunctionCallStatement);
    }

    @Test
    public void testFunctionCallMissingLparen() {
        String source = "fun10)";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testFunctionCallMissingRparen() {
        String source = "fun(10";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testFunctionCallWithNonEmptyArgs() {
        String source = "function(1, 2)";
        var parser = new Parser(source);
        var stmt = parser.parse();
        if (stmt instanceof FunctionCallStatement fn)
            assertEquals(2, fn.arguments().size());
        else fail();
    }

    @Test
    public void testVariableDeclarationHasInvalidName() {
        String source = "val 0wesome = 10";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testVariableDeclaration() {
        String source = "val name = 10";
        var parser = new Parser(source);
        parser.parse();
    }

    @Test
    public void testVariableDeclarationHasInvalidValue() {
        String source = "val name = if";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testVariableDeclarationHasInvalidOp() {
        String source = "val value == 20";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testVariableDeclarationMissingOp() {
        String source = "val value 20";
        var parser = new Parser(source);
        try {
            parser.parse();
        } catch (RuntimeException e) {
            // FIXME: somehow supply the error message and test if we get the right error.
        }
    }

    @Test
    public void testEmitMultipleErrors() {
        String source = "if value == 10 {\n" +
                "val == 10\n" +
                "if blah = 10{}\n" +
                "val = 20\n" +
                "}";
        var parser = new Parser(source);
        parser.parse();
    }
}
