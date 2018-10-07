package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
    private Serializable state;
    private int id;
    private JvnServerImpl jvnServer;

    JvnObjectImpl(Serializable o, int objectId, JvnServerImpl js) throws JvnException {
        state = o;
        id = objectId;
        jvnServer = js;
    }

    /**
     * Get a Read lock on the object
     *
     * @throws JvnException
     **/
    public void jvnLockRead()
            throws jvn.JvnException {
        jvnServer.jvnLockRead(id);
    }

    /**
     * Get a Write lock on the object
     *
     * @throws JvnException
     **/
    public void jvnLockWrite()
            throws jvn.JvnException {
        jvnServer.jvnLockWrite(id);
    }

    /**
     * Unlock  the object
     *
     * @throws JvnException
     **/
    public void jvnUnLock()
            throws jvn.JvnException {

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
    public void jvnInvalidateReader()
            throws jvn.JvnException {

    }

    /**
     * Invalidate the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnInvalidateWriter()
            throws jvn.JvnException {
        return null;
    }

    /**
     * Reduce the Write lock of the JVN object
     *
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnInvalidateWriterForReader()
            throws jvn.JvnException {
        return null;
    }
}
