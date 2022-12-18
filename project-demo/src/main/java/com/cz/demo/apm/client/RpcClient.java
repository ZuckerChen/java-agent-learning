package com.cz.demo.apm.client;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author chenzhang
 * @date 2022/12/16 8:36 上午
 */
public class RpcClient {
    public void invoke() {
        int i = new Random().nextInt(5000);
        try {
            TimeUnit.MILLISECONDS.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("rpc invoke");
    }
}
