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
import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public class JvnCoordImpl
        extends UnicastRemoteObject
        implements JvnRemoteCoord {

    private HashMap<JvnRemoteServer, HashMap<String, Integer>> internalIdLookupTable;
    private HashMap<Integer, JvnObject> store;
    private HashMap<Integer, JvnObjectLock> locks;
    private int objectCount;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnCoordImpl() throws Exception {
        // to be completed
        internalIdLookupTable = new HashMap<>();
        store = new HashMap<>();
        locks = new HashMap<>();
        objectCount = 0;
    }

    /**
     * Allocate a NEW JVN object id (usually allocated to a
     * newly created JVN object)
     *
     * @throws java.rmi.RemoteException,JvnException
     **/
    public int jvnGetObjectId()
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        return objectCount++;
    }

    /**
     * Associate a symbolic name with a JVN object
     *
     * @param jon : the JVN object name
     * @param jo  : the JVN object
     *            //     * @param joi : the JVN object identification
     *            //     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        HashMap<String, Integer> jsObjectsLookupTable = internalIdLookupTable.computeIfAbsent(js, n -> new HashMap<>());

        if (jsObjectsLookupTable.get(jon) != null) {
            throw new jvn.JvnException(String.format("Object with name %s already registered", jon));
        }

        try {
            jsObjectsLookupTable.put(jon, jo.jvnGetObjectId());
            store.put(jo.jvnGetObjectId(), jo);
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     *
     * @param jon : the JVN object name
     * @param js  : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        HashMap<String, Integer> jsObjectsLookupTable = internalIdLookupTable.computeIfAbsent(js, n -> new HashMap<>());
        int joi = jsObjectsLookupTable.get(jon);
        return store.get(joi);
    }

    private JvnObjectLock getJvnObjectLockFromId(int joi) {
        JvnObjectLock jvnObjectLock = locks.get(joi);

        if (jvnObjectLock == null) {
            jvnObjectLock = new JvnObjectLock();
            locks.put(joi, jvnObjectLock);
        }

        return jvnObjectLock;
    }

    /**
     * Get a Read lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(joi);
        UUID jsId = UUID.fromString(js.toString());

        if (jvnObjectLock.containsValue(LockState.W)) {
            UUID prevWriterJsId = jvnObjectLock.entrySet().iterator().next().getKey();
            js.jvnInvalidateWriterForReader(joi);
            jvnObjectLock.put(prevWriterJsId, LockState.R);
        }

        jvnObjectLock.put(jsId, LockState.R);

        return store.get(joi);
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     *
     * @param joi : the JVN object identification
     * @param js  : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(joi);
        UUID jsId = UUID.fromString(js.toString());

        if (jvnObjectLock.containsValue(LockState.W)) {
            UUID prevWriterJsId = jvnObjectLock.entrySet().iterator().next().getKey();
            try {
                Registry registry = LocateRegistry.getRegistry(1029);
                JvnRemoteServer jvnServer = (JvnRemoteServer) registry.lookup(prevWriterJsId.toString());
                jvnServer.jvnInvalidateWriter(joi);
                jvnObjectLock.put(prevWriterJsId, null);
            } catch (Exception e) {
                System.err.println("JvnCoord exception: " + e.toString());
                e.printStackTrace();
            }
        }

        if (jvnObjectLock.containsValue(LockState.R)) {
            jvnObjectLock.entrySet().stream().forEach(entry -> {
                try {
                    UUID prevWriterJsId = entry.getKey();
                    Registry registry = LocateRegistry.getRegistry(1029);
                    JvnRemoteServer jvnServer = (JvnRemoteServer) registry.lookup(prevWriterJsId.toString());
                    jvnServer.jvnInvalidateReader(joi);
                    jvnObjectLock.put(prevWriterJsId, null);
                } catch (Exception e) {
                    System.err.println("JvnCoord exception: " + e.toString());
                    e.printStackTrace();
                }
            });
        }

        jvnObjectLock.put(jsId, LockState.W);

        return store.get(joi);
    }

    /**
     * A JVN server terminates
     *
     * @param js : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public void jvnTerminate(JvnRemoteServer js)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        locks.entrySet()
                .stream()
                .filter(entry -> entry.getValue().containsKey(UUID.fromString(js.toString())))
                .forEach(entry -> entry.setValue(null));
    }

    public static void main(String argv[]) {
        try {
            JvnRemoteCoord jvnCoord = new JvnCoordImpl();
            Registry registry = LocateRegistry.createRegistry(1029);
            registry.bind("JvnCoord", jvnCoord);

            System.err.println("Coordinator ready");
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
