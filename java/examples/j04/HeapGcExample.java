package j04;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeapGcExample {

    // Static 필드를 통해 배열에 대한 참조를 유지한다.
    private static final List<byte[]> retained = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("PID: " + ProcessHandle.current().pid());

        printMemory("START");

        // 1단계: 임시 객체를 반복적으로 생성한다.
        for (int i = 0; i < 100; i++) {
            createTemporaryObjects();
        }

        // 2단계: 생성한 객체의 참조를 계속 유지한다.
        for (int i = 0; i < 40; i++) {

            // 약 1MiB 배열을 생성하고 List에 보관한다.
            retained.add(new byte[1024 * 1024]);
            printMemory("RETAINED: " + (i + 1));

            // 외부에서 Heap 상태를 관찰한 시간을 확보한다.
            TimeUnit.MILLISECONDS.sleep(200);
        }

        System.out.println("Allocation finished.");

        // 프로그램이 종료되지 않도록 대기한다.
        // 이 시점에도 retained 가 배열을 참조하고 있다.
        TimeUnit.SECONDS.sleep(60);
    }

    private static void createTemporaryObjects() {
        // 반복적으로 임시 배열을 생성한다.
        // 메서드 종료 후 다른 참조가 없다면 GC 대상이 될 수 있다.
        byte[] data = new byte[1024 * 1024];

        // 배열을 실제로 사용한다.
        data[0] = 1;
        data[data.length - 1] = 2;
    }

    private static void printMemory(String label) {
        Runtime runtime = Runtime.getRuntime();

        long used = runtime.totalMemory() - runtime.freeMemory();
        long commited = runtime.totalMemory();
        long max = runtime.maxMemory();

        System.out.printf(
                "[%s] used=%.2f MiB, committed=%.2f MiB, max=%.2fMiB%n",
                label,
                used / 1024.0 / 1024.0,
                commited / 1024.0 / 1024.0,
                max / 1024.0 / 1024.0
        );
    }
}
