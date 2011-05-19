package org.ccci.framework.sblio;

import java.util.List;

import com.siebel.data.SiebelException;

/**
 * 
 * @author Ryan Carlson
 * @author Matt Drees
 *
 */
public interface SiebelPersistence
{
    /**
     * This method takes an object with certain fields populated with data
     * and performs a query based on the data in those fields.  Similar to
     * a dbio.objSelect(obj) statement.  We only expect to return one record.
     * 
     * @param object the object that both determines the query and houses the returned data
     * @return 0 if there were no matching rows; 
     *      1 if exactly one matched the query; 
     *      2 if more than one row matched (in this case, the object will be populated with the first row's data)
     * @throws SblioException if a {@link SiebelException} was thrown
     */
	public int siebelSelect(Object object);
	
	/**
	 * Builds a query from the given object, queries Siebel for matching rows, and returns these rows as java objects of the same type as the given query object.
	 * The query is constructed exactly as it is constructed by {@link #siebelSelect(Object)}, but in this case, the query object is not
	 * populated with any of the result rows.
	 * 
	 * @param <T> the type of the query object, and correspondingly, the type of the returned objects
	 * @param object determines the query based on its populated fields
	 * @return a {@link List} of objects built from the rows returned from Siebel
	 * @throws SblioException if a {@link SiebelException} was thrown
	 */
	public <T> List<T> siebelListSelect(T object);
	
	/**
	 * Inserts a row into the corresponding Siebel business component, built from the given object
	 * 
	 * @param object the row to be inserted
	 * @return the id assigned by Siebel for the newly inserted row 
	 * @throws SblioException if a {@link SiebelException} was thrown, or if the insert failed
     */
	public String siebelInsert(Object object);
	
	/**
	 * If a row exists with the same key as the given object, then update it with the given object's data; otherwise, insert
	 * a new row with the given object's data. 
	 * 
	 * @param object
	 * @return the id of the row (which may have just been created)
     * @throws SblioException if a {@link SiebelException} was thrown
	 */
	public String siebelUpsert(Object object);
	
	public String siebelInsertMvgField(Object parentObj, String fieldName, Object recordForUpsert);
	public String siebelUpsertMvgField(Object parentObj, String fieldName, Object recordForUpsert);
	
	/**
	 * If a parent row exists and the..
	 * @param parentObj
	 * @param childObj
	 * @return
	 */
	public String siebelSetMvgPrimaryRecord(Object parentObj, String fieldName, Object childObj);
	/**
	 * Deletes the row corresponding to the given object.
	 * A siebel query is executed for rows that match the given object (as in {@link #siebelSelect(Object)}, and then the first matching
	 * row is deleted; any others are ignored.
	 * 
	 * @param object
	 * @return true if a row was found and deleted; false otherwise
	 * @throws SblioException if a {@link SiebelException} was thrown, or if the delete failed
	 */
	public boolean siebelDelete(Object object);
	
	public boolean siebelDeleteAll(Object obj);
	public boolean siebelDeleteMvgField(Object parentObj, String fieldName, Object recordForDelete);
	public boolean siebelSynchronize(SiebelSynchronizable exampleObj, List<? extends SiebelSynchronizable> records);	

	
	public SiebelServiceWrapper getService(String serviceName);
	public com.siebel.data.SiebelDataBean getDataBean();
	public String getUsername();
	public String getSystem();

    public void reset();
    
    public void close() throws Exception;
}
