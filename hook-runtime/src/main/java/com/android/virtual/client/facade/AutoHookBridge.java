package com.android.virtual.client.facade;

import java.lang.reflect.Method;

public class AutoHookBridge {

    private static final AutoHookBridge mBridge = new AutoHookBridge();

    private Callback mCallback = new Callback(){};
    private AutoHookBridge(){}

    public static AutoHookBridge get() {
        return mBridge;
    }

    public interface Callback {
        default Object callOriginByBackup(Method backup, Object thiz, Object... args){ return null;}
    }

    public void setCallback(Callback callback){
        this.mCallback = callback;
    }

    public Object callOriginByBackup(Method backup, Object thiz, Object... args) throws Throwable {
        return this.mCallback.callOriginByBackup(backup, thiz, args);
    }
}
