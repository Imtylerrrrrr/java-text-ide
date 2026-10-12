# Java Text IDE

객체지향프로그래밍 Term Project 1. 자바 파일을 업로드·컴파일·실행하는 텍스트 기반 IDE입니다.

## 실행 방법

JDK 17 이상이 필요합니다. `javac`와 `java`가 PATH에 있어야 합니다.

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out JavaIDE
```

## 메뉴

1. Java File Upload: 컴파일할 `.java` 파일을 지정합니다.
2. Compile: 업로드한 파일을 컴파일합니다. 실패하면 오류를 `<파일명>.java.error`에 저장합니다.
3. Run: 컴파일된 프로그램을 인자와 함께 실행하고 Exit Code를 보여줍니다.
4. Reset: 업로드한 파일과 컴파일 상태를 초기화합니다.
5. Compile Error File: 저장된 `.error` 파일 내용을 보여줍니다.
6. Show Log: 작업 기록(`ide.log`)을 보여줍니다.

## 파일 구성

| 파일 | 역할 | 담당 |
|---|---|---|
| `src/JavaIDE.java` | 메뉴, 업로드, 입력 검증 | A |
| `src/LogManager.java` | `ide.log` 기록·출력 | A |
| `src/CompileManager.java` | `javac` 실행, 오류 줄 번호 추출, `.error` 저장 | B |
| `src/RunManager.java` | `java` 실행, 출력·Exit Code 전달 | B |
| `guide.md` | A↔B 인터페이스 약속 | - |
