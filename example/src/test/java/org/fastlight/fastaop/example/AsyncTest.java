package org.fastlight.fastaop.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.apt.model.MetaMethod;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ychost@outlook.com
 * @date 2021-04-06
 */
@FastAspect
public class AsyncTest {
    @Test
    public void test() throws ExecutionException, InterruptedException {
        doAsync();
        doInMainThread();
        Future<String> future = getFuture();
        String str = future.get();
        Assert.assertEquals("hello", str);
        assertMainThread(true);
    }

    @Async
    public Future<String> getFuture() {
        assertMainThread(false);
        return new AsyncResult<>("hello");
    }

    @Async
    public void doAsync() {
        // 判断线程
        assertMainThread(false);
    }

    public void doInMainThread() {
        assertMainThread(true);
    }

    void assertMainThread(boolean isMainThread) {
        Assert.assertEquals(isMainThread, Thread.currentThread().getName().contains("main"));
    }

    /**
     * 异步注解
     */
    @Target(ElementType.METHOD)
    public static @interface Async {

    }

    @FastAspectAround
    public static class AsyncHandler implements FastAspectHandler {

        private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(8);

        @Override
        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(Async.class);
        }

        /**
         * 模仿 Spring 的 Async 写的
         */
        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            if (Future.class.isAssignableFrom(ctx.getMetaMethod().getReturnType())) {
                FutureTask<?> futureTask = new FutureTask<>(() -> ((Future<?>)ctx.proceed()).get());
                EXECUTOR_SERVICE.execute(futureTask);
                return futureTask;
            } else {
                EXECUTOR_SERVICE.execute(ctx::invoke);
                return null;
            }
        }
    }

    public static class AsyncResult<T> implements Future<T> {
        private Object result = null;

        public AsyncResult(Object data) {
            result = data;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return (T)result;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return (T)result;
        }
    }
}
