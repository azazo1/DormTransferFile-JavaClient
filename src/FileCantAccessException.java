import java.io.IOException;

public class FileCantAccessException extends IOException {
    public FileCantAccessException(String s) {
        super(s);
    }
}
