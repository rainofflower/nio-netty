package test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class HandlerTemplate implements TemplateInterface<String> {

    public String call() {
        String r = Thread.currentThread().getName()+" 查询数据...";
        log.info(r);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        String name = Thread.currentThread().getName();
        log.info(name+" 获取到数据 "+name.substring(2));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        return r;
    }

    @Test
    public void test(){
        for(int i = 0; i<7; i++){
            new Thread(()->{
                try {
                    new HandlerTemplate().handler();
                } catch (Exception e) {}
            },"线程"+i).start();
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Long.MAX_VALUE));
    }
}
