/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.io.*;
import java.lang.reflect.Field;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class JvnCoordImpl
        extends UnicastRemoteObject
        implements JvnRemoteCoord {

    private final String cacheFolder = "/var/tmp/javanaise";
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
        internalIdLookupTable = new HashMap<>();
        jvnObjects = new HashMap<>();
        jvnObjectLocks = new HashMap<>();
        objectCount = 0;

        setStateFromBackup();
    }

    /**
     * Allocate a NEW JVN object id (usually allocated to a
     * newly created JVN object)
     *
     * @throws java.rmi.RemoteException,JvnException
     **/
    public int jvnGetObjectId()
            throws java.rmi.RemoteException, jvn.JvnException {
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

        this.backupState();
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
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObjectId);
        boolean requesterHasWriteLock = jvnObjectLock.get(jvnRemoteServer) == LockState.W;

        if (!requesterHasWriteLock) {
            if (jvnObjectLock.containsValue(LockState.W)) {
                JvnRemoteServer prevWriterJvnServer = jvnObjectLock.entrySet().stream().filter(e -> e.getValue() == LockState.W).iterator().next().getKey();
                try {
                    Serializable jvnObjectState = prevWriterJvnServer.jvnInvalidateWriterForReader(jvnObjectId);
                    jvnObjects.put(jvnObjectId, new JvnObjectImpl(jvnObjectState, jvnObjectId));
                    jvnObjectLock.put(prevWriterJvnServer, LockState.R);
                } catch (Exception e) {
                    System.err.println("Error invalidating writer: " + e.toString());
                    jvnTerminate(prevWriterJvnServer);
                }
            }

            jvnObjectLock.put(jvnRemoteServer, LockState.R);
        }

        this.backupState();

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
        JvnObjectLock jvnObjectLock = getJvnObjectLockFromId(jvnObjectId);
        boolean requesterHasWriteLock = jvnObjectLock.get(jvnRemoteServer) == LockState.W;

        if (jvnObjectLock.containsValue(LockState.W) && !requesterHasWriteLock) {
            JvnRemoteServer prevWriterJvnServer = jvnObjectLock.entrySet().stream().filter(e -> e.getValue() == LockState.W).iterator().next().getKey();
            try {
                Serializable jvnObjectState = prevWriterJvnServer.jvnInvalidateWriter(jvnObjectId);
                jvnObjectLock.put(prevWriterJvnServer, LockState.NL);
                jvnObjects.put(jvnObjectId, new JvnObjectImpl(jvnObjectState, jvnObjectId));
            } catch (Exception e) {
                System.err.println("Error invalidating writer: " + e.toString());
                jvnTerminate(prevWriterJvnServer);
            }
        }

        if (jvnObjectLock.containsValue(LockState.R)) {
            jvnObjectLock.entrySet().stream().filter(e -> e.getValue() == LockState.R).forEach(entry -> {
                try {
                    JvnRemoteServer prevReaderJvnServer = entry.getKey();
                    if (!prevReaderJvnServer.equals(jvnRemoteServer)) {
                        prevReaderJvnServer.jvnInvalidateReader(jvnObjectId);
                    }
                } catch (Exception e) {
                    System.err.println("Error invalidating reader: " + e.toString());
                }
            });
        }

        jvnObjectLock.put(jvnRemoteServer, LockState.W);

        this.backupState();

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
        } catch (Exception e) {
            System.err.println("JvnCoord exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void backupState() {
        File tempFolder = new File(cacheFolder);
        if (!tempFolder.exists()) {
            tempFolder.mkdir();
        }

        ArrayList<String> a = new ArrayList<>(
                Arrays.asList("internalIdLookupTable", "jvnObjects", "jvnObjectLocks", "objectCount")
        );

        a.forEach(fieldName -> {
            try {
                Field classField = this.getClass().getDeclaredField(fieldName);
                Serializable fieldValue = (Serializable) classField.get(this);
                String pathname = String.format("%s/%s", cacheFolder, fieldName);
                this.writeBackupFile(pathname, fieldValue);
            } catch (Exception e) {
                System.err.println("Error while persisting: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void setStateFromBackup() {
        ArrayList<String> a = new ArrayList<>(
                Arrays.asList("internalIdLookupTable", "jvnObjects", "jvnObjectLocks", "objectCount")
        );

        a.forEach(fieldName -> {
            try {
                Field classField = this.getClass().getDeclaredField(fieldName);
                Serializable prevFieldValue = (Serializable) classField.get(this);
                String pathname = String.format("%s/%s", cacheFolder, fieldName);
                Serializable fieldValue = this.readBackupFile(pathname);
                classField.set(this, fieldValue == null ? prevFieldValue : fieldValue);
            } catch (Exception e) {
                System.err.println("Error while reading: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void writeBackupFile(String pathname, Serializable field) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(pathname);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

        objectOutputStream.writeObject(field);

        fileOutputStream.close();
        objectOutputStream.close();
    }

    private Serializable readBackupFile(String pathname) throws IOException {
        File file = new File(pathname);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        Serializable field = null;
        FileInputStream fileInputStream = new FileInputStream(file);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        try {
            field = (Serializable) objectInputStream.readObject();

            fileInputStream.close();
            objectInputStream.close();
        } catch (Exception e) {
            System.err.println("Error while reading: " + e.getMessage());
        }

        return field;
    }
}
