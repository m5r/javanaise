package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JvnProxy implements InvocationHandler {

    private JvnObject jvnObject;

    private JvnProxy(String jvnObjectName, Object jvnObjectState) {
        try {
            JvnServerImpl jvnServer = JvnServerImpl.jvnGetServer();
            JvnObject jvnObject = jvnServer.jvnLookupObject(jvnObjectName);

            if (jvnObject == null) {
                jvnObject = jvnServer.jvnCreateObject((Serializable) jvnObjectState);
                jvnObject.jvnUnLock();
                jvnServer.jvnRegisterObject(jvnObjectName, jvnObject);
            }

            this.jvnObject = jvnObject;
        } catch (Exception e) {
            System.out.println("JvnProxy problem : " + e.getMessage());
        }
    }

    public static Object get(String jvnObjectName, Object jvnObjectState) throws JvnException {
        return Proxy.newProxyInstance(
                jvnObjectState.getClass().getClassLoader(),
                jvnObjectState.getClass().getInterfaces(),
                new JvnProxy(jvnObjectName, jvnObjectState)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(JvnObjectInterceptor.class)) {
            JvnObjectInterceptor lockType = method.getAnnotation(JvnObjectInterceptor.class);
            switch(lockType.lockType()) {
                case read:
                    jvnObject.jvnLockRead();
                    break;
                case write:
                    jvnObject.jvnLockWrite();
                    break;
            }
        } else {
            System.err.println("Method without annotation, falling back to a write lock");
            jvnObject.jvnLockWrite();
        }

        Object methodResult = method.invoke(jvnObject.jvnGetObjectState(), args);
        jvnObject.jvnUnLock();

        return methodResult;
    }

}
