import org.fastlight.fastaop.example.CtxAsserts;
import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.handler.FastAspectContext;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * 无 Package 的类测试
 *
 * @author ychost@outlook.com
 * @date 2021-03-29
 */
public class RootClassTest {
    @FastAspect
    @Test
    public void noPackage() {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        Method method = new Object() {}.getClass().getEnclosingMethod();
        CtxAsserts.assertEq(ctx, RootClassTest.class, method);
    }
}
