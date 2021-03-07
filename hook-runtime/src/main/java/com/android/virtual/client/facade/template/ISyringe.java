package com.android.virtual.client.facade.template;

/**
 * ISyringe
 *
 * @author 27216
 * @since 2021/3/7 7:55
 **/

public interface ISyringe<T> {
    /**
     * 依赖注入
     * @param target 依赖注入的目标
     */
    void inject(T target);
}
