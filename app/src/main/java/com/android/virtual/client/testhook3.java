package com.android.virtual.client;

import com.android.virtual.client.hook.annotation.AutoHookMethod;
import com.android.virtual.client.hook.annotation.PluginmModule;
import com.android.virtual.client.hook.annotation.ThisObject;
import com.android.virtual.client.hook.model.AutoThisObject;

@PluginmModule(value = "xxx", processName = "zzz")
public class testhook3 {

    @AutoHookMethod(targetClass = testhook2.class, methodName = "z55666")
    public static String withinAnnotatedClass(AutoThisObject thiz, String X1, int xz2) throws Throwable {

        return "123";
    }

    @AutoHookMethod(targetClass = testhook2.class, methodName = "z556664")
    public String withinAnnotatedClass4(AutoThisObject thiz, String X1, int xz2) throws Throwable {

        return "123";
    }

    public void withinAXlass() {

    }
}
