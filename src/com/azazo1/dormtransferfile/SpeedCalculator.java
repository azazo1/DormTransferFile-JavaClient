package com.azazo1.dormtransferfile;

import java.util.LinkedList;

public final class SpeedCalculator {
    /**
     * 平均数参照时间, 单位 毫秒
     * 过大会导致性能开销太大
     */
    public static final int DEFAULT_CLIP_TIME_DURATION = 1000 * 5;
    /**
     * 当前已传输的字节数的各个存档
     */
    private final LinkedList<Long> nowBytes = new LinkedList<>();
    /**
     * 传输字节数存档对应的时间戳
     */
    private final LinkedList<Long> timestamps = new LinkedList<>();
    private final int clipTimeDuration;

    public SpeedCalculator(int clipTimeDuration) {
        this.clipTimeDuration = clipTimeDuration;
    }

    public SpeedCalculator() {
        this(DEFAULT_CLIP_TIME_DURATION);
    }

    /**
     * 输入数据
     */
    public void update(long now, long timeInMilli) {
        if (!timestamps.isEmpty()) {
            long lastTime = timestamps.getLast();
            if (timeInMilli - lastTime > clipTimeDuration / 10) { // 防止频率过大
                nowBytes.add(now);
                timestamps.add(timeInMilli);
            }
        } else {
            nowBytes.add(now);
            timestamps.add(timeInMilli);
        }

        long outTime = timeInMilli - clipTimeDuration;
        for (int i = 0; i < nowBytes.size(); i++) { // 清除掉过期的数据
            if (timestamps.get(i) < outTime) {
                timestamps.remove(i);
                nowBytes.remove(i);
                i--;
            }
        }
    }

    /**
     * 获得速度, 单位是 字节/秒
     */
    public long getSpeed() {
        if (nowBytes.isEmpty()) {
            return 0;
        }
        long deltaBytes = nowBytes.getLast() - nowBytes.getFirst();
        long deltaTime = timestamps.getLast() - timestamps.getFirst();
        return (long) (deltaBytes * 1.0 / deltaTime * 1000);
    }
}
