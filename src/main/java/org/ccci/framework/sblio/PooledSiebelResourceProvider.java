package org.ccci.framework.sblio;

import java.util.ArrayList;
import java.util.List;

import org.ccci.framework.provider.IResourceProvider;
import org.ccci.framework.user.IUser;
import org.ccci.util.ServletProperties;

import com.siebel.data.SiebelException;

public class PooledSiebelResourceProvider implements IResourceProvider
{

    public static final String NAME = "PooledSiebelResourseProvider";
    protected boolean useDev = true;
    
    private static List resourceNames = null;
    
    static
    {
        resourceNames = new ArrayList(1);
        resourceNames.add(SiebelPersistenceImpl.NAME);
    }
	
    public PooledSiebelResourceProvider(){}
    
    public PooledSiebelResourceProvider(boolean useDev)
    {
        this.useDev = useDev;
    }
    
    public PooledSiebelResourceProvider(String type, ServletProperties properties)
    {
    	
    }
    
	public void expireOldResources()
	{
		return;
	}

	public Object getResource(String name, IUser user) throws Exception
	{
		if(resourceNames.contains(name))
		{
		    SiebelPersistence persistence = SiebelPersistencePoolList.getInstance().getPersistenceSession(useDev?"DEV":"TEST", null, null, null);
			try
			{
				persistence.getDataBean().getBusObject("Account");
				persistence.reset();
				return persistence;
			}
			catch(SiebelException se)
			{
				SiebelPersistencePoolList.getInstance().killPersistenceSession(persistence);
				persistence = SiebelPersistencePoolList.getInstance().getPersistenceSession(useDev?"DEV":"DEV", null, null, null);
				persistence.getDataBean().getBusObject("Account");
				persistence.reset();
				return persistence;	
			}
		}
		return null;
	}

	public List getResourcesProvided()
	{
		return resourceNames;
	}

	public void releaseResource(Object resource, IUser user) throws Exception
	{
		if(resource instanceof SiebelPersistence)
		{
		    SiebelPersistence persistence = (SiebelPersistence)resource;
			persistence.reset();
			SiebelPersistencePoolList.getInstance().releasePersistenceSession(persistence);
		}

	}

	public String getName()
	{
		return this.NAME;
	}

}
