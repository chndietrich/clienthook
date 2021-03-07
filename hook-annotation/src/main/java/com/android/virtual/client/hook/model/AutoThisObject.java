package com.android.virtual.client.hook.model;

import java.lang.reflect.Method;

public interface AutoThisObject {
    Method getBackup();
    Object getThisObject();
    Object call(Object... args) throws Throwable;
}
