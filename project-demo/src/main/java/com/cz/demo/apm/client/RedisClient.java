package com.cz.demo.apm.client;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author chenzhang
 * @date 2022/12/18 12:13 上午
 */
public class RedisClient {
    public void invoke() {
        int i = new Random().nextInt(5000);
        try {
            TimeUnit.MILLISECONDS.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("redis invoke");
    }
}
