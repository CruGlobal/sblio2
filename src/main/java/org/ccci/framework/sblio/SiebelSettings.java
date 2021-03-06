package org.ccci.framework.sblio;

import com.google.common.io.Resources;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class SiebelSettings
{
    private static Logger siebelLogger = Logger.getLogger("Siebel");

    static final String siebelPropertiesFilename = "Sblio.properties";
    
    // package-level properties
    static Properties props;
    static List pooledUsers;
    static int poolSize;

    static
    {
        try
        {
        	reloadProperties();
        }
        catch(IOException e)
        {
            siebelLogger.error(e.getMessage());
            props = null;
        }
    }
        
    /**
     * 
     * @throws IOException
     */
    public static void reloadProperties() throws IOException
    {
    	props = new Properties();
        InputStream propertiesStream = Resources.getResource("/" + siebelPropertiesFilename).openStream();
        try
        {
            props.load(propertiesStream);
        }
        finally
        {
            propertiesStream.close();
        }
        
        siebelLogger.debug("SiebelSettings:reloadProperties()  loaded properties file");
        
        // load the list of users that we should pool... this is actually
        // used by DbioList.  the list is also converted to lowercase for
        // easier comparison
        String pooledUserList = props.getProperty("pooledUserList");
        pooledUsers = pooledUserList == null ?
           Collections.emptyList() :
           Arrays.asList(pooledUserList.trim().split("\\s*,\\s*"));

        for(int i=0; i<pooledUsers.size(); i++)
        {
            pooledUsers.set(i,((String)pooledUsers.get(i)).toLowerCase());
            siebelLogger.debug("SiebelSettings:reloadProperties()  pooled user: "+pooledUsers.get(i));
        }
        String strPoolSize = props.getProperty("poolSize");
        poolSize = 1;
        try
        {
            if (strPoolSize!=null) poolSize = Integer.parseInt(strPoolSize);
        }
        catch(Exception e)
        {
            poolSize = 1;
        }
    }

    /**
     * @return Returns the dbioLog.
     */
    public static Logger getDbioLog()
    {
        return siebelLogger;
    }
    /**
     * @return Returns the pooledUsers.
     */
    public static List getPooledUsers()
    {
        return pooledUsers;
    }
    /**
     * @return Returns the poolSize.
     */
    public static int getPoolSize()
    {
        return poolSize;
    }
    /**
     * @return Returns the props.
     */
    public static Properties getProps()
    {
        return props;
    }
}
