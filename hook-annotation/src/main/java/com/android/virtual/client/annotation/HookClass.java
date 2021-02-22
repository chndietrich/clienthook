package com.android.virtual.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HookClass {
    Class<?> value();
    /**
     * @return 所在的组
     */
    String group() default "";
}
