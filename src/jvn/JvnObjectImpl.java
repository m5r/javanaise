package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    transient private LockState lock;

    JvnObjectImpl(Serializable o, int objectId) throws JvnException {
        System.out.println("JvnObjectImpl.JvnObjectImpl");
        state = o;
        id = objectId;
    }

    /**
     * Get a Read lock on the object
     *
     * @throws JvnException
     **/
    public synchronized void jvnLockRead()
            throws jvn.JvnException {
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
            case WC:
                lock = LockState.W;
                break;
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

        try {
            notifyAll();
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
        jvnUnLock();
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
        jvnUnLock();
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
        while (lock == LockState.W) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        jvnUnLock();

        return state;
    }
}
