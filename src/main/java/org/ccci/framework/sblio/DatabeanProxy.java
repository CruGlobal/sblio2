package org.ccci.framework.sblio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ccci.framework.failover.CcciFailoverManager;

import com.siebel.data.SiebelException;


public class DatabeanProxy implements java.lang.reflect.InvocationHandler
{
	private IDssDataBean databean;
	private CcciFailoverManager failoverManager;
	
	public static IDssDataBean wrapDatabean(IDssDataBean databean, CcciFailoverManager failoverManager)
	{
		return (IDssDataBean) java.lang.reflect.Proxy.newProxyInstance(databean.getClass().getClassLoader(), 
														databean.getClass().getInterfaces(), 
														new DatabeanProxy(databean, failoverManager));
	}
	
	private DatabeanProxy(IDssDataBean databean, CcciFailoverManager failoverManager)
	{
		this.databean = databean;
		this.failoverManager = failoverManager;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args)throws Throwable
	{
		Object result = null;
		try
		{
			result = method.invoke(databean,args);
		}
		catch (InvocationTargetException e)
		{
			Throwable t = getRootExeption(e);
			if(t instanceof SiebelException)
				failoverManager.recordAndProcessError(System.currentTimeMillis());
			throw t;
		}
		return result;
	}

    private Throwable getRootExeption(InvocationTargetException e)
    {
        Throwable throwable = e.getCause();
        while (throwable.getCause() != null)
        {
            throwable = throwable.getCause();
        }
        return throwable;
    }



}
