package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    private LockState lock;

    JvnObjectImpl(Serializable o, int objectId) throws JvnException {
        state = o;
        id = objectId;
        lock = LockState.NoLock;
    }

    /**
     * Get a Read lock on the object
     *
     * @throws JvnException
     **/
    public void jvnLockRead()
            throws jvn.JvnException {
        if (lock != LockState.WC && lock != LockState.RC) {
            state = JvnServerImpl.jvnGetServer().jvnLockRead(id);
        }

        if (lock == LockState.WC) {
            lock = LockState.RWC;
        } else {
            lock = LockState.R;
        }
    }

    /**
     * Get a Write lock on the object
     *
     * @throws JvnException
     **/
    public void jvnLockWrite()
            throws jvn.JvnException {
//            switch
                // WC: W
                // RC, NL
        state = JvnServerImpl.jvnGetServer().jvnLockWrite(id);
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
            default:
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
    public int jvnGetObjectId()
            throws jvn.JvnException {
        return id;
    }

    /**
     * Get the object state
     *
     * @throws JvnException
     **/
    public Serializable jvnGetObjectState()
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
        while (lock != LockState.RC) {
            try {
                wait();
            } catch (Exception e) {
                System.err.println("JvnCoord exception: " + e.toString());
                e.printStackTrace();
            }
        }

        lock = LockState.NoLock;
    }

    /**
     * Invalidate the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public synchronized Serializable jvnInvalidateWriter()
            throws jvn.JvnException {
        while (lock != LockState.WC || lock != LockState.RWC) {
            try {
                wait();
            } catch (Exception e) {
                System.err.println("JvnCoord exception: " + e.toString());
                e.printStackTrace();
            }
        }

        lock = LockState.NoLock;
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
        while (lock != LockState.WC || lock != LockState.RWC) {
            try {
                wait();
            } catch (Exception e) {
                System.err.println("JvnCoord exception: " + e.toString());
                e.printStackTrace();
            }
        }

        if (lock == LockState.RWC) {
            lock = LockState.R;
        } else {
            lock = LockState.RC;
        }

        return state;
    }
}
