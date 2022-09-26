package mylang.ast;

import java.util.Objects;

public record Number(Integer number) {
    public Number {
        Objects.requireNonNull(number);
    }
}
