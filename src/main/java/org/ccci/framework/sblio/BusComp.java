package org.ccci.framework.sblio;

import com.google.common.base.Strings;
import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelBusObject;
import com.siebel.data.SiebelException;
import org.ccci.framework.sblio.annotations.BusinessComp;

import java.lang.reflect.Field;
import java.util.List;

public class BusComp
{
	private SiebelBusComp busComp;
	private boolean isAlreadyQueried = false;
	private SiebelBusObject associatedBusObj;
	
	public BusComp(SiebelBusComp busComp, SiebelBusObject associtedBusObj)
	{
		this.busComp = busComp;
		this.associatedBusObj = associtedBusObj;
	}
	
	public BusComp(SiebelBusComp busComp)
    {
        this(busComp, null);
    }
	
	//getters & setters
	
	public BusComp getChildBusComp(String childBusCompName) throws SiebelException
	{
		return new BusComp(associatedBusObj.getBusComp(childBusCompName));
	}

	public boolean isAlreadyQueried() {
		return isAlreadyQueried;
	}

	public void setAlreadyQueried(boolean isAlreadyQueried) {
		this.isAlreadyQueried = isAlreadyQueried;
	}

	//delegate methods
	
	public boolean activateField(String arg0) throws SiebelException {
		return busComp.activateField(arg0);
	}

	public boolean clearToQuery() throws SiebelException {
		return busComp.clearToQuery();
	}

	public boolean deleteRecord() throws SiebelException
	{
		return busComp.deleteRecord();
	}
	
	public boolean executeQuery(boolean arg0) throws SiebelException {
		this.isAlreadyQueried = busComp.executeQuery(arg0);
		return isAlreadyQueried;
	}

	public boolean executeQuery2(boolean arg0, boolean arg1) throws SiebelException {
		this.isAlreadyQueried = busComp.executeQuery2(arg0, arg1);
		return isAlreadyQueried;
	}

	public boolean firstRecord() throws SiebelException {
		return busComp.firstRecord();
	}

	public String getFieldValue(String arg0) throws SiebelException {
		return busComp.getFieldValue(arg0);
	}

	public SiebelBusComp getMVGBusComp(String arg0) throws SiebelException
	{
		return busComp.getMVGBusComp(arg0);
	}
	
	public SiebelBusComp getAssocBusComp() throws SiebelException
	{
		return busComp.getAssocBusComp();
	}
	
	public SiebelBusComp getPicklistBusComp(String arg0) throws SiebelException {
		return busComp.getPicklistBusComp(arg0);
	}

	public String name() {
		return busComp.name();
	}

	public boolean newRecord(boolean arg0) throws SiebelException
	{
		return busComp.newRecord(arg0);
	}
	
	public boolean nextRecord() throws SiebelException {
		return busComp.nextRecord();
	}

	public void release() {
		busComp.release();
		if(associatedBusObj!=null) associatedBusObj.release();
	}

	public boolean setFieldValue(String fieldName, String fieldValue) throws SiebelException
	{
		return busComp.setFieldValue(fieldName, fieldValue);
	}
	
	public boolean setSearchExpr(String arg0)throws SiebelException
	{
		return busComp.setSearchExpr(arg0);
	}
	
	public boolean setSearchSpec(String arg0, String arg1)
			throws SiebelException {
		return busComp.setSearchSpec(arg0, arg1);
	}
	
	public boolean setSortSpec(String arg0) throws SiebelException
	{
		return busComp.setSortSpec(arg0);
	}

	public boolean setViewMode(int arg0) throws SiebelException {
		return busComp.setViewMode(arg0);
	}
	
	public boolean writeRecord() throws SiebelException
	{
		return busComp.writeRecord();
	}
	
	public boolean associate(boolean value) throws SiebelException {
		return busComp.associate(value);
	}

    void activateFields(Object obj) throws SiebelException
    {
    	List<Field> fields = SiebelHelper.getAllDeclaredInstanceFields(obj);
    	for(Field field : fields)
    	{
    		if(!SiebelUtil.isTransientField(field) && !SiebelUtil.isMvgField(field) && !SiebelUtil.isChildBusinessCompField(field))
    		{
    			String fieldName = SiebelHelper.getFieldName(field);
    			activateField(fieldName);
    		}
    	}
    	return;
    }

    public void prepareForQuery(Object obj, boolean forUpdate, boolean setViewMode) throws SiebelException
    {
    	activateFields(obj);
    
    	if(setViewMode)
    		setViewMode(3);
    	
    	clearToQuery();
    
    	setSearchSpecs(obj, forUpdate);
    }

    /**
     * Copy all values from Object -> BusinessComponent to prepare for search query
     * used by:
     * 		-siebelSelect
     * 		-siebelListSelect
     * @param obj
     * @param keyMatters (for distinguishing b/w plain select & select for an update)
     * @return
     * @throws SiebelException
     * @throws IllegalArgumentException if all nulls are searched for (this would return all rows in all of Siebel and we don't want that!)
     */
    void setSearchSpecs(Object obj, boolean keyMatters) throws SiebelException
    {
    	boolean emptyQuery = true;
    
    	List<Field> fields = SiebelHelper.getAllDeclaredInstanceFields(obj);
        for(Field field : fields)
    	{
            field.setAccessible(true);
    		if (!SiebelUtil.isTransientField(field) && !SiebelUtil.isMvgField(field) && !SiebelUtil.isChildBusinessCompField(field) && !(keyMatters && !SiebelUtil.isKeyField(field)))
    		{
    			try
                {
                    if(field.get(obj) != null)
                    {
                    	Object fieldValueObject = SiebelHelper.getFieldValueFromAccessibleField(obj, field);
                    	String fieldName = SiebelHelper.getFieldName(field);
                    	String fieldValue = SiebelHelper.convertFieldValueToSiebelValue(field,fieldValueObject);
                    	if(!Strings.isNullOrEmpty(fieldValue))
                    	{
                    		emptyQuery = false;
                    		setSearchSpec(fieldName,fieldValue);
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
}
