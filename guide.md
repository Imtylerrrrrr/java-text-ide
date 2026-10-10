# B → A 인터페이스 가이드

B(컴파일·실행) 담당이 A(메뉴·업로드·로그) 담당에게 제공하는 메서드 형태.
클래스명과 시그니처는 `out/`에 남아 있는 기존 클래스 구조에 맞췄다.

## 역할 경계

| 담당 | 클래스 | 책임 |
|---|---|---|
| A | `JavaIDE` | 메뉴 루프, 입력 받기, 업로드/리셋, **상태 관리**(`uploadedFile`, `compiled`), 에러 메시지 출력, 로그 기록 호출 |
| A | `LogManager` | `ide.log` 기록·출력 |
| B | `CompileManager` | javac 실행, stderr 파싱, `.error` 파일 저장 |
| B | `RunManager` | java 실행, stdout/stderr 출력, 종료 코드 반환 |

원칙: **B 클래스는 입력을 받지 않고, 로그를 쓰지 않고, 상태 검사를 하지 않는다.**
"업로드 안 됨", "컴파일 먼저" 같은 검사와 로그 기록은 전부 A(`JavaIDE`)가 B를 호출하기 전후에 한다.

---

## CompileManager (B)

```java
public class CompileManager {
    public boolean compile(File javaFile);
    public String  getSummary();
    public int     getErrorCount();
    public List<Integer> getErrorLines();
    public File    getErrorFile();   // 오류 파일 없으면 null
}
```

### `boolean compile(File javaFile)`
- 입력: 업로드된 `.java` 파일 (A가 존재·확장자 검증 완료한 것)
- 동작: `javac <파일>` 실행 → stderr 수집
- 반환: 성공 `true` / 실패 `false`
- 부수효과
  - 실패 시: stderr 전체를 `<파일명>.error`(예: `BonusPointAccount.java.error`)에 저장 (**원본 `.java`와 같은 폴더**), 오류 개수·줄 번호 저장
  - 성공 시: 이전 `.error` 파일 삭제, `getErrorFile()`은 `null`
  - javac 실행 자체가 안 될 때(javac 없음 등): `false`, 오류 개수 `0`, `getErrorFile()`은 `null`
- 화면 출력: **안 함** (출력은 A가 `getSummary()`로)

### `String getSummary()`
직전 `compile()` 결과 한 줄. A는 이걸 그대로 `println` 한다.
- 성공: `compiled successfully - BonusPointAccount.java`
- 실패: `2 compile errors occurred (lines 3, 11) - BonusPointAccount.java.error`
- javac 실행 불가: `Error: cannot run javac - BonusPointAccount.java`

### `int getErrorCount()` / `List<Integer> getErrorLines()`
로그 문자열 만들 때 사용 (`COMPILE FAIL (2 errors) - ...`). 성공 시 `0` / 빈 리스트.

### `File getErrorFile()`
메뉴 5(Compile Error File)에서 사용. `null`이면 A가 `Error: no compile error file` 출력.

---

## RunManager (B)

```java
public class RunManager {
    public int run(File javaFile, String args);
}
```

### `int run(File javaFile, String args)`
- 입력: 컴파일 성공한 `.java` 파일, 사용자가 입력한 인자 문자열 (Enter만 치면 `""`)
- 동작: 클래스명(파일명에서 `.java` 제거)으로 `java <클래스명> <인자들>` 실행. 인자는 `args.trim().split("\\s+")`로 공백 분리 (빈 문자열이면 인자 없음, 따옴표 묶음은 지원 안 함)
- 작업 폴더: `.java` 파일이 있는 폴더 (`ProcessBuilder.directory(javaFile.getParentFile())`)
- 화면 출력: 대상 프로그램의 **stdout과 stderr를 모두** 출력 (B가 직접)
- 반환: 종료 코드 (`Exit Code: N` 출력과 로그는 A가)
- java 실행 불가(java 없음 등): `Error: cannot run java - <클래스명>` 출력 후 `-1` 반환

---

## A 쪽 호출 순서 (참고)

```java
// 2. Compile
if (uploadedFile == null) { println(NO_FILE); logger.write("COMPILE REFUSED (no file)"); return; }
compiled = compiler.compile(uploadedFile);
System.out.println(compiler.getSummary());
logger.write(compiled
    ? "COMPILE SUCCESS - " + name
    : "COMPILE FAIL (" + compiler.getErrorCount() + " errors) - " + name);

// 3. Run
if (uploadedFile == null) { println(NO_FILE); ...; return; }
if (!compiled)            { println(COMPILE_FIRST); logger.write("RUN REFUSED (not compiled)"); return; }
String args = /* "Type Program Arguments (Enter for none): " 입력 */;
int exit = runner.run(uploadedFile, args);
System.out.println("Exit Code: " + exit);
logger.write("RUN args=\"" + args + "\" exit=" + exit);

// 4. Reset → uploadedFile = null; compiled = false;
```

## 예외 처리 약속
- B 메서드는 `IOException`, `InterruptedException`을 **안에서 직접 try-catch로 처리**한다. `throws` 없음.
- 그래서 A는 `compile()`, `run()`을 try-catch 없이 그냥 호출하면 된다.
  - `compile()` 실패 이유는 `getSummary()` 문자열에 들어 있음 (위 참고)
  - `run()`이 `-1`을 돌려주면 B가 이미 에러 메시지를 출력한 상태
- 단, 잘못된 메뉴 번호·빈 입력 등 **A 자신의 입력 처리**에서 나는 예외는 A가 막아야 한다 (비정상 종료 금지).

## 같이 쓰는 상수 (위치: `JavaIDE` 또는 별도 `Messages`)

```java
static final String NO_FILE       = "Error: no file uploaded";
static final String COMPILE_FIRST = "Error: compile the file first";
static final String NO_ERROR_FILE = "Error: no compile error file";
static final String DASH = " - ";   // 하이픈 (결정 사항 1 참고)
```

## 결정 사항
1. **대시는 하이픈 `-`** (메시지·로그 모두). PDF 설명문은 en dash `–`지만 실행 예시 화면과 로그 예시는 `-`라서, 실제 출력 기준인 예시를 따름. 터미널 인코딩 문제도 피할 수 있음.
2. **`.error` 파일은 원본 `.java`와 같은 폴더**에 `<파일명>.java.error`로 저장. 컴파일·실행도 그 폴더를 작업 디렉터리로 함 (`.class`도 같은 폴더에 생김).
3. **인자는 공백으로만 분리**, 따옴표로 묶은 인자(`"Kim Lee"`)는 지원 안 함. 과제 예시(`10000 Kim`)에 필요 없고 구현이 단순함. 보고서 특이 사항에 한계로 명시.
