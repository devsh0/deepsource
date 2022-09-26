package mylang;

public class Utils {
    public static void die(String message) {
        throw new RuntimeException(message);
    }

    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isWhitespace(char ch) {
        return switch (ch) {
            case ' ', '\t' -> true;
            default -> false;
        };
    }

    public static boolean isAlpha(char ch) {
        return ch >= 'a' && ch <= 'z';
    }
}
