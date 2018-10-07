/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;


public class JvnServerImpl
        extends UnicastRemoteObject
        implements JvnLocalServer, JvnRemoteServer {

    // A JVN server is managed as a singleton
    private static JvnServerImpl js = null;
    private HashMap<String, JvnObject> store;
    private JvnRemoteCoord jvnCoord;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        // to be completed
        store = new HashMap<>();
        try {
            Registry registry = LocateRegistry.getRegistry(1029);
            jvnCoord = (JvnRemoteCoord) registry.lookup("JvnCoord");
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Static method allowing an application to get a reference to
     * a JVN server instance
     *
     * @throws JvnException
     **/
    public static JvnServerImpl jvnGetServer() {
        if (js == null) {
            try {
                js = new JvnServerImpl();
            } catch (Exception e) {
                return null;
            }
        }
        return js;
    }

    /**
     * The JVN service is not used anymore
     *
     * @throws JvnException
     **/
    public void jvnTerminate()
            throws jvn.JvnException {
        // to be completed
    }

    /**
     * creation of a JVN object
     *
     * @param o : the JVN object state
     * @throws JvnException
     **/
    public JvnObject jvnCreateObject(Serializable o)
            throws jvn.JvnException {
        // to be completed
        int objectId;
        try {
            objectId = jvnCoord.jvnGetObjectId();
        } catch (Exception e) {
            System.err.println("JvnCoord exception: int" + e.toString());
            e.printStackTrace();

            throw new jvn.JvnException("Error getting object id from JvnCoord");
        }

        JvnObjectImpl interceptionObject = new JvnObjectImpl(o, objectId, jvnGetServer());
        interceptionObject.jvnLockWrite();

        return interceptionObject;
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     * @throws JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo)
            throws jvn.JvnException {
        // to be completed
    }

    /**
     * Provide the reference of a JVN object beeing given its symbolic name
     *
     * @param jon : the JVN object name
     * @return the JVN object
     * @throws JvnException
     **/
    public JvnObject jvnLookupObject(String jon)
            throws jvn.JvnException {
        // to be completed
        return store.get(jon);
    }

    /**
     * Get a Read lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockRead(int joi)
            throws JvnException {
        // to be completed
        return null;

    }

    /**
     * Get a Write lock on a JVN object
     *
     * @param joi : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockWrite(int joi)
            throws JvnException {
        // to be completed
        return null;
    }


    /**
     * Invalidate the Read lock of the JVN object identified by id
     * called by the JvnCoord
     *
     * @param joi : the JVN object id
     * @return void
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnInvalidateReader(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
    }

    ;

    /**
     * Invalidate the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriter(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        return null;
    }

    ;

    /**
     * Reduce the Write lock of the JVN object identified by id
     *
     * @param joi : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriterForReader(int joi)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        return null;
    }

    ;

}

 
