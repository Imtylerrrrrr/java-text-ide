import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 메뉴에서 발생한 모든 동작을 작업 폴더의 ide.log에 누적한다.
public class LogManager {
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logFile;

    public LogManager() {
        this(Path.of("ide.log"));
    }

    public LogManager(Path logFile) {
        this.logFile = logFile;
    }

    public void write(String message) throws IOException {
        // 파일명/예외에 줄바꿈이 있어도 한 동작은 로그 한 줄로 남긴다.
        String singleLine = message.replace('\r', ' ').replace('\n', ' ');
        String entry = "[" + LocalDateTime.now().format(TIMESTAMP) + "] "
                + singleLine + System.lineSeparator();
        Files.writeString(logFile, entry, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void show(PrintStream output) throws IOException {
        if (!Files.exists(logFile)) {
            output.println("No log entries");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.println(line);
            }
        }
    }
}
