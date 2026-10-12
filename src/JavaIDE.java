import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// A 담당: 입력/메뉴, 업로드한 파일의 상태, 에러 출력, 로그 호출.
public class JavaIDE {
    private static final String SEPARATOR = "##########################";
    // PDF 입력 검증 표의 메시지를 그대로 사용한다 (U+2013 en dash).
    private static final String VALIDATION_DASH = " \u2013 ";
    private static final String NO_FILE = "Error: no file uploaded";
    private static final String COMPILE_FIRST = "Error: compile the file first";
    private static final String NO_ERROR_FILE = "Error: no compile error file";

    private final BufferedReader input = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final LogManager logger = new LogManager();
    private final RunManager runner = new RunManager();
    private CompileManager compiler = new CompileManager();
    private File uploadedFile;
    private boolean compiled;
    private boolean running = true;

    public static void main(String[] args) {
        // Windows 기본 출력 인코딩에서도 PDF의 en dash와 한글 경로를 보존한다.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        new JavaIDE().start();
    }

    public void start() {
        while (running) {
            printMenu();
            String choice;
            try {
                choice = input.readLine();
            } catch (IOException e) {
                System.out.println("Error: cannot read input - " + e.getMessage());
                log("INPUT FAILED - " + e.getMessage());
                break;
            }
            if (choice == null) {
                log("EXIT (end of input)");
                break;
            }
            choice = choice.trim();
            // PDF 실행 예시: 메뉴 선택 후 결과/추가 입력 앞에도 구분선을 표시한다.
            printSeparator();
            try {
                switch (choice) {
                    case "1": upload(); break;
                    case "2": compile(); break;
                    case "3": run(); break;
                    case "4": reset(); break;
                    case "5": showErrorFile(); break;
                    case "6": showLog(); break;
                    case "q":
                    case "Q":
                        log("EXIT");
                        running = false;
                        break;
                    default:
                        System.out.println("Error: invalid choice - " + choice);
                        log("INVALID CHOICE - " + choice);
                }
            } catch (IOException | SecurityException e) {
                System.out.println("Error: operation failed - " + e.getMessage());
                log("MENU " + choice + " FAILED - " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Error: operation interrupted");
                log("MENU " + choice + " INTERRUPTED");
                // 메뉴로 복귀한다. B가 InterruptedException을 던지면 interrupt 상태는 해제된다.
            }
        }
    }

    private void printMenu() {
        printSeparator();
        System.out.println("1. Java File Upload");
        System.out.println("2. Compile");
        System.out.println("3. Run");
        System.out.println("4. Reset");
        System.out.println("5. Compile Error File");
        System.out.println("6. Show Log");
        System.out.print("Choice: ");
        System.out.flush();
    }

    private void printSeparator() {
        System.out.println();
        System.out.println(SEPARATOR);
    }

    private String readInput(String prompt) throws IOException {
        System.out.print(prompt);
        System.out.flush();
        String value = input.readLine();
        if (value == null) {
            log("EXIT (end of input)");
            running = false;
        }
        return value;
    }

    private void upload() throws IOException {
        String filename = readInput("Type Java Filename: ");
        if (filename == null) {
            return;
        }
        filename = filename.trim();
        File candidate = new File(filename);
        if (filename.isEmpty() || !candidate.isFile()) {
            System.out.println("Error: file not found" + VALIDATION_DASH + filename);
            log("UPLOAD REFUSED (file not found) - " + filename);
            return;
        }
        if (!candidate.getName().endsWith(".java")) {
            System.out.println("Error: not a java file" + VALIDATION_DASH + filename);
            log("UPLOAD REFUSED (not a java file) - " + filename);
            return;
        }
        candidate = candidate.getCanonicalFile();
        if (candidate.equals(uploadedFile)) {
            System.out.println("Error: file already uploaded" + VALIDATION_DASH + filename);
            log("UPLOAD REFUSED (already uploaded) - " + candidate.getName());
            return;
        }
        uploadedFile = candidate;
        compiled = false;
        // 새 파일에서 이전 파일의 컴파일 오류를 표시하지 않는다.
        compiler = new CompileManager();
        log("UPLOAD " + uploadedFile.getName());
    }

    private void compile() throws IOException, InterruptedException {
        if (uploadedFile == null) {
            System.out.println(NO_FILE);
            log("COMPILE REFUSED (no file)");
            return;
        }
        // 재컴파일이 실패하거나 예외가 나도 이전 성공 상태를 재사용하지 않는다.
        compiled = false;
        compiled = compiler.compile(uploadedFile);
        System.out.println(compiler.getSummary());
        log(compiled
                ? "COMPILE SUCCESS - " + uploadedFile.getName()
                : "COMPILE FAIL (" + compiler.getErrorCount() + " errors) - " + uploadedFile.getName());
    }

    private void run() throws IOException, InterruptedException {
        if (uploadedFile == null) {
            System.out.println(NO_FILE);
            log("RUN REFUSED (no file)");
            return;
        }
        if (!compiled) {
            System.out.println(COMPILE_FIRST);
            log("RUN REFUSED (not compiled)");
            return;
        }
        String args = readInput("Type Program Arguments (Enter for none): ");
        if (args == null) {
            return;
        }
        int exitCode = runner.run(uploadedFile, args);
        System.out.println("Exit Code: " + exitCode);
        log("RUN args=\"" + args + "\" exit=" + exitCode);
    }

    private void reset() {
        uploadedFile = null;
        compiled = false;
        compiler = new CompileManager();
        System.out.println("reset completed");
        log("RESET");
    }

    private void showErrorFile() throws IOException {
        File errorFile = compiler.getErrorFile();
        if (errorFile == null || !errorFile.isFile()) {
            System.out.println(NO_ERROR_FILE);
            log("COMPILE ERROR FILE REFUSED (no error file)");
            return;
        }
        // B가 javac 출력 바이트를 그대로 저장하므로 플랫폼 기본 문자셋으로 읽는다.
        try (BufferedReader reader = Files.newBufferedReader(errorFile.toPath(), Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        log("COMPILE ERROR FILE - " + errorFile.getName());
    }

    private void showLog() throws IOException {
        log("SHOW LOG");
        logger.show(System.out);
    }

    private void log(String message) {
        try {
            logger.write(message);
        } catch (IOException | SecurityException e) {
            System.out.println("Error: cannot write log - " + e.getMessage());
        }
    }
}
