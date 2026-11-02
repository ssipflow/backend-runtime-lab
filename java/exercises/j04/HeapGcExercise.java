package j04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * J-04 연습문제
 *
 * Heap Retention과 GC 관찰
 *
 * 다음 세 가지 명령을 처리하는 콘솔 프로그램을 작성하십시오.
 * allocate - 1MiB 배열 10개를 생성하여 List에 보관
 * release - List에 보관한 배열 참조를 모두 제거
 * status - 현재 보관 중인 배열 수, Used/Committed/Max Heap 출력
 * exit - 프로그램 종료
 *
 * 요구사항
 * 1. Scanner 또는 BufferedReader로 사용자 명령을 입력받습니다.
 * 2. 배열 참조는 Static List에 보관합니다.
 * 3. allocate를 반복하면 보관 객체 수가 증가해야 합니다.
 * 4. release는 보관 객체에 대한 참조를 제거해야 합니다.
 * 5. status는 Runtime API를 사용해 Heap 사용량을 출력합니다.
 * 6. System.gc()를 호출하지 않습니다.
 *
 * 제약조건
 * - Java 17 이상
 * - JDK 표준 라이브러리만 사용
 * - Heap 최대 크기 128MiB
 * - 참조 해제와 실제 GC 회수 시점을 구분할 것
 * - 정답 코드는 직접 작성
 *
 * 완료 기준
 * - allocate 반복 시 보관 객체 수와 Heap 사용량 증가 확인
 * - release 이후 보관 객체 수가 0으로 변경
 * - 이후 GC가 발생했을 때 Heap 사용량 감소 여부 확인
 * - GC 로그에서 수집 전후 Heap 사용량 해석
 *
 * 힌트
 * - `List.clear()` 는 List에 저장된 객체 참조를 제거하지만 객체 메모리를 즉시 회수하는 메서드는 아니다.
 */
public class HeapGcExercise {

    private static final List<byte[]> retained = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );

        while (true) {
            System.out.print("command> ");

            String command = reader.readLine();

            if (command == null) {
                break;
            }

            switch (command.toLowerCase().trim()) {
                case "allocate" -> allocate();
                case "release" -> release();
                case "status" -> status();
                case "exit" -> {
                    System.out.println("exit");
                    return ;
                }
                default -> {
                    System.out.println("Unavailable");
                }
            }
        }
    }

    private static void allocate() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            byte[] data = new byte[1024 * 1024];
            data[0] = 1;
            data[data.length - 1] = 2;

            retained.add(data);

            TimeUnit.MILLISECONDS.sleep(200);
        }
    }

    private static void release() {
        retained.clear();
    }

    private static void status() throws IOException {
        // 현재 보관 중인 배열 수, Used/Committed/Max Heap 출력
        Runtime runtime = Runtime.getRuntime();

        int listSize = retained.size();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long committed = runtime.totalMemory();
        long max = runtime.maxMemory();

        System.out.printf(
                "[MEM STATUS] list-size=%d, used=%.2f MiB, committed=%.2f MiB, max=%.2fMiB%n",
                listSize,
                used / 1024.0 / 1024.0,
                committed / 1024.0 / 1024.0,
                max / 1024.0 / 1024.0
        );
    }
}
