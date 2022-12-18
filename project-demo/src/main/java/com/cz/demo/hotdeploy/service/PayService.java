package com.cz.demo.hotdeploy.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author chenzhang
 * @date 2022/12/18 3:58 下午
 */
public class PayService {
    private final static Random random = new Random();

    public void pay(String channel) throws InterruptedException {

        TimeUnit.MILLISECONDS.sleep(random.nextInt(5000));
        System.out.println("channel:" + channel + "invoke pay");
        addMethod();
    }

    private static void addMethod(){
        System.out.println("add method!");
    }
}
