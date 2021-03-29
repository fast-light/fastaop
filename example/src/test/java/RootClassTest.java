import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.junit.Assert;
import org.junit.Test;

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
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getMetaMethod().getMetaOwner().getType(), RootClassTest.class);
    }
}
