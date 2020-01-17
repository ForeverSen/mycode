package com.huawei.l00379880.exam.utils;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author zhan
 * @date 2020/01/13 15:12:44
 * @description 描述信息
 */


public class IdUtils {
    private final long START_TIME = 1543403301020L;
    private final long WORKER_ID_BITS = 5L;
    private final long DATA_CENTER_ID_BITS = 5L;
    private final long MAX_WORKER_ID = 31L;
    private final long MAX_DATA_CENTER_ID = 31L;
    private final long SEQUENCE_BITS = 12L;
    private final long WORKER_ID_SHIFT = 12L;
    private final long DATA_CENTER_ID_SHIFT = 17L;
    private final long TIMESTAMP_LEFT_SHIFT = 22L;
    private final long SEQUENCE_MASK = 4095L;
    private long workerId;
    private long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private static IdUtils idWorker = new IdUtils(getWorkId(), getDataCenterId());

    private IdUtils(long workerId, long dataCenterId) {
        if (workerId <= 31L && workerId >= 0L) {
            if (dataCenterId <= 31L && dataCenterId >= 0L) {
                this.workerId = workerId;
                this.dataCenterId = dataCenterId;
            } else {
                throw new IllegalArgumentException(String.format("dataCenterId can't be greater than %d or less than 0", 31L));
            }
        } else {
            throw new IllegalArgumentException(String.format("workerId can't be greater than %d or less than 0", 31L));
        }
    }

    private synchronized long nextId() {
        long timestamp = this.timeGen();
        if (timestamp < this.lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", this.lastTimestamp - timestamp));
        } else {
            if (this.lastTimestamp == timestamp) {
                this.sequence = this.sequence + 1L & 4095L;
                if (this.sequence == 0L) {
                    timestamp = this.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }

            this.lastTimestamp = timestamp;
            return timestamp - 1572525218655L << 22 | this.dataCenterId << 17 | this.workerId << 12 | this.sequence;
        }
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp;
        for (timestamp = this.timeGen(); timestamp <= lastTimestamp; timestamp = this.timeGen()) {
        }

        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private static Long getWorkId() {
        try {
            String hostAddress = Inet4Address.getLocalHost().getHostAddress();
            int[] ints = StringUtils.toCodePoints(hostAddress);
            int sums = 0;
            int[] var3 = ints;
            int var4 = ints.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                int b = var3[var5];
                sums += b;
            }

            return (long) (sums % 32);
        } catch (Exception var7) {
            return RandomUtils.nextLong(0L, 31L);
        }
    }

    private static Long getDataCenterId() {
        int sums;
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            sums = getIntAmount(hostName);
        } catch (UnknownHostException var2) {
            sums = getIntAmount((String) null);
        }

        return (long) (sums % 32);
    }

    private static int getIntAmount(String hostName) {
        hostName = StringUtils.isBlank(hostName) ? UUID.randomUUID().toString() : hostName;
        return Arrays.stream(StringUtils.toCodePoints(hostName)).sum();
    }

    public static Long generateId() {
        return idWorker.nextId();
    }

    public static void main(String[] args) {
        System.out.println(generateId());
    }
}
