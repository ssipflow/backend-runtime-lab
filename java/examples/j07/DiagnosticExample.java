package j07;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class DiagnosticExample {

    private static final Object LOCK = new Object();

    // GC Root를 통해 계속 도달 가능한 객체를 유지한다.
    private static final List<byte[]> RETAINED = new ArrayList<>();

    // CPU 계산 결과를 외부에서 관찰할 수 있도록 한다.
    private static volatile long cpuResult;

    public static void main(String[] args) {
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Commands: cpu, lock, heap, clear, status, exit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String command = scanner.nextLine().trim();

                switch (command) {
                    case "cpu" -> startCpuWorkers();
                    case "lock" -> startLockWorkers();
                    case "heap" -> allocateHeap();
                    case "clear" -> clearHeap();
                    case "status" -> printStatus();
                    case "exit" -> {
                        return ;
                    }
                    default -> System.out.println("Unknown command");
                }
            }
        }
    }

    private static void startCpuWorkers() {
        for (int i = 1; i <= 4; i++) {
            Thread worker = new Thread(() -> {
                long start = System.nanoTime();
                long duration = TimeUnit.SECONDS.toNanos(4);
                long value = 0;

                while(System.nanoTime() - start < duration) {
                    value += 1;
                }

                cpuResult = value;
            }, "cpu-worker-" + i);

            worker.start();
        }

        System.out.println("CPU workers started");
    }

    private static void startLockWorkers() {
        for (int i = 1; i <= 4; i++) {
            Thread worker = new Thread(() -> {
                synchronized (LOCK) {
                    System.out.println(
                            Thread.currentThread().getName()
                            + " aquired lock"
                    );

                    try {
                        Thread.sleep(5000);
                    } catch(InterruptedException e) {
                        System.err.println("Error on " + Thread.currentThread().getName());
                        System.err.println(e.getMessage());

                        Thread.currentThread().interrupt();
                    }
                }
            }, "lock-worker-" + i);

            worker.start();
        }

        System.out.println("Lock workers started");
    }

    private static void allocateHeap() {
        // 호출할때마다 약 10MiB의 배열을 추가한다.
        for (int i = 0; i < 10; i++) {
            RETAINED.add(new byte[1024 * 1024]);
        }

        System.out.println("Retained arrays: " + RETAINED.size());
    }

    private static void clearHeap() {
        RETAINED.clear();
        System.out.println("References cleared");
    }

    private static void printStatus() {
        Runtime runtime = Runtime.getRuntime();

        long used = runtime.totalMemory() - runtime.freeMemory();

        System.out.printf(
                "retained=%d, used=%.2f, commited=%.2f MiB\n",
                RETAINED.size(),
                used / 1024.0 / 1024.0,
                runtime.totalMemory() / 1024.0 / 1024.0
        );
    }
}
