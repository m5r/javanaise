package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    private LockState lock;

    JvnObjectImpl(Serializable o, int objectId) throws JvnException {
        state = o;
        id = objectId;
        lock = LockState.NL;
    }

    /**
     * Get a Read lock on the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnLockRead()
            throws jvn.JvnException {
        System.out.println("object.jvnLockRead");
        System.out.println("current lock is: " + lock);
        switch (lock) {
            case WC:
                lock = LockState.RWC;
                break;
            case RC:
                lock = LockState.R;
                break;
            case NL:
                state = JvnServerImpl.jvnGetServer().jvnLockRead(id);
                lock = LockState.R;
                break;
        }
        System.out.println("new lock is: " + lock);
    }

    /**
     * Get a Write lock on the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnLockWrite()
            throws jvn.JvnException {
//            switch
                // WC: W
                // RC, NL
        if (lock != LockState.WC) {
            state = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
        }

        lock = LockState.W;
    }

    /**
     * Unlock  the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnUnLock()
            throws jvn.JvnException {
        switch (lock) {
            case R:
                lock = LockState.RC;
                break;
            case W:
            case RWC:
                lock = LockState.WC;
                break;
        }

        try {
            notify();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Get the object identification
     *
     * @throws JvnException
     **/
    public synchronized int jvnGetObjectId()
            throws jvn.JvnException {
        return id;
    }

    /**
     * Get the object state
     *
     * @throws JvnException
     **/
    public synchronized Serializable jvnGetObjectState()
            throws jvn.JvnException {
        return state;
    }


    /**
     * Invalidate the Read lock of the JVN object
     *
     * @throws JvnException
     **/
    public synchronized void jvnInvalidateReader()
            throws jvn.JvnException {
        while (lock == LockState.R || lock == LockState.RWC) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("finally invalidate");
        lock = LockState.NL;
    }

    /**
     * Invalidate the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public synchronized Serializable jvnInvalidateWriter()
            throws jvn.JvnException {
        while (lock == LockState.W) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        lock = LockState.NL; //TODO wait
        return state;
    }

    /**
     * Reduce the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public synchronized Serializable jvnInvalidateWriterForReader()
            throws jvn.JvnException {
        switch (lock) {
            case RWC:
                lock = LockState.R;
                break;
            case W:
            case WC:
                lock = LockState.RC;
                break;
        }

        return state;
    }
}
