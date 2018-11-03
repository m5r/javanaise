package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    private LockState lock;

    JvnObjectImpl(Serializable jvnObjectState, int objectId) throws JvnException {
        System.out.println("JvnObjectImpl.JvnObjectImpl");
        state = jvnObjectState;
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
        System.out.println("JvnObjectImpl.jvnLockRead");
        System.out.println(this.lock);
        switch (lock) {
            case W:
            case WC:
                lock = LockState.RWC;
                break;
            case R:
            case RC:
                lock = LockState.R;
                break;
            case NL:
                state = JvnServerImpl.jvnGetServer().jvnLockRead(id);
                lock = LockState.R;
                break;
        }
    }

    /**
     * Get a Write lock on the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnLockWrite()
            throws jvn.JvnException {
        if (this.lock == null) {
            this.lock = LockState.NL;
        }

        switch (lock) {
            case W:
            case WC:
            case RWC:
                lock = LockState.W;
                break;
            case R:
            case RC:
            case NL:
                state = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
                lock = LockState.W;
                break;
        }
    }

    /**
     * Unlock  the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnUnLock()
            throws jvn.JvnException {
        switch (lock) {
            case RC:
            case WC:
                lock = LockState.NL;
                break;
            case R:
                lock = LockState.RC;
                break;
            case W:
            case RWC:
                lock = LockState.WC;
                break;
        }

        notifyAll();
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
        System.out.println("JvnObjectImpl.jvnInvalidateReader");
        System.out.println("    before jvnInvalidateReader: " + lock);
        while (lock == LockState.R || lock == LockState.RWC) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        lock = LockState.NL;
        System.out.println("    after jvnInvalidateReader: " + lock);
    }

    /**
     * Invalidate the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public synchronized Serializable jvnInvalidateWriter()
            throws jvn.JvnException {
        System.out.println("JvnObjectImpl.jvnInvalidateWriter");
        System.out.println("    before jvnInvalidateWriter: " + lock);
        while (lock == LockState.W) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        lock = LockState.NL;
        System.out.println("    after jvnInvalidateWriter: " + lock);

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
        System.out.println("JvnObjectImpl.jvnInvalidateWriterForReader");
        System.out.println("    before invalidateWriterForReader: " + lock);

        while (lock == LockState.W) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        switch (lock) {
            case RWC:
                lock = LockState.R;
            case R:
            case WC:
                lock = LockState.RC;
        }

        System.out.println("    after invalidateWriterForReader: " + lock);

        return state;
    }
}
