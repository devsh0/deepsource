package mylang.ast;

import java.util.List;

public record FunctionCallStatement(Name name, List<Object> arguments) implements Statement {
}
