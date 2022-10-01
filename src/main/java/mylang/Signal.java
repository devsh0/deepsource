package mylang;

public class Signal<T> {
    private final T value;
    private final String message;

    private Signal(T value, String message) {
        this.value = value;
        this.message = message;
    }

    public boolean success() {
        return value != null;
    }

    public boolean failure() {
        return !success();
    }

    public String message() {
        return message;
    }


    public T get() {
        if (failure())
            throw new RuntimeException("Invalid access!");
        return value;
    }

    public static <T> Signal<T> successInstance(T object) {
        if (object == null)
            throw new RuntimeException("Success signal requires non-null object!");
        return new Signal<>(object, "");
    }

    public static <T> Signal<T> failureInstance(String message) {
        if (message == null)
            throw new RuntimeException("Failure signal requires a valid message!");
        return new Signal<>(null, message);
    }
}
