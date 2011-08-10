package org.ccci.framework.sblio;

import java.util.List;

import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;

/**
 * this class will throw a siebel exception every time... isn't it beautiful?
 * @author ryan.t.carlson
 *
 */
public class BadSiebelPersistence implements SiebelPersistence
{
	
	public BadSiebelPersistence()
	{
		super();
	}
	
	public SiebelDataBean getDataBean()
	{
		return null;
	}

	public SiebelServiceWrapper getService(String arg0)
	{
		throw new SblioException(null, new SiebelException());
	}

	public void reset()
	{
		return;
	}

	public boolean siebelDelete(Object obj)
	{
		throw new SblioException(null, new SiebelException());
	}

	public boolean siebelDeleteMvgField(Object parentObj, String fieldName, Object recordForDelete)
	{
        throw new SblioException(null, new SiebelException());
	}

	public String siebelInsert(Object obj)
	{
        throw new SblioException(null, new SiebelException());
	}

	public String siebelInsertMvgField(Object parentObj, String fieldName, Object recordForUpsert)
	{
        throw new SblioException(null, new SiebelException());
	}

	public <T> List<T> siebelListSelect(T obj)
	{
        throw new SblioException(null, new SiebelException());
	}

	public int siebelSelect(Object obj) 
	{
        throw new SblioException(null, new SiebelException());
	}

	public boolean siebelSynchronize(SiebelSynchronizable exampleObj, List<? extends SiebelSynchronizable> records)
	{
        throw new SblioException(null, new SiebelException());
	}

	public String siebelUpsert(Object obj)
	{
        throw new SblioException(null, new SiebelException());
	}

	public String siebelUpsertMvgField(Object parentObj, String fieldName, Object recordForUpsert)
	{
        throw new SblioException(null, new SiebelException());
	}

	public String getSystem()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getUsername()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean siebelDeleteAll(Object obj)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public String siebelSetMvgPrimaryRecord(Object parentObj, String fieldName, Object childObj)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String siebelInsertMvgField(Object parentObj,
			Object recordForUpsert, boolean manyToMany) {
		// TODO Auto-generated method stub
		return null;
	}

    public void close() throws Exception
    {
    }

	public String siebelInsertChildField(Object parent, String fieldName, Object child)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
