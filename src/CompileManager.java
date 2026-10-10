import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// javac로 컴파일하고, 오류가 나면 stderr를 파싱해 개수·줄 번호를 구하고 .error 파일로 저장한다.
public class CompileManager {
    // 예: BonusPointAccount.java:3: error: ';' expected
    static final Pattern ERROR_LINE = Pattern.compile("\\.java:(\\d+): error:");

    private String fileName;
    private boolean success;
    private int errorCount;
    private List<Integer> errorLines = List.of();
    private File errorFile;
    private boolean javacFailed;

    public boolean compile(File javaFile) {
        File source = javaFile.getAbsoluteFile();
        File dir = source.getParentFile();
        fileName = source.getName();
        success = false;
        javacFailed = false;
        errorCount = 0;
        errorLines = List.of();
        errorFile = null;

        try {
            // 시스템 언어가 한국어여도 "error:" 형식으로 나오도록 javac 출력 언어를 영어로 고정
            ProcessBuilder pb = new ProcessBuilder("javac", "-J-Duser.language=en", "-encoding", "UTF-8", fileName);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            success = process.waitFor() == 0;

            File target = new File(dir, fileName + ".error");
            if (success) {
                Files.deleteIfExists(target.toPath());
                return true;
            }

            // 같은 줄의 오류가 여러 개면 개수는 모두 세고, 줄 번호는 한 번만 오름차순으로 기록
            TreeSet<Integer> lines = new TreeSet<>();
            Matcher m = ERROR_LINE.matcher(new String(output));
            while (m.find()) {
                errorCount++;
                lines.add(Integer.parseInt(m.group(1)));
            }
            errorLines = List.copyOf(lines);
            Files.write(target.toPath(), output);
            errorFile = target;
            return false;
        } catch (IOException e) {
            // javac를 찾지 못했거나 .error 파일을 쓰지 못한 경우
            return failJavac();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failJavac();
        }
    }

    private boolean failJavac() {
        success = false;
        javacFailed = true;
        errorCount = 0;
        errorLines = List.of();
        errorFile = null;
        return false;
    }

    public String getSummary() {
        if (fileName == null) {
            return "";
        }
        if (javacFailed) {
            return "Error: cannot run javac - " + fileName;
        }
        if (success) {
            return "compiled successfully - " + fileName;
        }
        String lines = errorLines.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return errorCount + " compile errors occurred (lines " + lines + ") - " + errorFile.getName();
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<Integer> getErrorLines() {
        return errorLines;
    }

    public File getErrorFile() {
        return errorFile;
    }
}
