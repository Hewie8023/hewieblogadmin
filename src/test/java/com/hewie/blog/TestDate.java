package com.hewie.blog;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;

@Slf4j
public class TestDate {
    public static void main(String[] args) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar instance = Calendar.getInstance();
        instance.set(2999, 11, 1);
        long timeInMillis = instance.getTimeInMillis();
        log.info(currentTimeMillis + "");
        log.info(timeInMillis + "");
    }
}
