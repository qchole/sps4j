package io.github.sps4j.core.load;

import io.github.sps4j.common.utils.CallUtils;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A ByteBuddy interceptor that sets the correct thread context class loader before invoking a plugin method.
 * This ensures that the plugin's own class loader is used during the method execution.
 *
 * @author Allan-QLB
 */
public class PluginMethodInvocationInterceptor {

    /**
     * Intercepts a method call on a plugin instance.
     * It sets the thread context class loader to the plugin's class loader, invokes the original method,
     * and then restores the original context class loader.
     *
     * @param proxy The proxied plugin instance.
     * @param superMethod The original method that was invoked.
     * @param args The arguments passed to the method.
     * @return The result of the original method invocation.
     * @throws Throwable The exception thrown by the original method.
     */
    @RuntimeType
    public Object intercept(@This Object proxy, @SuperMethod Method superMethod, @AllArguments Object[] args) throws Throwable {
        try {
            return CallUtils.executeWithContextLoader(proxy.getClass().getClassLoader(), () -> superMethod.invoke(proxy, args));
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }

}
