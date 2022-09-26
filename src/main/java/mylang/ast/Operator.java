package mylang.ast;

import java.util.Objects;

public record Operator(String string) {
    public Operator {
        Objects.requireNonNull(string);
    }
}
