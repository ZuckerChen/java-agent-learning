package com.cz.demo.hotdeploy.service;

/**
 * @author chenzhang
 * @date 2022/12/15 2:26 下午
 */
public class ApplicationMain {
    public static void main(String[] args) throws InterruptedException {
        PayService payService = new PayService();
        while (true) {
            payService.pay("wechat");
        }
    }
}
