package mylang.ast;

import java.util.Objects;

public record Name(String name) {
    public Name {
        Objects.requireNonNull(name);
    }
}
