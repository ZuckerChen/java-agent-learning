package com.cz.demo.apm.client;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author chenzhang
 * @date 2022/12/18 12:15 上午
 */
public class BusinessManager {
    public void doBusiness() {
        RpcClient rpcClient = new RpcClient();
        DbClient dbClient = new DbClient();
        RedisClient redisClient = new RedisClient();

        rpcClient.invoke();
        dbClient.invoke();
        redisClient.invoke();

        System.out.println("do business invoke");
    }
}
