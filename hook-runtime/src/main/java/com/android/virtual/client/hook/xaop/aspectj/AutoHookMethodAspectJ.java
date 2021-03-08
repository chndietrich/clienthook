package com.android.virtual.client.hook.xaop.aspectj;

import com.android.virtual.client.hook.annotation.AutoHookMethod;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * <pre>
 *     desc   : 埋点hook
 *     author : 德友
 *     time   : 2021年2月22日 18:14:06
 * </pre>
 */
//@Aspect
public class AutoHookMethodAspectJ {

    @Pointcut("within(@com.android.virtual.client.hook.annotation.AutoHookMethod *)")
    public void withinAnnotatedClass() {
    }

    @Pointcut("execution(!synthetic * *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType() {
    }

    @Pointcut("execution(!synthetic *.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType() {
    }

    @Pointcut("execution(@com.android.virtual.client.hook.annotation.AutoHookMethod * *(..)) || methodInsideAnnotatedType()")
    public void method() {
    } //方法切入点

    @Pointcut("execution(@com.android.virtual.client.hook.annotation.AutoHookMethod *.new(..)) || constructorInsideAnnotatedType()")
    public void constructor() {
    } //构造器切入点

    @Around("(method() || constructor()) && @annotation(autoHookMethod)")
    public Object HookMethodAndExecute(ProceedingJoinPoint joinPoint, AutoHookMethod autoHookMethod) throws Throwable {

        return "6666";
    }
}
