package com.example.jianlou.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {
    // 时间差阈值：2分钟（单位：毫秒）
    private static final long TIME_THRESHOLD = 2 * 60 * 1000;

    /**
     * 计算两个时间戳的差值（是否超过2分钟）
     *
     @param time1
     前一条消息时间戳（13位毫秒数字符串）
     *
     @param time2
     当前消息时间戳（13位毫秒数字符串）
     *
     @return
     true=超过2分钟，false=未超过
     */
    public static boolean isOver2Minutes(String time1, String time2) {
        if (time1 == null || time1.isEmpty() || time2 == null || time2.isEmpty()) {
            return true; // 第一条消息默认显示时间
        }
        try {
            long t1 = Long.parseLong(time1);
            long t2 = Long.parseLong(time2);
            return Math.abs(t2 - t1) > TIME_THRESHOLD;
        } catch (NumberFormatException e) {
            e
                    .printStackTrace();
            return true;
        }
    }

    /**
     * 将13位时间戳格式化为「yyyy-MM-dd HH:mm」
     *
     @param timeStamp
     13位毫秒数字符串
     *
     @return
     格式化后的时间字符串
     */
    public static String formatTime(String timeStamp) {
        if (timeStamp == null || timeStamp.isEmpty()) {
            return "";
        }
        try {
            long time = Long.parseLong(timeStamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
            return sdf.format(new Date(time));
        } catch (NumberFormatException e) {
            e
                    .printStackTrace();
            return "";
        }
    }
}
