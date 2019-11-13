package test;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class NormalTest {

    @Test
    public void test(){
        System.gc();
    }

    EventLoop pool = new DefaultEventLoop(Executors.newFixedThreadPool(3));

    @Test
    public void testNettyFuture(){
        EventExecutor executor = new DefaultEventExecutor();
        Promise promise = new DefaultPromise(executor);
        promise.addListener(new GenericFutureListener<Future<String>>() {
            public void operationComplete(Future<String> future) throws Exception {
                if(future.isSuccess()){
                    log.info("任务结束，结果：" + future.get());
                } else {
                    log.info("任务失败，异常：" + future.cause());
                }
            }
        });
        pool.submit(()->{
            try {
                log.info("处理任务中...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                //promise.setFailure(e);
            }
//            promise.setFailure(new RuntimeException("发生异常"));
            promise.setSuccess("netty异步任务");
        });
        promise.addListener(new GenericFutureListener<Future<String>>() {
            @Override
            public void operationComplete(Future future) throws Exception {
                log.info("任务结束，balabala...");
            }
        });
        try {
            promise.sync();
//            promise.await();
        } catch (Exception e) {
            log.info("捕获到sync抛出的异常：{}",e);
        }
    }

    @Test
    public void test2(){
        log.info("{}",isPowerOfTwo(3));
    }

    /**
     * 3
     * 11
     *
     * 11 & 01
     */
    private static boolean isPowerOfTwo(int val) {
        int a = -val;
        int b = (val & a);
        return b == val;
    }

}
