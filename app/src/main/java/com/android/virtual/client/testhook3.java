package com.android.virtual.client;

import com.android.virtual.client.hook.annotation.AutoHookMethod;
import com.android.virtual.client.hook.annotation.AutoHookMethodReflect;
import com.android.virtual.client.hook.annotation.PluginmModule;
import com.android.virtual.client.hook.annotation.ThisObject;
import com.android.virtual.client.hook.model.AutoThisObject;

@PluginmModule(value = "xxx", processName = "zzz")
public class testhook3 {

    @AutoHookMethod(value = testhook2.class, methodName = "z55666")
    public static String withinAnnotatedClass(AutoThisObject thiz, String X1, int xz2) throws Throwable {

        return "123";
    }

    @AutoHookMethod(testhook2.class)
    public String withinAnnotatedClass4(AutoThisObject thiz) throws Throwable {

        return "123";
    }

    @AutoHookMethodReflect("s366")
    public void withinAXlass(AutoThisObject thiz) throws Throwable {

    }
}
