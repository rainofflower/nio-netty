package test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class FinallyTest {

    /**
     * finally中的代码在每次要退出try块前都会被调用
     * 具体有下面几种情况
     * 1、try块中有return，执行try块中的return前，会先执行finally语句块，然后再执行try块中的return
     * 2、try块有异常抛出，先创建异常并执行抛出指令，然后执行finally语句块，等finally语句块执行完了之后再执行异常抛出之后的逻辑
     * 3、try块放在循环中，try语句块中有 continue 或 break 指令，
     * 在每次循环中，遇到continue或break会先执行finally语句块，然后执行continue或break。也就是说每次循环都是一个新的try,finally
     */
    @Test
    public void testFinally(){
        try{
            int i = 0;
            while(true){
                try{
                    ++i;
                    if(i == 1){
//                    return;
//                        throw new Exception("发生异常");
                    }
                    if(i == 2){
                        continue;
                    }
                    if(i == 3){
                        break;
                    }
                }finally {
                    log.info("执行finally...");
                }
            }
        }catch (Exception e){
            log.info("捕获异常：{}",e);
        }
    }
}
