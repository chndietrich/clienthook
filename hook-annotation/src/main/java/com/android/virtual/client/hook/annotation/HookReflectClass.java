package com.android.virtual.client.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HookReflectClass {
    String value();
    /**
     * @return 所在的组
     */
    String group() default "";
}
