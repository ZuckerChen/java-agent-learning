package com.cz.demo.apm;

import com.cz.demo.apm.client.BusinessManager;

/**
 * @author chenzhang
 * @date 2022/12/16 8:50 上午
 */
public class LogApplicationMain {
    public static void main(String[] args) {
        System.out.println("main project start");
        BusinessManager businessManager = new BusinessManager();
        businessManager.doBusiness();
    }
}
