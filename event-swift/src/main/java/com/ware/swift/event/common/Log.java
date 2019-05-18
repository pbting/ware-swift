package com.ware.swift.event.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {

    private static final String thisClassName = Log.class.getName();
    private static final String msgSep = "\r\n";
    private static final Logger logger = LoggerFactory.getLogger(Log.class);

    private static Object getStackMsg(Object msg) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        if (ste == null) {
            return "";
        }

        boolean srcFlag = false;
        for (int i = 0; i < ste.length; i++) {
            StackTraceElement s = ste[i];
            // 如果上一行堆栈代码是本类的堆栈，则该行代码则为源代码的最原始堆栈。
            if (srcFlag) {
                return s == null ? "" : s.toString() + msgSep + msg.toString();
            }
            // 定位本类的堆栈
            if (thisClassName.equals(s.getClassName())) {
                srcFlag = true;
                i++;
            }
        }
        return "";
    }

    public static void debug(Object msg) {
        Object message = getStackMsg(msg);
        logger.debug(message.toString());
    }

    public static void debug(Object msg, Throwable t) {
        Object message = getStackMsg(msg);
        logger.debug(message.toString(), t);
    }

    public static void info(Object msg) {
        Object message = getStackMsg(msg);
        logger.info(message.toString());
    }

    public static void info(Object msg, Throwable t) {
        Object message = getStackMsg(msg);
        logger.info(message.toString(), t);
    }

    public static void warn(Object msg) {
        Object message = getStackMsg(msg);
        logger.warn(message.toString());
    }

    public static void warn(Object msg, Throwable t) {
        Object message = getStackMsg(msg);
        logger.warn(message.toString(), t);
    }

    public static void error(Object msg) {
        Object message = getStackMsg(msg);
        logger.error(message.toString());
    }

    public static void error(Object msg, Throwable t) {
        Object message = getStackMsg(msg);
        if (logger != null) {
            logger.error(message.toString(), t);
        }
    }
}
