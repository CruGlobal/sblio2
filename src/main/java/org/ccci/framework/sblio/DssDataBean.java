package org.ccci.framework.sblio;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.ccci.exceptions.FatalException;
import org.ccci.framework.sblio.annotations.BusinessComp;
import org.ccci.framework.sblio.annotations.BusinessObject;
import org.ccci.framework.sblio.annotations.Key;
import org.ccci.framework.sblio.annotations.ManyToMany;
import org.ccci.framework.sblio.annotations.MvgBusComp;
import org.ccci.framework.sblio.annotations.ReadOnly;
import org.ccci.framework.sblio.annotations.Transient;
import org.ccci.framework.sblio.exceptions.SiebelUnavailableException;
import org.ccci.util.Util;

import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;

/**
 * 
 * See "Java Data Bean Quick Reference" at http://download.oracle.com/docs/cd/B40099_02/books/OIRef/OIRefJava_QR.html for elaboration
 * on the jdb api.  The javadocs are not very complete.
 * 
 * This class is not threadsafe; only one thread should use an DssDataBean instance at a time.
 * 
 * @author Ryan Carlson
 * @author Matt Drees
 * @author Lee Braddock
 */
public class DssDataBean implements IDssDataBean
{
	private final DssDataBeanHelper helper = new DssDataBeanHelper();
	
//	private static Logger siebelLog = Logger.getLogger("Siebel");
	
    public static String NAME = "SiebelDataBean";
	private String system;
	private String username;
	private String url;

	private SiebelDataBean databean;
	private SiebelBusObject busObj;
	private DssBusComp mainBusComp;

	static
	{
	    /*
	     * Siebel jdb does not like it if the file.encoding property is set to the Mac's default encoding, MacRoman.
	     * See http://blog.ideaportriga.org/java/?p=78 for more info.
	     */
	    if (System.getProperty("file.encoding").equals("MacRoman"))
	    {
	        System.setProperty("file.encoding", "utf8");
	    }
	}
	
	public DssDataBean(String username, String password, String url)
	{
		databean = new SiebelDataBean();
		try
		{
			databean.login(url, username, password, "enu");
		}
		catch(SiebelException se)
		{
			throw new SiebelUnavailableException(se);
		}
	}
	
	/**
	 * construct which creates internal databean and logs it into Siebel
	 * takes system (ie: DEV) & username (ie: DSSJAVA) and looks up password in
	 * Siebel.properties
	 * @param system
	 * @param username
	 */
	public DssDataBean(String system, String username)
	{
		this.system = system;
		this.username = username;
		this.url = (String)SiebelSettings.props.get(system + ".url");

		String password = (String)SiebelSettings.props.get(system + "." + username);

		databean = new SiebelDataBean();

		try
		{
			databean.login(url.toString(), username, password, "enu");	
		}
		catch(SiebelException se)
		{
			throw new SiebelUnavailableException(se);
		}
	}

	public int siebelSelect(Object obj)
	{
		long end = System.currentTimeMillis();
		long start = end;
		try
        {
            setupForQuery(obj, false);

    		mainBusComp.executeQuery(false);
    
    		if(mainBusComp.firstRecord())
    		{
    			copySearchResultsToEntityObject(mainBusComp, obj);
    			
    			// process (any) many to many mvg(s)
    			siebelSelectManyToManyMvg(obj);
				
    			int count = 1;

    			// this is for callers who depend on exactly one match, if there's another record, increment match, but still only return the first
    			if(mainBusComp.nextRecord()) count++;		
    			
    			end = System.currentTimeMillis();
    			
    			return count;
    		}
    		    		
    		end = System.currentTimeMillis();
    
    		return 0;
        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform select on " + obj, e);
        }
        finally
        {
    		reset();
//        	Logger.getLogger("Siebel").debug("SiebelSelect query finished. Time: " + String.valueOf(end-start) + ". MainBusComp: " + mainBusComp.getName());
        }
	}

	private void siebelSelectManyToManyMvg(Object obj)
	{
		try
		{
			Field fs[] = obj.getClass().getDeclaredFields();
			for (Field f : fs)
			{
				f.setAccessible(true);
				ManyToMany manyToMany = f.getAnnotation(ManyToMany.class);
				if (manyToMany != null)
					f.set(obj, siebelListSelectManyToManyMvg(mainBusComp, manyToMany.clazz().newInstance()));
			}

		} catch (Exception e) {
			throw new SblioException("Unable to perform select on " + obj, e);
		}
	}

    private <T> List<T> siebelListSelectManyToManyMvg(DssBusComp mainBusComp, T mvgObject) 
	{
        if (mvgObject == null)
            throw new NullPointerException("query object is null");
        
		DssBusComp m_busCompMVG = null;

        try
        {
    		List<T> returnList = new ArrayList<T>();
    		
    		m_busCompMVG = new DssBusComp(mainBusComp.getMVGBusComp(getBusinessComponent(mvgObject).name()));
    		
        	prepareForQuery(m_busCompMVG, mvgObject, false, true);

			m_busCompMVG.executeQuery2(true, true);
			
    		if(m_busCompMVG.firstRecord())
    		{
    		    @SuppressWarnings("unchecked")
    		    Class<T> objType = (Class<T>) mvgObject.getClass();
    			do
    			{
    				T tempObj = instantiate(objType);
    				copySearchResultsToEntityObject(m_busCompMVG, tempObj);
    				returnList.add(tempObj);
    			}
    			while(m_busCompMVG.nextRecord());
    		}
    		
    		return returnList;

        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform list select on " + mvgObject, e);
        }
        finally
        {
        	if(m_busCompMVG != null)
        		m_busCompMVG.release();
        }
	}

    public <T> List<T> siebelListSelect(T obj) 
	{
    	long end = System.currentTimeMillis();
		long start = end;
		
        if (obj == null)
            throw new NullPointerException("query object is null");
        try
        {
    		List<T> returnList = new ArrayList<T>();
    		setupForQuery(obj, false);
    		mainBusComp.executeQuery(false);
    		
    		if(mainBusComp.firstRecord())
    		{
    		    @SuppressWarnings("unchecked")
    		    Class<T> objType = (Class<T>) obj.getClass();
    			do
    			{
    				T tempObj = instantiate(objType);
    				copySearchResultsToEntityObject(mainBusComp, tempObj);
    				returnList.add(tempObj);
    			}
    			while(mainBusComp.nextRecord());
    		}
    		end = System.currentTimeMillis();
    		
    		return returnList;
        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform list select on " + obj, e);
        }
        finally
        {
    		reset();
//        	Logger.getLogger("Siebel").debug("SiebelListSelect query finished. Time: " + String.valueOf(end-start) + ". MainBusComp: " + mainBusComp.getName());
        }
	}

    private <T> T instantiate(Class<T> objType) 
    {
        try
        {
            return objType.newInstance();
        }
        catch (InstantiationException e)
        {
            throw new IllegalArgumentException(String.format(
                "can't instantiate object of type %s; make sure it's not abstract or an interface", 
                objType.getName()), e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalArgumentException(String.format(
                "can't instantiate object of type %s; make sure it has a public no-argument constructor", 
                objType.getName()), e);
        }
    }

	public String siebelInsert(Object obj)
	{
	    try
	    {
    		loadBusinessObject(obj);
    		loadBusinessComponent(obj);
    		
    		mainBusComp.newRecord(false);
    		setFieldsForInsert(mainBusComp, obj);
    		boolean success = mainBusComp.writeRecord();
    		String retId = mainBusComp.getFieldValue("Id");
    		    		
    		if(success) return retId;
    		else throw new SblioException("insert did not succeed");
	    }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform insert on " + obj, e);
        }
        finally
        {
        	reset();
        }
	}

	public String siebelUpsert(Object obj)
	{
		String retVal;
		try
        {
            setupForQuery(obj, true);

            mainBusComp.executeQuery(false);

            if(mainBusComp.firstRecord())
            {
            	setFieldsForUpdate(mainBusComp, obj);
            	mainBusComp.writeRecord();
            }
            else
            {
            	mainBusComp.newRecord(false);
            	setFieldsForInsert(mainBusComp, obj);
            	mainBusComp.writeRecord();	
            }
            
            retVal = mainBusComp.getFieldValue("Id");
        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform upsert on " + obj, e);
        }
        finally
        {
        	reset();
        }
        
		return retVal;
	}
	
	public String siebelSetMvgPrimaryRecord(Object parentObj, Object childObj)
	{
		DssBusComp childBusComp = null;

		try
		{
			setupForQuery(parentObj, true);
			
			mainBusComp.executeQuery(false);
			
			if(mainBusComp.firstRecord())
			{
				MvgBusComp mvgBc = childObj.getClass().getAnnotation(MvgBusComp.class);
				if(mvgBc == null) throw new RuntimeException("missing annotation");
				
				childBusComp = new DssBusComp(mainBusComp.getMVGBusComp(mvgBc.name()));
				
				activateFields(childBusComp,childObj);
				
				childBusComp.setViewMode(3);
				childBusComp.clearToQuery();
				
				setSearchSpecs(childBusComp, childObj, false);
				
				childBusComp.executeQuery(false);
				
				if(childBusComp.firstRecord())
				{
					childBusComp.setFieldValue("SSA Primary Field", "Y");
					mainBusComp.writeRecord();
				}
			}
		}
		catch(SiebelException se)
		{
			throw new SblioException("Unable to set primary record on " + childObj,se);
		}
		finally
		{
			if(childBusComp != null)
				childBusComp.release();
			
			reset();
		}
		
		return "";
	}
	
	public String siebelInsertMvgField(Object parentObj, Object recordForUpsert)
	{
		return siebelUpsertMvgField(parentObj,recordForUpsert,true,false);
	}
	
	public String siebelInsertMvgField(Object parentObj, Object recordForUpsert, boolean manyToMany)
	{
		return siebelUpsertMvgField(parentObj,recordForUpsert,true,manyToMany);
	}
	
	public String siebelUpsertMvgField(Object parentObj, Object recordForUpsert) 
	{
		return siebelUpsertMvgField(parentObj,recordForUpsert,false,false);
	}
	
	private String siebelUpsertMvgField(Object parentObj, Object recordForUpsert, boolean forceInsert, boolean manyToMany)
	{
		DssBusComp mvgBusComp = null;
    	DssBusComp assocBusComp = null;
		String rowId = "";
		
		if(parentObj != null)
		{
			try
            {
                setupForQuery(parentObj, true);

                mainBusComp.executeQuery(false);

                if(mainBusComp.firstRecord())
                {
                	MvgBusComp mvgBc = recordForUpsert.getClass().getAnnotation(MvgBusComp.class);
                	
                	if(mvgBc != null)
                	{
                		mvgBusComp = new DssBusComp(mainBusComp.getMVGBusComp(mvgBc.name()));

                		if(manyToMany)
                		{
                        	assocBusComp = new DssBusComp(mvgBusComp.getAssocBusComp());

                        	prepareForQuery(assocBusComp, recordForUpsert, false, false);

                        	assocBusComp.executeQuery2(true, true);

                			if (assocBusComp.firstRecord())
                			{
	                			assocBusComp.associate(true);
	                			assocBusComp.writeRecord();
	                			mainBusComp.writeRecord();
	                			
	                			// set primary field if first record in MVG
	                			prepareForQuery(mvgBusComp, recordForUpsert, true, false);
	                			mvgBusComp.executeQuery2(true, true);
	            				if (mvgBusComp.firstRecord())
	            				{
	            					mvgBusComp.setFieldValue("SSA Primary Field", "Y");
	            					mainBusComp.writeRecord();
	            				}
                			}
                		}
                			
                		else if(!forceInsert && searchForMatchInMvg(mvgBusComp, recordForUpsert))	//update
                		{
                			setFieldsForUpdate(mvgBusComp, recordForUpsert);
                			mvgBusComp.writeRecord(); 
                			mainBusComp.writeRecord();
                		}
                		else	//insert
                		{
                			mvgBusComp.newRecord(true);
                			setFieldsForInsert(mvgBusComp, recordForUpsert);
                			mvgBusComp.writeRecord();
                			mainBusComp.writeRecord();
                		}
                		rowId = mvgBusComp.getFieldValue("Id");
                	}
                }
            }
            catch (SiebelException e)
            {
                throw new SblioException("unable to upsert: " + recordForUpsert, e);
            }
            finally
            {
            	if(mvgBusComp != null)
            		mvgBusComp.release();

            	if(assocBusComp != null)
            		assocBusComp.release();
            	
            	reset();
            }
		}
		
		return rowId;
	}
	
	public boolean siebelDelete(Object obj)
	{
		try
        {
            setupForQuery(obj, true);

            mainBusComp.executeQuery(false);

            if(mainBusComp.firstRecord())
            {
            	return mainBusComp.deleteRecord();
            }
            
            else return false;
        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to perform delete on " + obj, e);
        }
        finally
        {
        	reset();
        }
	}
	
	public boolean siebelDeleteAll(Object obj)
	{
		try
        {
            setupForQuery(obj, false);

            mainBusComp.executeQuery(false);

            if(mainBusComp.firstRecord())
            {
            	do
            	{
            		mainBusComp.deleteRecord();
            	}
            	while(mainBusComp.nextRecord());
            	return true;
            }
            
            else return false;
        }
        catch (SiebelException e)
        {
            throw new SblioException("Unable to delete " + obj, e);
        }
        finally
        {
        	reset();
        }
	}
	
	public boolean siebelDeleteMvgField(Object parentObj, Object recordForDelete)
	{		
		if(parentObj != null)
		{
			DssBusComp mvgBusComp = null;

			try
            {
                setupForQuery(parentObj, true);

                mainBusComp.executeQuery(false);

                if(mainBusComp.firstRecord())
                {
                	MvgBusComp mvgBc = recordForDelete.getClass().getAnnotation(MvgBusComp.class);
                	
                	if(mvgBc != null)
                	{
                		mvgBusComp = new DssBusComp(mainBusComp.getMVGBusComp(mvgBc.name()));
                		if(searchForMatchInMvg(mvgBusComp, recordForDelete))
                		{
                			mvgBusComp.deleteRecord();
                			mainBusComp.writeRecord();
                		}
                	}
                }
            }
            catch (SiebelException e)
            {
                throw new SblioException("unable to delete mvg field " + recordForDelete, e);
            }
            finally
            {
            	if(mvgBusComp != null)
            		mvgBusComp.release();
            	
            	reset();
            }
		}

		return false;
	}
	
	public boolean siebelSynchronize(SiebelSynchronizable exampleObj, List<? extends SiebelSynchronizable> records)
	{
		try
		{
			setupForQuery(exampleObj, false);

			mainBusComp.executeQuery(false);
			if(mainBusComp.firstRecord())
			{
				deleteRowsNotInPropertySet(mainBusComp, exampleObj, records);
			}
			insertRowsNotInBusComp(mainBusComp, records);
		}
		catch (SiebelException e)
		{
			throw new SblioException("unable to synchronize " + exampleObj, e);
		}
		finally
		{
			reset();
		}

		return true;
	}
	
	public DssSiebelService getService(String serviceName)
	{
		try
        {
            return new DssSiebelService(databean.getService(serviceName));
        }
        catch (SiebelException e)
        {
            throw new SblioException("unable to retrieve service " + serviceName, e);
        }
	}
	
	/**
	 * This method will remove rows in Siebel that don't have an exact matching row in the passed thru @param externalDataRows
	 * UNLESS: that row is defined as an IgnoredType in interface code.
	 * @param busComp
	 * @param exampleObject
	 * @param externalDataRows
	 * @throws SiebelException
	 */
	private void deleteRowsNotInPropertySet(DssBusComp busComp, SiebelSynchronizable exampleObject,
											List<? extends SiebelSynchronizable> externalDataRows) throws SiebelException
	{
		boolean moreRecords = true;
		do
			
		{
			copySearchResultsToEntityObject(busComp, exampleObject);
			if(!exampleObject.shouldIgnore() && !externalDataRows.contains(exampleObject))
			{
				busComp.deleteRecord();
				moreRecords = busComp.firstRecord();
			}
			else
			{
				moreRecords = busComp.nextRecord();
			}
		}
		while(moreRecords);
		
		return;
	}
	
	private void insertRowsNotInBusComp(DssBusComp busComp, List<?> propertySet) throws SiebelException
	{
		for(Object o : propertySet)
		{
			busComp.clearToQuery();
			setSearchSpecs(busComp, o, true);
			busComp.executeQuery(false);
			if(!busComp.firstRecord())
			{
				busComp.newRecord(true);
				setFieldsForInsert(busComp, o);
				busComp.writeRecord();
			}
			else
			{
				setFieldsForUpdate(busComp, o);
				busComp.writeRecord();
			}
		}
		return;
	}


	private boolean searchForMatchInMvg(DssBusComp mvgBusComp, Object recordForInsert) throws SiebelException
	{
		boolean found = false;
		if(mvgBusComp.firstRecord())
		{
			setSearchSpecs(mvgBusComp, recordForInsert, true);
			found = mvgBusComp.executeQuery(true) && mvgBusComp.firstRecord();
		}
		return found;
	}



	//************************************************************************
	//************************************************************************
	//SET FIELDS IN BUSCOMP'S FOR INSERT/UPDATE
	//************************************************************************
	//************************************************************************
	private void setFieldsForUpdate(DssBusComp busComp, Object obj) throws SiebelException
	{
		List<Field> fields = helper.getAllDeclaredInstanceFields(obj);
		for(Field field : fields){
            field.setAccessible(true);
			if(!isTransientField(field) && !isManyToManyField(field) && !isReadOnlyField(field)){
				String fieldName = helper.getFieldName(field);
				Object fieldValueObject = helper.getFieldValueFromAccessibleField(obj, field);
				if(!field.getType().getName().equals("java.util.List"))
				{
					if(fieldValueObject == null)
					{
						busComp.setFieldValue(fieldName, "");
					}
					else
					{
						String fieldValue = helper.convertFieldValueToSiebelValue(field, fieldValueObject);
						busComp.setFieldValue(fieldName, fieldValue);
					}
				}
			}
		}
		return;
	}

    private void setFieldsForInsert(DssBusComp busComp, Object obj) throws SiebelException
	{
		List<Field> fields = helper.getAllDeclaredInstanceFields(obj);
        for(Field field : fields){
            field.setAccessible(true);
			if(!isTransientField(field) && !isManyToManyField(field) && !isReadOnlyField(field)){
				String fieldName = helper.getFieldName(field);
				Object fieldValueObject = helper.getFieldValueFromAccessibleField(obj, field);

				if(fieldValueObject != null && !field.getType().equals(List.class))
				{
                    String fieldValue = helper.convertFieldValueToSiebelValue(field, fieldValueObject);
					busComp.setFieldValue(fieldName, fieldValue);
				}
			}
		}
		return;
	}

		
	//************************************************************************
	//************************************************************************
	//HELPER METHODS- COMMON LOGIC
	//************************************************************************
	//************************************************************************
	
	private void activateFields(DssBusComp busComp, Object obj) throws SiebelException
	{
		List<Field> fields = helper.getAllDeclaredInstanceFields(obj);
		for(Field field : fields)
		{
			if(!isTransientField(field) && !isManyToManyField(field))
			{
				String fieldName = helper.getFieldName(field);
				busComp.activateField(fieldName);
			}
		}
		return;
	}

	private boolean isTransientField(Field field)
	{
		return field.getAnnotation(Transient.class) != null;
	}
	
	private boolean isManyToManyField(Field field)
	{
		return field.getAnnotation(ManyToMany.class) != null;
	}
	
	private boolean isKeyField(Field field)
	{
		return field.getAnnotation(Key.class) != null;
	}
	
	private boolean isReadOnlyField(Field field)
	{
		ReadOnly ro = field.getAnnotation(ReadOnly.class);
		
		return ro!=null;
	}
	

	/**
	 * Copy all values from Object -> BusinessComponent to prepare for search query
	 * used by:
	 * 		-siebelSelect
	 * 		-siebelListSelect
	 * @param busComp
	 * @param obj
	 * @param keyMatters (for distinguishing b/w plain select & select for an update)
	 * @return
	 * @throws SiebelException
	 * @throws IllegalArgumentException if all nulls are searched for (this would return all rows in all of Siebel and we don't want that!)
	 */
	private void setSearchSpecs(DssBusComp busComp, Object obj, boolean keyMatters) throws SiebelException
	{
		boolean emptyQuery = true;

		List<Field> fields = helper.getAllDeclaredInstanceFields(obj);
        for(Field field : fields)
		{
            field.setAccessible(true);
			if (!isTransientField(field) && !isManyToManyField(field) && !(keyMatters && !isKeyField(field)))
			{
				try
                {
                    if(field.get(obj) != null)
                    {
                    	Object fieldValueObject = helper.getFieldValueFromAccessibleField(obj, field);
                    	String fieldName = helper.getFieldName(field);
                    	String fieldValue = helper.convertFieldValueToSiebelValue(field,fieldValueObject);
                    	if(!Util.isBlank(fieldValue))
                    	{
                    		emptyQuery = false;
                    		busComp.setSearchSpec(fieldName,fieldValue);
                    	}
                    }
                }
                catch (IllegalAccessException e)
                {
                    throw new AssertionError("field " + field + " was forced to be accessible");
                }
			}
		}
		if(emptyQuery)
		{
			//it's ok not to check for null b/c we have to have @BusinessComp to be this far.  it's already been checked
			if(!obj.getClass().getAnnotation(BusinessComp.class).blankQuery())	
			{
				String errorMsg = "Blank Query (all nulls) found in SiebelDataBean for Object: " + obj.getClass().getName();
				throw new IllegalArgumentException(errorMsg);
			}
		}
	}

	/**
	 * Method takes data from a SiebelBusinessComponent after query and moves data back into the entity
	 * object.
	 * used by:
	 * 		-siebelSelect
	 * 		-siebelSubSelect
	 * @param busComp
	 * @param obj
	 * @return
	 * @throws Exception 
	 */
	private void copySearchResultsToEntityObject(DssBusComp busComp, Object obj)
	{
		List<Field> fields = helper.getAllDeclaredInstanceFields(obj);
        for(Field field : fields){
            field.setAccessible(true);
			if(!isTransientField(field) && !isManyToManyField(field)){
				String fieldName = helper.getFieldName(field);
				Class<?> fieldType = field.getType();
				String fieldValue = getFieldValue(busComp, fieldName);
				
			    Object convertedValue = helper.convertSiebelValueToFieldValue(fieldType, fieldValue);
				
			    helper.setFieldValueToAccessibleField(obj, field, convertedValue);
			}
		}
		return;
	}

    private String getFieldValue(DssBusComp busComp, String fieldName) 
    {
        try
        {
            return busComp.getFieldValue(fieldName);
        }
        catch(SiebelException e)
        {
            throw new SblioException("Error getting field "+fieldName, e);
        }
    }

	//************************************************************************
	//************************************************************************
	//SETUP QUERY/INSERT METHODS
	//************************************************************************
	//************************************************************************
	/**
	 * This method does the setup work for siebelSelect.  It loads a Siebel BusinessObject,
	 * and BusinessComponent & populates the BC with the appropriate searchSpecs.
	 * @param obj
	 * @param forUpdate
	 * @return
	 * @throws SiebelException
	 * @throws IllegalAccessException
	 * @throws FatalException -- could come from this method (failed query) or populateBusinessComp
	 */
	private void setupForQuery(Object obj, boolean forUpdate) throws SiebelException
	{
		loadBusinessObject(obj);
		loadBusinessComponent(obj);

    	prepareForQuery(mainBusComp, obj, forUpdate, true);
	}
	
	private void prepareForQuery(DssBusComp dssBusComp, Object obj, boolean forUpdate, boolean setViewMode) throws SiebelException
	{
		activateFields(dssBusComp, obj);

		if(setViewMode)
			dssBusComp.setViewMode(3);
		
		dssBusComp.clearToQuery();

		setSearchSpecs(dssBusComp, obj, forUpdate);
	}



	//************************************************************************
	//************************************************************************
	//LOAD BC/BO METHODS
	//************************************************************************
	//************************************************************************
	
	private BusinessComp getBusinessComponent(Object obj)
	{
		BusinessComp bc = obj.getClass().getAnnotation(BusinessComp.class);

		if(bc == null)
		{
			Class<?>[] classes = obj.getClass().getInterfaces();
			for(int i=0; (bc==null && i<classes.length); i++)
				bc = (BusinessComp) classes[i].getAnnotation(BusinessComp.class);
		}

		return bc;
	}

	private void loadBusinessComponent(Object obj) throws SiebelException
	{
		BusinessComp bc = getBusinessComponent(obj);
		
		if(bc != null)
		{
			mainBusComp = new DssBusComp(busObj.getBusComp(bc.name()), bc.name());
		}
		
		return;
	}

	private void loadBusinessObject(Object obj) throws SiebelException
	{
		BusinessObject bo = obj.getClass().getAnnotation(BusinessObject.class);
		if(bo == null)
		{
			Class<?>[] classes = obj.getClass().getInterfaces();
			for(int i=0; (bo==null && i<classes.length); i++)
			{
				bo = (BusinessObject) classes[i].getAnnotation(BusinessObject.class);
			}
		}
		
		if(bo != null)
		{
			String boName = bo.name();
			busObj = databean.getBusObject(boName);
		}

		return;
	}


	public void reset()
	{
		if(mainBusComp!=null)mainBusComp.release();
		if(busObj!=null)busObj.release();

		busObj = null;
		mainBusComp = null;
	}

	public String getSystem()
	{
		return system;
	}

	public String getUsername()
	{
		return username;
	}

	public SiebelDataBean getDataBean()
	{
		return this.databean;
	}
}
