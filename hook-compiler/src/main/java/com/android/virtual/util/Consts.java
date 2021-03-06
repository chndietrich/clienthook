package com.android.virtual.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 常量
 * @author 德友
 */
public class Consts {
    // Generate
    public static final String SEPARATOR = "$$";
    public static final String PROJECT = "XHook";
    public static final String TAG = PROJECT + "::";

    // Log
    static final String PREFIX_OF_LOGGER = PROJECT + "::HookCompiler ";

    // Options of processor
    public static final String KEY_MODULE_NAME = "moduleName";

    /**
     * 页面配置所在的包名
     */
    public static final String PAGE_CONFIG_PACKAGE_NAME = "com.android.virtual.client";

    public static final String PAGE_CONFIG_CLASS_NAME_SUFFIX = "HookConfig";

    public static final String PAGE_CONFIG_CLASS_NAME_SUFFIX_AUTO = "AutoHookConfig";

    public static final String METHOD_INJECT = "inject";

    public static final String METHOD_GET_HOOK_CLASS = "getHookClass";

    public static final String ISYRINGE = PAGE_CONFIG_PACKAGE_NAME + ".facade.template.ISyringe";

    public static final String METHODBACKUP = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.HookMethodBackup";

    public static final String METHODPARAMS = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.MethodParams";

    public static final String METHOD = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.HookMethod";

    public static final String CLASS = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.HookClass";

    public static final String REFLECTCLASS = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.HookReflectClass";

    public static final String AUTOTHISOBJECT = PAGE_CONFIG_PACKAGE_NAME + ".hook.model.AutoThisObject";

    public static final String THISOBJECT = PAGE_CONFIG_PACKAGE_NAME + ".hook.annotation.ThisObject";

    public static final String HOOKBRIDGE = PAGE_CONFIG_PACKAGE_NAME + ".facade.AutoHookBridge";

    public static final String WARNING_TIPS = "DO NOT EDIT THIS FILE!!! IT WAS GENERATED BY XROUTER.";

    /**
     * 首字母大写
     *
     * @param s 待转字符串
     * @return 首字母大写字符串
     */
    public static String upperFirstLetter(final String s) {
        if (StringUtils.isEmpty(s) || !Character.isLowerCase(s.charAt(0))) {
            return s;
        }
        return (char) (s.charAt(0) - 32) + s.substring(1);
    }
}
