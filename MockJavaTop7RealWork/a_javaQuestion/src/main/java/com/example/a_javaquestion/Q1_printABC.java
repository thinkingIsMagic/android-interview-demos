package com.example.a_javaquestion;

/*
题目：
多线程打印 ABC（编程题）
3个线程，一个打A、一个打B、一个打C
输出五次：ABCABC...

思路：synchronized + wait/notifyAll
 */
public class Q1_printABC {
    private static final int PRINT_COUNT = 5;
    // 0: A、1:B、2:C
    private static int tag = 0;
    // 锁
    private static Object lock = new Object();


    public static void main(String[] args) {
        new Thread(()->{
            for(int i=0; i<PRINT_COUNT; i++){
                synchronized (lock){
                    while(tag != 0){
                        try {
                            lock.wait();
                        }catch (Exception e){
                        }
                    }
                    System.out.println("A");
                    tag=1;
                    lock.notifyAll();
                }
            }
        },"threadA").start();
        new Thread(()->{
            for(int i=0; i<PRINT_COUNT; i++){
                synchronized (lock){
                    while(tag != 1){
                        try {
                            lock.wait();
                        }catch (Exception e){}
                    }
                    System.out.println("B");
                    tag=2;
                    lock.notifyAll();
                }
            }
        },"threadB").start();
        new Thread(()->{
            for(int i=0; i<PRINT_COUNT; i++){
                synchronized (lock){
                    while(tag != 2) {
                        try {
                            lock.wait();
                        } catch (Exception e) {
                        }
                    }
                    System.out.println("C");
                    tag=0;
                    lock.notifyAll();
                }
            }
        },"threadC").start();
    }
}