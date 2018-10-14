package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    private LockState lock;

    JvnObjectImpl(Serializable o, int objectId) throws JvnException {
        state = o;
        id = objectId;
        lock = LockState.W;
    }

    /**
     * Get a Read lock on the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnLockRead()
            throws jvn.JvnException {
        /*if (lock != LockState.WC && lock != LockState.RC) {
            state = JvnServerImpl.jvnGetServer().jvnLockRead(id);
        }

        if (lock == LockState.WC) {
            lock = LockState.RWC;
        } else {
            lock = LockState.R;
        }*/

        switch (lock) {
            case WC:
                lock = LockState.RWC;
                break;
            case RC:
            case NL:
                lock = LockState.R;
                break;
        }

        state = JvnServerImpl.jvnGetServer().jvnLockRead(id);
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
        System.out.println("state");
        System.out.println(state);
        return state;
    }


    /**
     * Invalidate the Read lock of the JVN object
     *
     * @throws JvnException
     **/
    public synchronized void jvnInvalidateReader()
            throws jvn.JvnException {
        boolean isReading = lock == LockState.R || lock == LockState.RWC;
        boolean isWriting = lock == LockState.W;
        boolean waitingCondition = isReading || isWriting;
        while (waitingCondition) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        lock = LockState.NL;
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
