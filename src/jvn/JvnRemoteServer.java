/***
 * JAVANAISE API
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.*;
import java.io.*;


/**
 * Remote interface of a JVN server (used by a remote JvnCoord)
 */

public interface JvnRemoteServer extends Remote {
	    
	/**
	* Invalidate the Read lock of a JVN object 
	* @param joi : the JVN object id
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException;
	    
	/**
	* Invalidate the Write lock of a JVN object 
	* @param joi : the JVN object id
	* @return the current JVN object state 
	* @throws java.rmi.RemoteException,JvnException
	**/
        public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException;
	
	/**
	* Reduce the Write lock of a JVN object 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException;

}

 
