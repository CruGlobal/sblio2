package org.ccci.framework.sblio;

import com.google.common.base.Strings;
import com.siebel.data.SiebelException;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.log4j.Logger;
import org.ccci.framework.failover.CcciFailoverManager;
import org.ccci.framework.sblio.annotations.Siebel;
import org.ccci.framework.sblio.exceptions.SiebelUnavailableException;

import javax.inject.Inject;

public class SiebelPersistenceFactory extends BasePoolableObjectFactory
{
	private Logger siebelLog = Logger.getLogger(this.getClass());

	@Inject @Siebel private CcciFailoverManager failoverManager;
	
	private String url;
	private String system;
	private String username;
	private String password;
	
	public SiebelPersistenceFactory()
	{
		super();
	}
	
    /**
     * @param system
     * @param username
     */
    public SiebelPersistenceFactory(String system, String username, CcciFailoverManager fm)
    {
        this.system = system;
        this.username = username;
        this.failoverManager = fm;
    }
    
    @Deprecated
    public SiebelPersistenceFactory(String url, String username, String password, CcciFailoverManager fm)
    {
    	super();
    	this.url = url;
    	this.username = username;
    	this.password = password;
    	this.failoverManager = fm;
    }
    /**
     * for makeObject we'll simply return a new SiebelDataBean
     */  
    public Object makeObject() throws SiebelException
    {
		if(failoverManager != null)
	    	if(failoverManager.isFailoverMode()) return null;
    	
    	siebelLog.debug("SiebelDataBeanFactory:makeObject()  creating SiebelDataBean");
    	
    	try
    	{
			if(Strings.isNullOrEmpty(url)) url = SiebelSettings.getProps().getProperty(system + "." + "url");
    		if(Strings.isNullOrEmpty(username)) username = SiebelSettings.getProps().getProperty(system + "." + "defaultUser");
			if(Strings.isNullOrEmpty(password)) password = SiebelSettings.getProps().getProperty(system + "." + username);

			SiebelPersistence persistence;

			if(this.failoverManager == null)
    		{
    			persistence = new SiebelPersistenceImpl(username,password,url);
    		}
    		else
    		{
    			persistence = DatabeanProxy.wrapDatabean(new SiebelPersistenceImpl(username,password,url), failoverManager);
    		}
    		
    		return persistence;
    	}
    	catch(SiebelUnavailableException e)
    	{
    		if(failoverManager != null)
    			failoverManager.recordAndProcessError(System.currentTimeMillis());
    		throw e;
    	}
    	
    } 
    
    @Override
    public boolean validateObject(Object obj)
    {
    	boolean superBool = super.validateObject(obj);
    	SiebelPersistence databean = (SiebelPersistence) obj;
    	try
    	{
    		databean.getDataBean().getBusObject("CCCI DSS BO").getBusComp("CCCI DSS BU Name Lookup");
    	}
    	catch(SiebelException se)
    	{
    		return false;
    	}
    	
    	return superBool;
    }
}
