package org.ccci.framework.sblio;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ccci.exceptions.FatalException;
import org.ccci.framework.sblio.annotations.BusinessComp;
import org.ccci.framework.sblio.annotations.BusinessObject;
import org.ccci.framework.sblio.annotations.ChildBusinessCompField;
import org.ccci.framework.sblio.annotations.MvgField;
import org.ccci.framework.sblio.exceptions.SiebelUnavailableException;

import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelDataBean;
import com.siebel.data.SiebelException;

/**
 * 
 * See "Java Data Bean Quick Reference" at
 * http://download.oracle.com/docs/cd/B40099_02/books/OIRef/OIRefJava_QR.html
 * for elaboration on the jdb api. The javadocs are not very complete.
 * 
 * This class is probably not threadsafe; only one thread should use an
 * SiebelPersistence instance at a time.
 * 
 * @author Ryan Carlson
 * @author Matt Drees
 * @author Lee Braddock
 * @author Nathan Kopp
 */
public class SiebelPersistenceImpl implements SiebelPersistence
{
	// private static Logger siebelLog = Logger.getLogger("Siebel");

	public static String NAME = "SiebelPersistence";
	private String system;
	private String username;
	private String url;

	private SiebelDataBean databean;

	static
	{
		/*
		 * Siebel jdb does not like it if the file.encoding property is set to
		 * the Mac's default encoding, MacRoman. See
		 * http://blog.ideaportriga.org/java/?p=78 for more info.
		 */
		if (System.getProperty("file.encoding").equals("MacRoman"))
		{
			System.setProperty("file.encoding", "utf8");
		}
	}

	public SiebelPersistenceImpl(String username, String password, String url)
	{
		databean = new SiebelDataBean();
		try
		{
			this.username = username;
			this.url = url;
			databean.login(url, username, password, "enu");
		}
		catch (SiebelException se)
		{
			throw new SiebelUnavailableException(se);
		}
	}

	/**
	 * construct which creates internal databean and logs it into Siebel takes
	 * system (ie: DEV) & username (ie: DSSJAVA) and looks up password in
	 * Siebel.properties
	 * 
	 * @param system
	 * @param username
	 */
	public SiebelPersistenceImpl(String system, String username)
	{
		this.system = system;
		this.username = username;
		this.url = (String) SiebelSettings.props.get(system + ".url");

		String password = (String) SiebelSettings.props.get(system + "." + username);

		databean = new SiebelDataBean();

		try
		{
			databean.login(url.toString(), username, password, "enu");
		}
		catch (SiebelException se)
		{
			throw new SiebelUnavailableException(se);
		}
	}

	public int siebelSelect(Object obj)
	{
		List<Object> list = siebelListSelect(obj);

		return list.size();
	}

	public <T> List<T> siebelListSelect(T obj)
	{
		return new ArrayList<T>(siebelCollectionSelect(obj));
	}

	public <T> Collection<T> siebelCollectionSelect(T obj)
	{
		BusComp mainBusComp = null;
		try
		{
			if (obj == null)
				throw new NullPointerException("query object is null");

			mainBusComp = loadCompAndObj(obj);

			return siebelCollectionSelect(mainBusComp, obj);
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform list select on " + obj, e);
		}
		finally
		{
			if (mainBusComp != null)
				mainBusComp.release();
		}
	}

	private <T> Collection<T> siebelCollectionSelect(BusComp busComp, T object)
	{
		try
		{
			if (object == null)
				throw new NullPointerException("query object is null");

			Collection<T> collection = new ArrayList<T>();

			busComp.prepareForQuery(object, false, true);
			busComp.executeQuery(false);

			if (busComp.firstRecord())
			{
				@SuppressWarnings("unchecked")
				Class<T> objType = (Class<T>) object.getClass();
				do
				{
					T tempObj = instantiate(objType);

					copySearchResultsToEntityObject(busComp, tempObj);
					if (collection.size() == 0)
						copySearchResultsToEntityObject(busComp, object);

					List<T> objects = new ArrayList<T>();
					objects.add(tempObj);
					if (collection.size() == 0)
						objects.add(object);

					cascadeLoadRelationships(busComp, objects);

					collection.add(tempObj);
				}
				while (busComp.nextRecord());
			}

			return collection;

		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform select on " + object, e);
		}
	}

	private <T> void cascadeLoadRelationships(BusComp parentBusComp, List<T> parentObjects)
	{
		try
		{
			List<Field> fs = SiebelHelper.getAllDeclaredInstanceFields(parentObjects.get(0));
			for (Field f : fs)
			{
				f.setAccessible(true);

				ChildBusinessCompField childBusinessCompField = f.getAnnotation(ChildBusinessCompField.class);
				if (childBusinessCompField != null && childBusinessCompField.cascadeLoad())
				{
					BusComp childBusComp = parentBusComp.getChildBusComp(SiebelUtil.determineSiebelFieldNameForChildBusinessCompField(parentObjects.get(0), f.getName()));
					Collection<?> siebelSelection = siebelCollectionSelect(childBusComp, childBusinessCompField.clazz().newInstance());
					for(Object parentObject : parentObjects)
						f.set(parentObject, siebelSelection);
					childBusComp.release();
				}

				MvgField fieldMetadata = f.getAnnotation(MvgField.class);
				if (fieldMetadata != null && fieldMetadata.cascadeLoad())
				{
					String siebelName = SiebelUtil.determineSiebelFieldNameForMvgField(parentObjects.get(0), f.getName());
					Collection<Object> siebelSelection = siebelSelectMvg(parentBusComp, siebelName, fieldMetadata.clazz().newInstance());
					for(Object parentObject : parentObjects)
						f.set(parentObject, siebelSelection);
				}
			}
		}
		catch (Exception e)
		{
			throw new SblioException("Unable to perform select on " + parentObjects.get(0), e);
		}
	}

	/**
	 * Iterate through the instances of mvgObject in the context of the
	 * specified business component
	 * 
	 * @param <T>
	 * @param busComp
	 * @param exampleChildObject
	 * @return
	 */
	private <T> Collection<T> siebelSelectMvg(BusComp busComp, String fieldName, T exampleChildObject)
	{
		if (exampleChildObject == null)
			throw new NullPointerException("query object is null");

		BusComp tempBusComp = null;

		try
		{
			Collection<T> collection = new ArrayList<T>();

			tempBusComp = new BusComp(busComp.getMVGBusComp(fieldName));

			tempBusComp.prepareForQuery(exampleChildObject, false, true);

			tempBusComp.executeQuery2(true, true);

			if (tempBusComp.firstRecord())
			{
				@SuppressWarnings("unchecked")
				Class<T> objType = (Class<T>) exampleChildObject.getClass();
				do
				{
					T tempObj = instantiate(objType);
					copySearchResultsToEntityObject(tempBusComp, tempObj);
					collection.add(tempObj);
				}
				while (tempBusComp.nextRecord());
			}

			return collection;

		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform select on " + exampleChildObject, e);
		}
		finally
		{
			if (tempBusComp != null)
				tempBusComp.release();
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
			throw new IllegalArgumentException(String.format("can't instantiate object of type %s; make sure it's not abstract or an interface", objType.getName()), e);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalArgumentException(String.format("can't instantiate object of type %s; make sure it has a public no-argument constructor", objType.getName()), e);
		}
	}

	public String siebelInsert(Object obj)
	{
		BusComp mainBusComp = null;
		try
		{
			mainBusComp = loadCompAndObj(obj);

			return siebelInsert(mainBusComp, obj);
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform insert on " + obj, e);
		}
		finally
		{
			if (mainBusComp != null)
				mainBusComp.release();
		}
	}

	private String siebelInsert(BusComp busComp, Object obj)
	{
		try
		{
			busComp.newRecord(false);
			setFieldsForInsert(busComp, obj);
			if (!busComp.writeRecord())
				throw new SblioException("insert did not succeed");

			return busComp.getFieldValue("Id");
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform insert on " + obj, e);
		}
	}

	public String siebelUpsert(Object obj)
	{
		BusComp mainBusComp = null;
		try
		{
			mainBusComp = setupForQuery(obj, true);
			mainBusComp.executeQuery(false);

			if (mainBusComp.firstRecord())
			{
				setFieldsForUpdate(mainBusComp, obj);
				mainBusComp.writeRecord();
				return mainBusComp.getFieldValue("Id");
			}
			else
				return siebelInsert(mainBusComp, obj);
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform upsert on " + obj, e);
		}
		finally
		{
			if (mainBusComp != null)
				mainBusComp.release();
		}
	}

	public String siebelInsertChildField(Object parent, String fieldName, Object child)
	{
		BusComp busComp = null;
		BusComp childBusComp = null;
		try
		{
			ChildBusinessCompField fieldMetadata = SiebelHelper.getField(parent.getClass(), fieldName).getAnnotation(ChildBusinessCompField.class);
			if (fieldMetadata == null || !fieldMetadata.manyToMany())
				throw new SblioException("Child field is not many to many " + child + " for parent " + parent);

			busComp = loadCompAndObj(parent);

			siebelCollectionSelect(busComp, parent);

			childBusComp = busComp.getChildBusComp(SiebelUtil.determineSiebelFieldNameForChildBusinessCompField(parent, fieldName));

			return siebelInsert(childBusComp, child);
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform insert on parent " + parent + " and child " + child, e);
		}
		finally
		{
			if (busComp != null)
				busComp.release();
			if (childBusComp != null)
				childBusComp.release();
		}
	}

	public String siebelInsertMvgField(Object parentObj, String fieldName, Object recordForUpsert)
	{
		return siebelUpsertMvgField(parentObj, fieldName, recordForUpsert, true);
	}

	public String siebelUpsertMvgField(Object parentObj, String fieldName, Object recordForUpsert)
	{
		return siebelUpsertMvgField(parentObj, fieldName, recordForUpsert, false);
	}

	private String siebelUpsertMvgField(Object parentObj, String fieldName, Object recordForUpsert, boolean forceInsert)
	{
		BusComp mvgBusComp = null;
		BusComp assocBusComp = null;
		String rowId = "";

		if (parentObj != null)
		{
			BusComp mainBusComp = null;
			try
			{
				mainBusComp = setupForQuery(parentObj, true);

				mainBusComp.executeQuery(false);

				if (mainBusComp.firstRecord())
				{
					Field f = SiebelHelper.getField(parentObj.getClass(), fieldName);
					MvgField fieldMetadata = f.getAnnotation(MvgField.class);

					String siebelFieldName = SiebelUtil.determineSiebelFieldNameForMvgField(parentObj, fieldName);
					mvgBusComp = new BusComp(mainBusComp.getMVGBusComp(siebelFieldName));

					if (fieldMetadata != null && fieldMetadata.manyToMany())
					{
						assocBusComp = new BusComp(mvgBusComp.getAssocBusComp());

						assocBusComp.prepareForQuery(recordForUpsert, true, false);

						assocBusComp.executeQuery2(true, true);

						if (assocBusComp.firstRecord())
						{
							assocBusComp.associate(true);
							assocBusComp.writeRecord();
							mainBusComp.writeRecord();

							mvgBusComp.prepareForQuery(recordForUpsert, true, false);
							mvgBusComp.executeQuery2(true, true);
							if (mvgBusComp.firstRecord())
							{
								setFieldsForUpdate(mvgBusComp, recordForUpsert);
								mvgBusComp.writeRecord();
								mainBusComp.writeRecord();
							}
						}
					}

					else if (!forceInsert && searchForMatchInMvg(mvgBusComp, recordForUpsert)) // update
					{
						setFieldsForUpdate(mvgBusComp, recordForUpsert);
						mvgBusComp.writeRecord();
						mainBusComp.writeRecord();
					}
					else
					// insert
					{
						mvgBusComp.newRecord(true);
						setFieldsForInsert(mvgBusComp, recordForUpsert);
						mvgBusComp.writeRecord();
						mainBusComp.writeRecord();
					}
					rowId = mvgBusComp.getFieldValue("Id");
				}
			}
			catch (SiebelException e)
			{
				throw new SblioException("unable to upsert: " + recordForUpsert, e);
			}
			finally
			{
				if (mvgBusComp != null)
					mvgBusComp.release();

				if (assocBusComp != null)
					assocBusComp.release();

				if (mainBusComp != null)
					mainBusComp.release();
			}
		}

		return rowId;
	}

	public boolean siebelDelete(Object obj)
	{
		BusComp mainBusComp = null;
		try
		{
			mainBusComp = setupForQuery(obj, true);

			mainBusComp.executeQuery(false);

			if (mainBusComp.firstRecord())
			{
				return mainBusComp.deleteRecord();
			}

			else
				return false;
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to perform delete on " + obj, e);
		}
		finally
		{
			if (mainBusComp != null)
				mainBusComp.release();
		}
	}

	public boolean siebelDeleteAll(Object obj)
	{
		BusComp mainBusComp = null;
		try
		{
			mainBusComp = setupForQuery(obj, false);

			mainBusComp.executeQuery(false);

			if (mainBusComp.firstRecord())
			{
				do
				{
					mainBusComp.deleteRecord();
				}
				while (mainBusComp.nextRecord());
				return true;
			}

			else
				return false;
		}
		catch (SiebelException e)
		{
			throw new SblioException("Unable to delete " + obj, e);
		}
		finally
		{
			if (mainBusComp != null)
				mainBusComp.release();
		}
	}

	public boolean siebelDeleteMvgField(Object parentObj, String fieldName, Object recordForDelete)
	{
		if (parentObj != null)
		{
			BusComp mvgBusComp = null;
			BusComp mainBusComp = null;

			try
			{
				mainBusComp = setupForQuery(parentObj, true);

				mainBusComp.executeQuery(false);

				if (mainBusComp.firstRecord())
				{
					String siebelFieldName = SiebelUtil.determineSiebelFieldNameForMvgField(parentObj, fieldName);

					mvgBusComp = new BusComp(mainBusComp.getMVGBusComp(siebelFieldName));
					if (searchForMatchInMvg(mvgBusComp, recordForDelete))
					{
						mvgBusComp.deleteRecord();
						mainBusComp.writeRecord();
					}
				}
			}
			catch (SiebelException e)
			{
				throw new SblioException("unable to delete mvg field " + recordForDelete, e);
			}
			finally
			{
				if (mvgBusComp != null)
					mvgBusComp.release();

				if (mainBusComp != null)
					mainBusComp.release();
			}
		}

		return false;
	}

	public boolean siebelSynchronize(SiebelSynchronizable exampleObj, List<? extends SiebelSynchronizable> records)
	{
		BusComp mainBusComp = null;
		try
		{
			mainBusComp = setupForQuery(exampleObj, false);

			mainBusComp.executeQuery(false);
			if (mainBusComp.firstRecord())
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
			if (mainBusComp != null)
				mainBusComp.release();
		}

		return true;
	}

	public SiebelServiceWrapper getService(String serviceName)
	{
		try
		{
			return new SiebelServiceWrapper(databean.getService(serviceName));
		}
		catch (SiebelException e)
		{
			throw new SblioException("unable to retrieve service " + serviceName, e);
		}
	}

	/**
	 * This method will remove rows in Siebel that don't have an exact matching
	 * row in the passed thru @param externalDataRows UNLESS: that row is
	 * defined as an IgnoredType in interface code.
	 * 
	 * @param busComp
	 * @param exampleObject
	 * @param externalDataRows
	 * @throws SiebelException
	 */
	private void deleteRowsNotInPropertySet(BusComp busComp, SiebelSynchronizable exampleObject, List<? extends SiebelSynchronizable> externalDataRows) throws SiebelException
	{
		boolean moreRecords = true;
		do

		{
			copySearchResultsToEntityObject(busComp, exampleObject);
			if (!exampleObject.shouldIgnore() && !externalDataRows.contains(exampleObject))
			{
				busComp.deleteRecord();
				moreRecords = busComp.firstRecord();
			}
			else
			{
				moreRecords = busComp.nextRecord();
			}
		}
		while (moreRecords);

		return;
	}

	private void insertRowsNotInBusComp(BusComp busComp, List<?> propertySet) throws SiebelException
	{
		for (Object o : propertySet)
		{
			busComp.clearToQuery();
			busComp.setSearchSpecs(o, true);
			busComp.executeQuery(false);
			if (!busComp.firstRecord())
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

	private boolean searchForMatchInMvg(BusComp mvgBusComp, Object recordForInsert) throws SiebelException
	{
		boolean found = false;
		if (mvgBusComp.firstRecord())
		{
			mvgBusComp.setSearchSpecs(recordForInsert, true);
			found = mvgBusComp.executeQuery(true) && mvgBusComp.firstRecord();
		}
		return found;
	}

	// ************************************************************************
	// ************************************************************************
	// SET FIELDS IN BUSCOMP'S FOR INSERT/UPDATE
	// ************************************************************************
	// ************************************************************************
	private void setFieldsForUpdate(BusComp busComp, Object obj) throws SiebelException
	{
		List<Field> fields = SiebelHelper.getAllDeclaredInstanceFields(obj);
		for (Field field : fields)
		{
			field.setAccessible(true);
			if (!SiebelUtil.isTransientField(field) && !SiebelUtil.isMvgField(field) && !SiebelUtil.isChildBusinessCompField(field) && !SiebelUtil.isReadOnlyField(field))
			{
				String fieldName = SiebelHelper.getFieldName(field);
				Object fieldValueObject = SiebelHelper.getFieldValueFromAccessibleField(obj, field);
				if (!field.getType().getName().equals("java.util.List"))
				{
					if (fieldValueObject == null)
					{
						busComp.setFieldValue(fieldName, "");
					}
					else
					{
						String fieldValue = SiebelHelper.convertFieldValueToSiebelValue(field, fieldValueObject);
						busComp.setFieldValue(fieldName, fieldValue);
					}
				}
			}
		}
		return;
	}

	private void setFieldsForInsert(BusComp busComp, Object obj) throws SiebelException
	{
		List<Field> fields = SiebelHelper.getAllDeclaredInstanceFields(obj);
		for (Field field : fields)
		{
			field.setAccessible(true);
			if (!SiebelUtil.isTransientField(field) && !SiebelUtil.isMvgField(field) && !SiebelUtil.isChildBusinessCompField(field) && !SiebelUtil.isReadOnlyField(field))
			{
				String fieldName = SiebelHelper.getFieldName(field);
				Object fieldValueObject = SiebelHelper.getFieldValueFromAccessibleField(obj, field);

				if (fieldValueObject != null && !field.getType().equals(List.class))
				{
					String fieldValue = SiebelHelper.convertFieldValueToSiebelValue(field, fieldValueObject);
					busComp.setFieldValue(fieldName, fieldValue);
				}
			}
		}
		return;
	}

	// ************************************************************************
	// ************************************************************************
	// HELPER METHODS- COMMON LOGIC
	// ************************************************************************
	// ************************************************************************

	/**
	 * Method takes data from a SiebelBusinessComponent after query and moves
	 * data back into the entity object. used by: -siebelSelect -siebelSubSelect
	 * 
	 * @param busComp
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	private void copySearchResultsToEntityObject(BusComp busComp, Object obj)
	{
		List<Field> fields = SiebelHelper.getAllDeclaredInstanceFields(obj);
		for (Field field : fields)
		{
			field.setAccessible(true);
			if (!SiebelUtil.isTransientField(field) && !SiebelUtil.isMvgField(field) && !SiebelUtil.isChildBusinessCompField(field))
			{
				String fieldName = SiebelHelper.getFieldName(field);
				Class<?> fieldType = field.getType();
				String fieldValue = getFieldValue(busComp, fieldName);

				Object convertedValue = SiebelHelper.convertSiebelValueToFieldValue(fieldType, fieldValue);

				SiebelHelper.setFieldValueToAccessibleField(obj, field, convertedValue);
			}
		}
		return;
	}

	private String getFieldValue(BusComp busComp, String fieldName)
	{
		try
		{
			return busComp.getFieldValue(fieldName);
		}
		catch (SiebelException e)
		{
			throw new SblioException("Error getting field " + fieldName, e);
		}
	}

	// ************************************************************************
	// ************************************************************************
	// SETUP QUERY/INSERT METHODS
	// ************************************************************************
	// ************************************************************************
	/**
	 * This method does the setup work for siebelSelect. It loads a Siebel
	 * BusinessObject, and BusinessComponent & populates the BC with the
	 * appropriate searchSpecs.
	 * 
	 * @param obj
	 * @param forUpdate
	 * @return
	 * @throws SiebelException
	 * @throws IllegalAccessException
	 * @throws FatalException -- could come from this method (failed query) or
	 *             populateBusinessComp
	 */
	private BusComp setupForQuery(Object obj, boolean forUpdate) throws SiebelException
	{
		BusComp busComp = loadCompAndObj(obj);

		busComp.prepareForQuery(obj, forUpdate, true);
		return busComp;
	}

	private BusComp loadCompAndObj(Object obj) throws SiebelException
	{
		SiebelBusObject busObj = loadBusinessObject(obj.getClass());
		return loadBusinessComponent(obj.getClass(), busObj);
	}

	private BusinessComp getBusinessComponent(Class<?> type)
	{
		BusinessComp bc = type.getAnnotation(BusinessComp.class);

		if (bc == null)
		{
			Class<?>[] classes = type.getInterfaces();
			for (int i = 0; (bc == null && i < classes.length); i++)
				bc = (BusinessComp) classes[i].getAnnotation(BusinessComp.class);
		}

		return bc;
	}

	private BusComp loadBusinessComponent(Class<?> type, SiebelBusObject busObj) throws SiebelException
	{
		BusinessComp bc = getBusinessComponent(type);

		if (bc != null)
		{
			return new BusComp(busObj.getBusComp(bc.name()), busObj);
		}

		return null;
	}

	private SiebelBusObject loadBusinessObject(Class<?> type) throws SiebelException
	{
		BusinessObject bo = type.getAnnotation(BusinessObject.class);
		if (bo == null)
		{
			Class<?>[] classes = type.getInterfaces();
			for (int i = 0; (bo == null && i < classes.length); i++)
			{
				bo = (BusinessObject) classes[i].getAnnotation(BusinessObject.class);
			}
		}

		if (bo != null)
		{
			String boName = bo.name();
			return databean.getBusObject(boName);
		}

		return null;
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

	public void reset()
	{
		// do nothing currently
	}

	public void close() throws Exception
	{
		databean.logoff();
		databean = null;
	}

	@Override
	protected void finalize() throws Throwable
	{
		// TODO Auto-generated method stub
		super.finalize();
		if (databean != null)
			databean.logoff();
	}
}
