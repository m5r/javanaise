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
import java.util.*;

public class JvnServerImpl
        extends UnicastRemoteObject
        implements JvnLocalServer, JvnRemoteServer {

    // A JVN server is managed as a singleton
    private static JvnServerImpl jvnServer = null;
    private static JvnRemoteCoord jvnCoord = null;
    private HashMap<Integer, JvnObject> jvnObjects;
    private HashMap<String, Integer> internalIdLookupTable;

    private final int CACHE_LIMIT = 10;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnServerImpl() throws Exception {
        super();
        jvnObjects = new HashMap<>();
        internalIdLookupTable = new HashMap<>();
    }

    /**
     * Static method allowing an application to get a reference to
     * a JVN coordinator instance
     *
     * @throws JvnException
     **/
    private static JvnRemoteCoord jvnGetCoord() {
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
        if (jvnServer == null) {
            try {
                jvnServer = new JvnServerImpl();
            } catch (Exception e) {
                System.err.println("oops: " + e);
                e.printStackTrace();
                return null;
            }
        }
        return jvnServer;
    }

    /**
     * The JVN service is not used anymore
     *
     * @throws JvnException
     **/
    public void jvnTerminate()
            throws jvn.JvnException {
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
     * @param jvnObjectState : the JVN object state
     * @throws JvnException
     **/
    public JvnObject jvnCreateObject(Serializable jvnObjectState)
            throws jvn.JvnException {
        try {
            return new JvnObjectImpl(jvnObjectState, jvnGetCoord().jvnGetObjectId());
        } catch (Exception e) {
            System.err.println("JvnCoord exception: int" + e.toString());
            e.printStackTrace();

            throw new jvn.JvnException("JvnServerImpl.jvnCreateObject error: " + e.getMessage());
        }
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jvnObjectName : the JVN object name
     * @param jvnObject     : the JVN object
     * @throws JvnException
     **/
    public void jvnRegisterObject(String jvnObjectName, JvnObject jvnObject)
            throws jvn.JvnException {
        try {
            jvnGetCoord().jvnRegisterObject(jvnObjectName, jvnObject, jvnGetServer());
            insertJvnObject(jvnObjectName, jvnObject);
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Provide the reference of a JVN object beeing given its symbolic name
     *
     * @param jvnObjectName : the JVN object name
     * @return the JVN object
     * @throws JvnException
     **/
    public JvnObject jvnLookupObject(String jvnObjectName)
            throws jvn.JvnException {
        Integer localJvnObjectId = internalIdLookupTable.get(jvnObjectName);
        if (localJvnObjectId != null) {
            JvnObject jvnObject = jvnObjects.get(localJvnObjectId);
            return jvnObject;
        }

        try {
            JvnObject jvnObject = jvnGetCoord().jvnLookupObject(jvnObjectName, jvnGetServer());

            if (jvnObject != null) {
                insertJvnObject(jvnObjectName, jvnObject);
            }

            return jvnObject;
        } catch (Exception e) {
            System.out.printf("Failed to lookup object with name \"%s\" from coordinator\n", jvnObjectName);
            return null;
        }
    }

    /**
     * Get a Read lock on a JVN object
     *
     * @param jvnObjectId : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockRead(int jvnObjectId)
            throws JvnException {
        try {
            return jvnGetCoord().jvnLockRead(jvnObjectId, jvnGetServer());
        } catch (Exception e) {
            System.out.printf("Failed to get a read lock for object with id \"%d\" from coordinator, falling back to local cache\n", jvnObjectId);
            return jvnObjects.get(jvnObjectId).jvnGetObjectState();
        }
    }

    /**
     * Get a Write lock on a JVN object
     *
     * @param jvnObjectId : the JVN object identification
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnLockWrite(int jvnObjectId)
            throws JvnException {
        try {
            return jvnGetCoord().jvnLockWrite(jvnObjectId, jvnGetServer());
        } catch (Exception e) {
            System.out.printf("Failed to get a write lock for object with id \"%d\" from coordinator, falling back to local cache\n", jvnObjectId);
            return jvnObjects.get(jvnObjectId).jvnGetObjectState();
        }
    }

    /**
     * Invalidate the Read lock of the JVN object identified by id
     * called by the JvnCoord
     *
     * @param jvnObjectId : the JVN object id
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnInvalidateReader(int jvnObjectId)
            throws java.rmi.RemoteException, jvn.JvnException {
        JvnObject jvnObject = jvnObjects.get(jvnObjectId);

        if (jvnObject == null) {
            throw new JvnException("Failed to find jvnObject with id \"" + jvnObjectId + "\"on local server");
        }

        jvnObject.jvnInvalidateReader();
    }

    /**
     * Invalidate the Write lock of the JVN object identified by id
     *
     * @param jvnObjectId : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriter(int jvnObjectId)
            throws java.rmi.RemoteException, jvn.JvnException {
        JvnObject jvnObject = jvnObjects.get(jvnObjectId);

        if (jvnObject == null) {
            throw new JvnException("Failed to find jvnObject with id \"" + jvnObjectId + "\"on local server");
        }

        return jvnObject.jvnInvalidateWriter();
    }

    /**
     * Reduce the Write lock of the JVN object identified by id
     *
     * @param jvnObjectId : the JVN object id
     * @return the current JVN object state
     * @throws java.rmi.RemoteException,JvnException
     **/
    public Serializable jvnInvalidateWriterForReader(int jvnObjectId)
            throws java.rmi.RemoteException, jvn.JvnException {
        JvnObject jvnObject = jvnObjects.get(jvnObjectId);

        if (jvnObject == null) {
            throw new JvnException("Failed to find jvnObject with id \"" + jvnObjectId + "\"on local server");
        }

        return jvnObject.jvnInvalidateWriterForReader();
    }

    private void insertJvnObject(String jvnObjectName, JvnObject jvnObject) throws jvn.JvnException {
        if (jvnObjects.size() > CACHE_LIMIT) {
            JvnObject oldestJvnObject = jvnObjects.entrySet().stream().min(
                    (jvnObject1, jvnObject2) -> ((JvnObjectImpl) jvnObject1).getLastAccess().compareTo(((JvnObjectImpl) jvnObject2).getLastAccess())
            ).get().getValue();
            jvnObjects.remove(oldestJvnObject.jvnGetObjectId());
        }

        int jvnObjectId = jvnObject.jvnGetObjectId();

        jvnObjects.put(jvnObjectId, jvnObject);
        internalIdLookupTable.put(jvnObjectName, jvnObjectId);
    }
}
