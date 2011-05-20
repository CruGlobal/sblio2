package org.ccci.framework.sblio;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class SiebelPersistencePoolList {
    private static final String DEFAULT_IDLE_TIMEOUT = "300000";
    private static final String DEFAULT_MIN_IDLE = "2";
    private static final String DEFAULT_MAX_IDLE = "5";
    private static final String DEFAULT_MAX_ACTIVE = "8";
    private static final String DEFAULT_WHEN_EXHAUSTED = "FAIL";
    private static Logger siebelLog = Logger.getLogger("Siebel");
    
    private static long lastFailureRecoveryTimestamp = System.currentTimeMillis();
    
    private Hashtable list = new Hashtable();
    
    private Hashtable allActivePersistenceSessions = new Hashtable();
    
    private static SiebelPersistencePoolList instance;
    
    private SiebelPersistencePoolList()
    {
    }
    
    /**
     * Static method for getting the singleton instance of the pool list.
     * @return
     */
    public synchronized static SiebelPersistencePoolList getInstance()
    {
        if(instance==null) instance = new SiebelPersistencePoolList();
        return instance;
    }

    public synchronized static void nullify(){
    	instance = null;
    }
    
    /**
     * Fetch a single
     * 
     * @param system 
     *      The name of the database system to connect to.  This
     *      is a name that identifies connection settings which must be
     *      set in the configuration file Dbio.properties.  More information
     *      about Dbio.properties can be found in the javadoc for Dbio.
     *      If this is null, the default system will be used (as configured
     *      in the properties file).
     *      
     * @param username
     *      The username to use for the connection.  If this is null, the
     *      default username for the system will be used (as configured
     *      in the properties file).
     *      The corresponding password must be configured in the properties
     *      file.
     * 
     * @return
     * @throws Exception
     */
    public SiebelPersistence getPersistenceSession(String system, String username) throws Exception
    {
        // detect and clear leaks... (since the allActiveDbios code is not fully tested)
        if (allActivePersistenceSessions.size()>20) allActivePersistenceSessions.clear();
                
        if(system==null || system.trim().length()==0)
        {
            system = SiebelSettings.props.getProperty("defaultSystem");
        }
        if(username==null || username.trim().length()==0)
        {
            username = SiebelSettings.props.getProperty(system+".defaultUser");
			Logger.getLogger("Siebel").error("SiebelPersistencePoolList:getPersistenceObject() got default username: "+username+" for system "+system);
        }

        GenericObjectPool pool = null;
        synchronized(this)
        {
	        pool = (GenericObjectPool)list.get(system+"|"+username);
	        
	        if(pool==null)
	        {
	            // create a new pool
	            // we should have a way of destroying pools, too
	            pool = new GenericObjectPool(new SiebelPersistenceFactory(system,username,null));

	            int maxActive = Integer.parseInt(DEFAULT_MAX_ACTIVE);
	            int maxIdle = Integer.parseInt(DEFAULT_MAX_IDLE);
	            int minIdle = Integer.parseInt(DEFAULT_MIN_IDLE);
	            int idleTimeout = Integer.parseInt(DEFAULT_IDLE_TIMEOUT);
	            String whenExhausted = DEFAULT_WHEN_EXHAUSTED;
	            pool.setMaxActive(maxActive);
	            pool.setMaxIdle(maxIdle);
	            pool.setMinIdle(minIdle);
	            pool.setMinEvictableIdleTimeMillis(idleTimeout);
	            // decode the "whenExhausted" setting
	            if(whenExhausted.equals("FAIL"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
	            else if (whenExhausted.equals("GROW"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
	            else if (whenExhausted.equals("BLOCK"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
	            else
	                throw new Exception("Setting for "+system+"."+username+".pool.whenExhausted"+" in Dbio.properties must be one of: GROW, FAIL, BLOCK");
                
                list.put(system+"|"+username,pool);
            }
        }
        
        try
        {
            SiebelPersistence sblio = (SiebelPersistenceImpl)pool.borrowObject();

            Exception ex = new Exception("SiebelDataBean allocated here");
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            allActivePersistenceSessions.put(sblio,stacktrace);
            
            Logger.getLogger("Siebel").error("DataBean Id: " + sblio.toString());
			Logger.getLogger("Siebel").error("SiebelPersistencePoolList:getDataBean() " + allActivePersistenceSessions.size() + " of " + pool.getMaxActive() + " possible are active");

            sblio.reset(); //just in case it didn't get reset on the way in
            
            return sblio;
        }
        catch(java.util.NoSuchElementException e)
        {
            // This exception occurs when the pool is full.  When this happens, we'll assume
            // that there's been some major problem.  So, we'll close all of the connections,
            // clear the pool, and start over.  If this has already happened within a 30-minute period,
            // we'll let the current exception propogate down the stack.  Otherwise, we'll try again
            // to get a Dbio database connection after clearing the pool.
            
            boolean propogateException = false;
            if(System.currentTimeMillis() - lastFailureRecoveryTimestamp < 30*60*1000)
            {
                propogateException = true;  // remember that we want to re-throw exception
            }
            lastFailureRecoveryTimestamp = System.currentTimeMillis();
            
            try
            {
                if(propogateException)
                {
                    String text = ""; 
                    try
                    {
                        text+=" utilization: "+pool.getNumActive()+" of "+pool.getMaxActive();
                    }
                    catch(Exception e2) {text+=" error trying to show utilization: "+e2.getMessage();}
                    throw new Exception("propogating exception caused in borrowObject for pool named: "+system+"|"+username+": "+text,e);
                }
            }
            finally
            {
                closeAll();
            }
            
            // if not re-thrown, then try again (if this throws, then let it go)
            // need to get a new pool!!
            pool = (GenericObjectPool)list.get(system+"|"+username);
            SiebelPersistence sblio = (SiebelPersistenceImpl)pool.borrowObject();

            Exception ex = new Exception("SiebelDataBean allocated here");
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            allActivePersistenceSessions.put(sblio,stacktrace);

            return sblio;
        }
    }
    
    public SiebelPersistence getPersistenceSession(String system, String username, String url, String password) throws Exception
    {
        // detect and clear leaks... (since the allActiveDbios code is not fully tested)
        if (allActivePersistenceSessions.size()>20) allActivePersistenceSessions.clear();
                
        if(system==null || system.trim().length()==0)
        {
            system = SiebelSettings.props.getProperty("defaultSystem");
        }
        if(username==null || username.trim().length()==0)
        {
            username = SiebelSettings.props.getProperty(system+".defaultUser");
			Logger.getLogger("Siebel").error("SiebelDataBeanPoolList:getDataBean() got default username: "+username+" for system "+system);
        }
        if(url == null || url.trim().length() == 0)
        {
        	url = SiebelSettings.props.getProperty(system+".url");
        }
        if(password == null || password.trim().length() == 0)
        {
        	password = SiebelSettings.props.getProperty(system+"."+username);
        }

        GenericObjectPool pool = null;
        synchronized(this)
        {
	        pool = (GenericObjectPool)list.get(system+"|"+username);
	        
	        if(pool==null)
	        {
	            // create a new pool
	            // we should have a way of destroying pools, too
	            pool = new GenericObjectPool(new SiebelPersistenceFactory(url, username, password, null));

	            int maxActive = Integer.parseInt(DEFAULT_MAX_ACTIVE);
	            int maxIdle = Integer.parseInt(DEFAULT_MAX_IDLE);
	            int minIdle = Integer.parseInt(DEFAULT_MIN_IDLE);
	            int idleTimeout = Integer.parseInt(DEFAULT_IDLE_TIMEOUT);
	            String whenExhausted = DEFAULT_WHEN_EXHAUSTED;
	            pool.setMaxActive(maxActive);
	            pool.setMaxIdle(maxIdle);
	            pool.setMinIdle(minIdle);
	            pool.setMinEvictableIdleTimeMillis(idleTimeout);
	            // decode the "whenExhausted" setting
	            if(whenExhausted.equals("FAIL"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
	            else if (whenExhausted.equals("GROW"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
	            else if (whenExhausted.equals("BLOCK"))
	                pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
	            else
	                throw new Exception("Setting for "+system+"."+username+".pool.whenExhausted"+" in Dbio.properties must be one of: GROW, FAIL, BLOCK");
                
                list.put(system+"|"+username,pool);
            }
        }
        
        try
        {
            SiebelPersistence sblio = (SiebelPersistence)pool.borrowObject();

            Exception ex = new Exception("SiebelDataBean allocated here");
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            allActivePersistenceSessions.put(sblio,stacktrace);
            
            Logger.getLogger("Siebel").error("DataBean Id: " + sblio.toString());
			Logger.getLogger("Siebel").error("SiebelDataBeanPoolList:getDataBean() " + allActivePersistenceSessions.size() + " of " + pool.getMaxActive() + " possible are active");

            sblio.reset(); //just in case it didn't get reset on the way in
            
            return sblio;
        }
        catch(java.util.NoSuchElementException e)
        {
            // This exception occurs when the pool is full.  When this happens, we'll assume
            // that there's been some major problem.  So, we'll close all of the connections,
            // clear the pool, and start over.  If this has already happened within a 30-minute period,
            // we'll let the current exception propogate down the stack.  Otherwise, we'll try again
            // to get a Dbio database connection after clearing the pool.
            
            boolean propogateException = false;
            if(System.currentTimeMillis() - lastFailureRecoveryTimestamp < 30*60*1000)
            {
                propogateException = true;  // remember that we want to re-throw exception
            }
            lastFailureRecoveryTimestamp = System.currentTimeMillis();
            
            try
            {
                if(propogateException)
                {
                    String text = ""; 
                    try
                    {
                        text+=" utilization: "+pool.getNumActive()+" of "+pool.getMaxActive();
                    }
                    catch(Exception e2) {text+=" error trying to show utilization: "+e2.getMessage();}
                    throw new Exception("propogating exception caused in borrowObject for pool named: "+system+"|"+username+": "+text,e);
                }
            }
            finally
            {
                closeAll();
            }
            
            // if not re-thrown, then try again (if this throws, then let it go)
            // need to get a new pool!!
            pool = (GenericObjectPool)list.get(system+"|"+username);
            SiebelPersistence sblio = (SiebelPersistenceImpl)pool.borrowObject();

            Exception ex = new Exception("SiebelDataBean allocated here");
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            allActivePersistenceSessions.put(sblio,stacktrace);

            return sblio;
        }
    }
    
    /**
     * Release a databean that has been gotten from this list.
     */
    public synchronized void releasePersistenceSession(SiebelPersistence databean) throws Exception
    {
        databean.reset();
        allActivePersistenceSessions.remove(databean);
        GenericObjectPool pool = null;
        pool = (GenericObjectPool)list.get(databean.getSystem()+"|"+databean.getUsername());
        
        if(pool==null)
        {
        	throw new RuntimeException("Pool not found for databean "+databean.getSystem()+"|"+databean.getUsername());
        }
        
		Logger.getLogger("Siebel").error("SiebelDataBeanPoolList:releaseDataBean() " + allActivePersistenceSessions.size() + " databeans are still active");

        pool.returnObject(databean);
        
        return;
    }
    
    public synchronized void killPersistenceSession(SiebelPersistence databean) throws Exception
    {
    	allActivePersistenceSessions.remove(databean);
//    	databean.getDataBean().logoff(); --apparently this causes an error too
    }
    
    /**
     * Don't make this synchronized, because then closing resources
     * will have a negative impact on opening new ones.  We use
     * selective internal synchronization instead.
     *
     */
    public void closeAll() throws Exception
    {
        Object[] poolsLocalCopy;
        allActivePersistenceSessions.clear();
        synchronized(list)
        {
            poolsLocalCopy = list.values().toArray();
            list.clear();
        }
        Exception saved = null;
        for(int i=0; i<poolsLocalCopy.length; i++)
        {
            GenericObjectPool pool = (GenericObjectPool)poolsLocalCopy[i];
            try
            {
                pool.close();
            }
            catch (Exception e)
            {
                // save the exception for re-throw later, but continue on
                // so that we can close the other pools
                saved = e;
            }
        }
        if(saved!=null) throw saved;
    }
//
//    /**
//     * Remove all unused (idle) Dbios from all pools.
//     */
//    public synchronized void closeUnused()
//    {
//        Object[] poolsLocalCopy;
//        poolsLocalCopy = list.values().toArray();
//        for(int i=0; i<poolsLocalCopy.length; i++)
//        {
//            GenericObjectPool pool = (GenericObjectPool)poolsLocalCopy[i];
//            pool.clear();
//        }
//    }
    
    public Hashtable getList(){
    	return list;
    }
    
    public Hashtable getAllActivePersistenceSessions(){
    	return allActivePersistenceSessions;
    }

	public static String getDefaultMaxActive() {
		return DEFAULT_MAX_ACTIVE;
	}
}
