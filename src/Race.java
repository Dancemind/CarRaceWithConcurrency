import java.util.Map;
import java.util.concurrent.*;

/**
 * Гонка, в которой каждый автомобиль это поток. Всего 10 авто.
 * <p>
 * Каждый автомобиль:
 * - готовится к гонке (случайное число для каждого авто)
 * - едет участок 1
 * - едет через тоннель (одновременно едут 3 авто максимум)
 * - едет участок 2
 * <p>
 * У каждого автомобиля преодоление участков и тоннеля занимает разное
 * время (случайное число для каждого участка и тоннеля).
 * <p>
 * Все автомобили стартуют одновременно.
 * <p>
 * По окончанию гонки (после финиша всех 10 авто) вывести:
 * - номер победителя и его время
 * - результаты заезда (номер автомобиля, его время - для всех участников)
 */
public class Race {

    private static final int MAX_CARS_IN_TUNNEL = 3;
    private static final int CARS_COUNT = 10;

    private static final Semaphore tunnelSemaphore = new Semaphore(MAX_CARS_IN_TUNNEL);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final CyclicBarrier cyclicBarrier = new CyclicBarrier(CARS_COUNT);
    private static final Map<Integer, Long> results = new ConcurrentHashMap<>();
    private static final CountDownLatch countDownLatch = new CountDownLatch(CARS_COUNT);

    private static int winnerIndex = -1;
    private static final Object monitor = new Object();

    public static void main(String[] args) {

        for (int i = 0; i < CARS_COUNT; i++){
            final int index = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    getReady(index);
                    try {
                        cyclicBarrier.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    long startTime = System.currentTimeMillis();
                    firstSection(index);
                    tunnel(index);
                    secondSection(index);
                    synchronized (monitor) {
                        if (winnerIndex == -1)
                            winnerIndex = index;
                    }
                    long finishTime = System.currentTimeMillis();
                    results.put(index, finishTime - startTime);
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        for (int key : results.keySet()){
            System.out.println("Car " + key + " time: " + results.get(key) + " ms");
        }
        System.out.println("Winner: car " + winnerIndex + " Time: " + results.get(winnerIndex) + " ms");
    }

    private static void sleepRandomTime() {
        long msecSleepTime = (long) ((Math.random() * 5 + 1) * 1000);   // 1-5 seconds
        try {
            Thread.sleep(msecSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void getReady(int index){
        System.out.println("Car " + index + " is getting ready..");
        sleepRandomTime();
        System.out.println("Car " + index + " is ready!");
    }

    private static void firstSection(int index){
        System.out.println("Car " + index + " started first section..");
        sleepRandomTime();
        System.out.println("Car " + index + " finished first section!");
    }
    private static void secondSection(int index){
        System.out.println("Car " + index + " started second section..");
        sleepRandomTime();
        System.out.println("Car " + index + " finished second section!");
    }
    private static void tunnel(int index){
        try {
            tunnelSemaphore.acquire();
            System.out.println("Car " + index + " entered the tunnel..");
            sleepRandomTime();
            System.out.println("Car " + index + " passed the tunnel!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            tunnelSemaphore.release();
        }

    }
}
