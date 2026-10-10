import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 컴파일된 클래스를 java로 실행하고, 표준 출력·표준 에러를 화면에 출력한 뒤 종료 코드를 돌려준다.
public class RunManager {

    public int run(File javaFile, String args) throws IOException, InterruptedException {
        File source = javaFile.getAbsoluteFile();
        String className = source.getName().replaceAll("\\.java$", "");

        List<String> command = new ArrayList<>(List.of("java", "-cp", ".", className));
        if (args != null && !args.isBlank()) {
            command.addAll(Arrays.asList(args.trim().split("\\s+")));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(source.getParentFile());
        // stdout과 stderr를 한 스트림으로 합쳐 출력 순서를 유지하고, 버퍼가 차서 멈추는 일을 막는다
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // 대상 프로그램이 키보드 입력을 기다리며 멈추지 않도록 입력을 바로 닫는다
        process.getOutputStream().close();

        // 줄 단위가 아닌 바이트 그대로 바로 넘겨, 줄바꿈 없는 출력(print)도 즉시·원본 그대로 보이게 한다
        process.getInputStream().transferTo(System.out);
        System.out.flush();
        return process.waitFor();
    }
}
