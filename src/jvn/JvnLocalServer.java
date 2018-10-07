/***
 * JAVANAISE API
 * Contact: 
 *
 * Authors: 
 */

package jvn;
import java.io.Serializable;

/**
 * Local interface of a JVN server  (used by the applications).
 * An application can get the reference of a JVN server through the static
 * method jvnGetServer() (see  JvnServerImpl). 
 */

public interface JvnLocalServer {
	
	/**
	* create of a JVN object
	* @param jos : the JVN object state
  * @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable jos)
	throws jvn.JvnException ; 
	
	/**
	* Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException; 
	
	/**
	* Get the reference of a JVN object associated to a symbolic name
	* @param jon : the JVN object symbolic name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException ; 
	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException;

	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException;

         
   /**
    * The JVN service is not used anymore by the application
    * @throws JvnException
    **/
   public  void jvnTerminate()
   throws jvn.JvnException; 
}

 
