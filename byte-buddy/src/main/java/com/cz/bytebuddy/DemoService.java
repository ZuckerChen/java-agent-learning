package com.cz.bytebuddy;

/**
 * @author chenzhang
 * @date 2022/12/15 7:35 下午
 */
public class DemoService {
    public String getName() {
        System.out.println("getName start");
        return "张三";
    }

    public Integer getAge() {
        System.out.println("getAge start");
        return 18;
    }
}
