package com.android.virtual.client.hook.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoTargetApi {

    int value();

    // 最大值
    int maxValue() default -1;
}
