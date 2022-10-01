package mylang;

public class Signal<T> {
    private final T value;
    private final boolean success;
    private final String message;

    private Signal(T value, boolean success, String message) {
        this.value = value;
        this.success = success;
        this.message = message;
    }

    public boolean success() {
        return success;
    }

    public boolean failure() {
        return !success;
    }

    public String message() {
        return message;
    }


    public T get() {
        if (failure() || value == null)
            throw new RuntimeException("Invalid access!");
        return value;
    }

    public static <T> Signal<T> of(T object) {
        return new Signal<>(object, true, "");
    }

    public static <T> Signal<T> fail(String message) {
        if (message == null)
            throw new RuntimeException("Failure signal requires a valid message!");
        return new Signal<>(null, false, message);
    }
}
