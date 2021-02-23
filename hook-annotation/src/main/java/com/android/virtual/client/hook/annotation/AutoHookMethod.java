package com.android.virtual.client.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实现自动hook（HookMethod）的注解
 *
 * @author 德友
 * @since 2021年2月22日 18:12:25
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoHookMethod {
    String value() default "";
}
