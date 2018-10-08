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
import java.util.UUID;


public class JvnServerImpl
        extends UnicastRemoteObject
        implements JvnLocalServer, JvnRemoteServer {

    // A JVN server is managed as a singleton
    private static JvnServerImpl js = null;
    private static JvnRemoteCoord jvnCoord = null;
    private HashMap<String, JvnObject> store;
    private HashMap<Integer, String> internalIdLookupTable;
    private final UUID id;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        // to be completed
        store = new HashMap<>();
        internalIdLookupTable = new HashMap<>();
        id = UUID.randomUUID();

        Registry registry = LocateRegistry.getRegistry(1029);
        registry.bind(id.toString(), this);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    /**
     * Static method allowing an application to get a reference to
     * a JVN coordinator instance
     *
     * @throws JvnException
     **/
    public static JvnRemoteCoord jvnGetCoord() {
        if (jvnCoord == null) {
            try {
                Registry registry = LocateRegistry.getRegistry(1029);
                jvnCoord = (JvnRemoteCoord) registry.lookup("JvnCoord");
            } catch (Exception e) {
                System.err.println("JvnCoord exception: " + e.toString());
                e.printStackTrace();
            }
        }
        return jvnCoord;
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
                System.err.println("oops: " + e);
                e.printStackTrace();
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
        try {
            jvnGetCoord().jvnTerminate(jvnGetServer());
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
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
            objectId = jvnGetCoord().jvnGetObjectId();
        } catch (Exception e) {
            System.err.println("JvnCoord exception: int" + e.toString());
            e.printStackTrace();

            throw new jvn.JvnException("Error getting object id from JvnCoord");
        }

        JvnObjectImpl interceptionObject = new JvnObjectImpl(o, objectId, id);
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
        try {
            jvnGetCoord().jvnRegisterObject(jon, jo, jvnGetServer());
            store.put(jon, jo);
            internalIdLookupTable.put(jo.jvnGetObjectId(), jon);
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
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
        try {
            return jvnGetCoord().jvnLookupObject(jon, js);
        } catch (Exception e) {
            System.out.printf("Failed to lookup object with name \"%s\" from coordinator, falling back to local cache\n", jon);
            return store.get(jon);
        }
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
        try {
            return jvnGetCoord().jvnLockRead(joi, jvnGetServer());
        } catch (Exception e) {
            System.out.printf("Failed to get a read lock for object with id \"%d\" from coordinator\n", joi);
            return null;
        }
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
        try {
            return jvnGetCoord().jvnLockWrite(joi, jvnGetServer());
        } catch (Exception e) {
            System.out.printf("Failed to get a write lock for object with id \"%d\" from coordinator\n", joi);
            return null;
        }
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
        JvnObject jvnObject = store.get(internalIdLookupTable.get(joi));
        jvnObject.jvnInvalidateReader();
    }

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
        JvnObject jvnObject = store.get(internalIdLookupTable.get(joi));
        jvnObject.jvnInvalidateWriter();
        return jvnObject;
    }

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
        JvnObject jvnObject = store.get(internalIdLookupTable.get(joi));
        jvnObject.jvnInvalidateWriterForReader();
        return jvnObject;
    }

}

 
