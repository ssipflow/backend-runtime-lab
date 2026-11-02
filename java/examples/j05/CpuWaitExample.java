package j05;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CpuWaitExample {

    // 계산 결과가 최적화 과정에서 불필요하게 제거되는 것을 방지한다.
    private static volatile long result;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("[PID] " + ProcessHandle.current().pid());

        List<Thread> threads = new ArrayList<>();

        // CPU를 계속 사용하는 작업 2개
        for (int i = 1; i <= 2; i++) {
            Thread worker = new Thread(
                    CpuWaitExample::cpuTask,
                    "cpu-worker-" + i
            );
            threads.add(worker);
            worker.start();
        }

        // CPU를 거의 사용하지 않고 대기하는 작업 2개
        for (int i = 1; i <= 2; i++) {
            Thread worker = new Thread(
                    CpuWaitExample::waitTask,
                    "wait-worker-" + i
            );
            threads.add(worker);
            worker.start();
        }

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        // 1초 간격으로 각 스레드 상태를 확인한다.
        for (int i = 0; i < 60; i++) {
            TimeUnit.SECONDS.sleep(1);

            System.out.println("--- SAMPLE " + (i + 1) + "---");

            for (Thread thread: threads) {
                System.out.printf(
                        "%s state=%s%n",
                        thread.getName(),
                        thread.getState()
                );
            }
        }

        // 샘플의 계산 작업을 종료한다.
        for (Thread thread: threads) {
            thread.interrupt();
        }

        for (Thread thread: threads) {
            thread.join();
        }

        System.out.println("Result: " + result);
    }

    private static void cpuTask() {
        long value = 0;

        // Interrupt 요청이 들어올 때까지 계산을 반복한다.
        while(!Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < 100_000; i++) {
                value += i * 31L;
            }

            // 계산 결과를 외부에서 관찰 가능한 필드에 기록한다.
            result = value;
        }
    }

    private static void waitTask() {
        try {
            while(!Thread.currentThread().isInterrupted()) {
                // 타이머 대기 중에는 CPU를 계속 점유하지 않는다.
                TimeUnit.SECONDS.sleep(1);
            }
        } catch(InterruptedException e) {

            // InterruptedException 발생 시 interrupt 상태가
            // 초기화될 수 있으므로 다시 설정한다.
            Thread.currentThread().interrupt();
        }
    }
}
