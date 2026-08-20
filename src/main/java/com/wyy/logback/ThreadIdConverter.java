package com.wyy.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 自定义 logback 转换器，在 pattern 中通过 %tid 输出线程编号（Thread.getId()）
 */
public class ThreadIdConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return String.valueOf(Thread.currentThread().getId());
    }
}
