package org.ccci.framework.sblio;

import javax.inject.Inject;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.log4j.Logger;
import org.ccci.framework.failover.CcciFailoverManager;
import org.ccci.framework.sblio.annotations.Siebel;
import org.ccci.framework.sblio.exceptions.SiebelUnavailableException;
import org.ccci.util.Util;

import com.siebel.data.SiebelException;

public class SiebelDataBeanFactory extends BasePoolableObjectFactory
{
	private static final String MODULE_NAME = "default";
	private Logger siebelLog = Logger.getLogger("Siebel");
	
	@Inject @Siebel private CcciFailoverManager failoverManager;
	
	private String url;
	private String system;
	private String username;
	private String password;
	
	public SiebelDataBeanFactory()
	{
		super();
	}
	
    /**
     * @param system
     * @param username
     */
    public SiebelDataBeanFactory(String system, String username, CcciFailoverManager fm)
    {
        this.system = system;
        this.username = username;
        this.failoverManager = fm;
    }
    
    @Deprecated
    public SiebelDataBeanFactory(String url, String username, String password, CcciFailoverManager fm)
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
    	if(failoverManager.isFailoverMode()) return null;
    	
    	siebelLog.debug("SiebelDataBeanFactory:makeObject()  creating SiebelDataBean");
    	
    	try
    	{
    		IDssDataBean databean;
    		url = SiebelSettings.getProps().getProperty(system + "." + "url");
    		if(Util.isBlank(username)) username = SiebelSettings.getProps().getProperty(system + "." + "defaultUser");
    		password = SiebelSettings.getProps().getProperty(system + "." + username);
    		
    		if(this.failoverManager == null)
    		{
    			databean = new DssDataBean(username,password,url);
    		}
    		else
    		{
    			databean = DatabeanProxy.wrapDatabean(new DssDataBean(username,password,url), failoverManager);
    		}
    		
    		return databean;
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
    	IDssDataBean databean = (IDssDataBean) obj;
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
