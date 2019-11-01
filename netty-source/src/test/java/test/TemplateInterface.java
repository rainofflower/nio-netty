package test;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

/**
 * 使用接口复习模板模式
 */
public interface TemplateInterface<T> extends Callable<T> {

    Semaphore permit = new Semaphore(3);

    default void acquire(){
        permit.acquireUninterruptibly();
    }

    default void release(){
        permit.release();
    }

    default T handler() throws Exception{
        acquire();
        try{
            return call();
        }finally {
            release();
        }
    }
}
