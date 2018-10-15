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

public class JvnCoordImpl
        extends UnicastRemoteObject
        implements JvnRemoteCoord {

    private HashMap<String, Integer> internalIdLookupTable;
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
     * @param jvnObjectName   : the JVN object name
     * @param jvnObject       : the JVN object
     * @param jvnRemoteServer : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public void jvnRegisterObject(String jvnObjectName, JvnObject jvnObject, JvnRemoteServer jvnRemoteServer)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        if (internalIdLookupTable.get(jvnObjectName) != null) {
            throw new jvn.JvnException(String.format("Object with name %s already registered", jvnObjectName));
        }

        try {
            internalIdLookupTable.put(jvnObjectName, jvnObject.jvnGetObjectId());
            store.put(jvnObject.jvnGetObjectId(), jvnObject);
            JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObject.jvnGetObjectId());
            jvnObjectLock.put(jvnRemoteServer, LockState.W);
            locks.put(jvnObject.jvnGetObjectId(), jvnObjectLock);
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Get the reference of a JVN object managed by a given JVN server
     *
     * @param jvnObjectName   : the JVN object name
     * @param jvnRemoteServer : the remote reference of the JVNServer
     * @throws java.rmi.RemoteException,JvnException
     **/
    public JvnObject jvnLookupObject(String jvnObjectName, JvnRemoteServer jvnRemoteServer)
            throws java.rmi.RemoteException, jvn.JvnException {
        // to be completed
        int joi = internalIdLookupTable.get(jvnObjectName);
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
     * @param jvnObjectId     : the JVN object identification
     * @param jvnRemoteServer : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockRead(int jvnObjectId, JvnRemoteServer jvnRemoteServer)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObjectId);

        if (jvnObjectLock.containsValue(LockState.W)) {
            JvnRemoteServer prevWriterJs = jvnObjectLock.entrySet().iterator().next().getKey();
            Serializable joState = jvnRemoteServer.jvnInvalidateWriterForReader(jvnObjectId);
            store.put(jvnObjectId, new JvnObjectImpl(joState, jvnObjectId));
            jvnObjectLock.put(prevWriterJs, LockState.R);
        }

        jvnObjectLock.put(jvnRemoteServer, LockState.R);

        return store.get(jvnObjectId).jvnGetObjectState();
    }

    /**
     * Get a Write lock on a JVN object managed by a given JVN server
     *
     * @param jvnObjectId     : the JVN object identification
     * @param jvnRemoteServer : the remote reference of the server
     * @return the current JVN object state
     * @throws java.rmi.RemoteException, JvnException
     **/
    public Serializable jvnLockWrite(int jvnObjectId, JvnRemoteServer jvnRemoteServer)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        System.out.println("JvnCoordImpl.jvnLockWrite");
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObjectId);

        if (jvnObjectLock.containsValue(LockState.W)) {
            System.out.println("on a un writer");
            JvnRemoteServer prevWriterJs = jvnObjectLock.entrySet().iterator().next().getKey();
            Serializable joState = prevWriterJs.jvnInvalidateWriter(jvnObjectId);
            store.put(jvnObjectId, new JvnObjectImpl(joState, jvnObjectId));
            jvnObjectLock.put(prevWriterJs, LockState.NL);
        }

        if (jvnObjectLock.containsValue(LockState.R)) {
            jvnObjectLock.entrySet().stream().forEach(entry -> {
                try {
                    JvnRemoteServer prevWriterJs = entry.getKey();
                    prevWriterJs.jvnInvalidateReader(jvnObjectId);
                    jvnObjectLock.put(prevWriterJs, LockState.NL);
                } catch (Exception e) {
                    System.err.println("JvnCoord exception: " + e.toString());
                    e.printStackTrace();
                }
            });
        }

        jvnObjectLock.put(jvnRemoteServer, LockState.W);

        return store.get(jvnObjectId).jvnGetObjectState();
    }

    /**
     * A JVN server terminates
     *
     * @param jvnRemoteServer : the remote reference of the server
     * @throws java.rmi.RemoteException, JvnException
     **/
    public void jvnTerminate(JvnRemoteServer jvnRemoteServer)
            throws java.rmi.RemoteException, JvnException {
        // to be completed
        locks.entrySet()
                .stream()
                .filter(entry -> entry.getValue().containsKey(jvnRemoteServer))
                .forEach(entry -> entry.setValue(null));
    }

    public static void main(String argv[]) {
        try {
            JvnRemoteCoord jvnCoord = new JvnCoordImpl();
            Registry registry = LocateRegistry.createRegistry(1029);
            registry.bind("JvnCoord", jvnCoord);

            System.err.println("Coordinator ready");

            setTimeout(() -> echo((JvnCoordImpl) jvnCoord), 10000);
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public HashMap<Integer, JvnObject> getStore() {
        return store;
    }

    public HashMap<String, Integer> getInternalIdLookupTable() {
        return internalIdLookupTable;
    }

    public HashMap<Integer, JvnObjectLock> getLocks() {
        return locks;
    }

    private static void echo(JvnCoordImpl jvnCoord) {
        System.out.println("state: " + jvnCoord.getStore().entrySet());
        System.out.println("lookup: " + jvnCoord.getInternalIdLookupTable().entrySet());
        System.out.println("locks: " + jvnCoord.getLocks().entrySet());
    }

    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }
}
