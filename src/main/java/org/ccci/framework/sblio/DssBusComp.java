package org.ccci.framework.sblio;

import com.siebel.data.SiebelBusComp;
import com.siebel.data.SiebelException;

public class DssBusComp
{
	private SiebelBusComp busComp;
	private boolean isAlreadyQueried = false;
	private String name;
	
	public DssBusComp(SiebelBusComp busComp)
	{
		this(busComp,null);
	}

	public DssBusComp(SiebelBusComp busComp, String name)
	{
		this.busComp = busComp;
		this.name = name;
	}

	
	//getters & setters
	
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

	public String getName()
	{
		return name;
	}

}
