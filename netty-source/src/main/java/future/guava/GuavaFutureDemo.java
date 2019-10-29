package future.guava;

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import util.CompositeThreadPoolConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Guava 异步回调
 *
 * MoreExecutors.listeningDecorator(ExecutorService pool)返回ListeningExecutorService
 * 向该修饰后的线程池提交Callable实例，会返回ListenableFuture实例。
 * 使用Futures.addCallback(
 *       final ListenableFuture<V> future,
 *       final FutureCallback<? super V> callback,
 *       Executor executor)
 *  第一个参数传入线程池返回的ListenableFuture实例，
 *  第二个参数传入一个FutureCallback实例（实现onSuccess()和onFailure()方法），
 *  线程池执行完提交的Callback之后就会在第三个参数指定的线程池中回调FutureCallback实例中的方法
 */
@Slf4j
public class GuavaFutureDemo {

    volatile boolean waterOk;
    volatile boolean cupOk;

    @Test
    public void daily(){
        ThreadPoolExecutor pool = new CompositeThreadPoolConfig().threadPoolExecutor();
        ListeningExecutorService gPool = MoreExecutors.listeningDecorator(pool);
        ListenableFuture<Boolean> hotWaterFuture = gPool.submit(new HotWater());
        Futures.addCallback(hotWaterFuture, new FutureCallback<Boolean>() {
            public void onSuccess(@Nullable Boolean result) {
                if(result){
                    log.info("水开了");
                    waterOk = true;
                }
            }
            public void onFailure(Throwable t) {
                //
            }
        },gPool);
        ListenableFuture<Boolean> washFuture = gPool.submit(new Wash());
        Futures.addCallback(washFuture, new FutureCallback<Boolean>() {
            public void onSuccess(@Nullable Boolean result) {
                if(result){
                    log.info("洗完了");
                    cupOk = true;
                }
            }
            public void onFailure(Throwable t) {
                //
            }
        },gPool);
        while(true){
            log.info("学习中...");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            if(waterOk && cupOk){
                log.info("泡茶喝，茶喝完");
                return;
            }
            else if(!waterOk){
                log.info("烧水失败,没茶喝了");
            }
            else if(!cupOk){
                log.info("杯子洗不了，没茶喝了");
            }
        }
    }

    static class HotWater implements Callable<Boolean> {

        public Boolean call() throws Exception {
            try{
                log.info("加水，启壶，烧水...");
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
            }catch (Exception e){
                return false;
            }
            return true;
        }
    }

    static class Wash implements Callable<Boolean>{

        public Boolean call() throws Exception {
            try{
                log.info("洗茶壶，洗茶杯...");
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
            }catch (Exception e){
                return false;
            }
            return true;
        }
    }
}
