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
    private HashMap<Integer, JvnObject> jvnObjects;
    private HashMap<Integer, JvnObjectLock> jvnObjectLocks;
    private int objectCount;

    /**
     * Default constructor
     *
     * @throws JvnException
     **/
    private JvnCoordImpl() throws Exception {
        // to be completed
        internalIdLookupTable = new HashMap<>();
        jvnObjects = new HashMap<>();
        jvnObjectLocks = new HashMap<>();
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
            jvnObjects.put(jvnObject.jvnGetObjectId(), jvnObject);
            JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObject.jvnGetObjectId());
            jvnObjectLock.put(jvnRemoteServer, LockState.W);
            jvnObjectLocks.put(jvnObject.jvnGetObjectId(), jvnObjectLock);
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
        int jvnObjectId = internalIdLookupTable.get(jvnObjectName);
        jvnLockRead(jvnObjectId, jvnRemoteServer);
        return jvnObjects.get(jvnObjectId);
    }

    private JvnObjectLock getJvnObjectLockFromId(int joi) {
        JvnObjectLock jvnObjectLock = jvnObjectLocks.get(joi);

        if (jvnObjectLock == null) {
            jvnObjectLock = new JvnObjectLock();
            jvnObjectLocks.put(joi, jvnObjectLock);
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

        System.out.println("server has write lock: " + (jvnObjectLock.get(jvnRemoteServer) == LockState.W));

        if (jvnObjectLock.containsValue(LockState.W)) {
            JvnRemoteServer prevWriterJvnServer = jvnObjectLock.entrySet().iterator().next().getKey();
            System.out.println("server really has write lock: " + (jvnRemoteServer == prevWriterJvnServer));
            Serializable jvnObjectState = jvnRemoteServer.jvnInvalidateWriterForReader(jvnObjectId);
            jvnObjects.put(jvnObjectId, new JvnObjectImpl(jvnObjectState, jvnObjectId));
            jvnObjectLock.put(prevWriterJvnServer, LockState.R);
        }

        jvnObjectLock.put(jvnRemoteServer, LockState.R);

        return jvnObjects.get(jvnObjectId).jvnGetObjectState();
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
            JvnRemoteServer prevWriterJvnServer = jvnObjectLock.entrySet().iterator().next().getKey();
            Serializable joState = prevWriterJvnServer.jvnInvalidateWriter(jvnObjectId);
            jvnObjects.put(jvnObjectId, new JvnObjectImpl(joState, jvnObjectId));
            jvnObjectLock.put(prevWriterJvnServer, LockState.NL);
        }

        if (jvnObjectLock.containsValue(LockState.R)) {
            jvnObjectLock.entrySet().stream().forEach(entry -> {
                try {
                    JvnRemoteServer prevWriterJvnServer = entry.getKey();
                    prevWriterJvnServer.jvnInvalidateReader(jvnObjectId);
                    jvnObjectLock.put(prevWriterJvnServer, LockState.NL);
                } catch (Exception e) {
                    System.err.println("JvnCoord exception: " + e.toString());
                    e.printStackTrace();
                }
            });
        }

        jvnObjectLock.put(jvnRemoteServer, LockState.W);

        return jvnObjects.get(jvnObjectId).jvnGetObjectState();
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
        jvnObjectLocks.entrySet()
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

    public HashMap<Integer, JvnObject> getJvnObjects() {
        return jvnObjects;
    }

    public HashMap<String, Integer> getInternalIdLookupTable() {
        return internalIdLookupTable;
    }

    public HashMap<Integer, JvnObjectLock> getJvnObjectLocks() {
        return jvnObjectLocks;
    }

    private static void echo(JvnCoordImpl jvnCoord) {
        System.out.println("state: " + jvnCoord.getJvnObjects().entrySet());
        System.out.println("lookup: " + jvnCoord.getInternalIdLookupTable().entrySet());
        System.out.println("jvnObjectLocks: " + jvnCoord.getJvnObjectLocks().entrySet());
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
