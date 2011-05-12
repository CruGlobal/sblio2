package org.ccci.framework.sblio;

import com.siebel.data.SiebelException;
import com.siebel.data.SiebelPropertySet;
import com.siebel.data.SiebelService;

public class DssSiebelService
{
	private SiebelService service;
	
	public DssSiebelService(SiebelService service)
	{
		this.service = service;
	}

	public boolean equals(Object arg0)
	{
		return service.equals(arg0);
	}

	public String getFirstProperty() throws SiebelException
	{
		return service.getFirstProperty();
	}

	public String getName()
	{
		return service.getName();
	}

	public String getNextProperty() throws SiebelException
	{
		return service.getNextProperty();
	}

	public String getProperty(String arg0) throws SiebelException
	{
		return service.getProperty(arg0);
	}

	public int hashCode()
	{
		return service.hashCode();
	}

	public boolean invokeMethod(String arg0, SiebelPropertySet arg1, SiebelPropertySet arg2) throws SiebelException
	{
		boolean result =  service.invokeMethod(arg0, arg1, arg2);
		release();
		return result;
	}

	public boolean propertyExists(String arg0) throws SiebelException
	{
		return service.propertyExists(arg0);
	}

	public void release()
	{
		service.release();
	}

	public void removeProperty(String arg0) throws SiebelException
	{
		service.removeProperty(arg0);
	}

	public void setProperty(String arg0, String arg1) throws SiebelException
	{
		service.setProperty(arg0, arg1);
	}

	public String toString()
	{
		return service.toString();
	}
	
	
}
