package org.ccci.framework.sblio.obj;

import org.ccci.framework.sblio.annotations.BusinessComp;
import org.ccci.framework.sblio.annotations.BusinessObject;
import org.ccci.framework.sblio.annotations.Key;
import org.ccci.framework.sblio.annotations.ReadOnly;

@BusinessObject(name = "Users")
@BusinessComp(name = "User")
public class SiebelUser
{
    @Key @ReadOnly
    private String id;

    private String loginName;

    private String firstName;
    private String lastName;
    private String personalTitle;
    private String employeeFlag;

    private String accountId;
    
    public static SiebelUser newForTest(String id, String loginName)
    {
        SiebelUser user = new SiebelUser();
        user.id = id;
        user.loginName = loginName;
        return user;
    }
    
    public SiebelUser()
    {
    }
    

    public String getLoginName()
    {
        return loginName;
    }

    public void setLoginName(String loginName)
    {
        this.loginName = loginName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setPersonalTitle(String personalTitle)
    {
        this.personalTitle = personalTitle;
    }

    public String getPersonalTitle()
    {
        return personalTitle;
    }

    public void setEmployeeFlag(String employeeFlag)
    {
        this.employeeFlag = employeeFlag;
    }

    public String getEmployeeFlag()
    {
        return employeeFlag;
    }

    public void setAccountId(String accountId)
    {
        this.accountId = accountId;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public void setId(String rowId)
    {
        this.id = rowId;
    }

    public String getId()
    {
        return id;
    }
    
    public String toString()
    {
    	return id + "," + loginName + "," + firstName + "," + lastName + "," + personalTitle + "," + employeeFlag + "," + accountId;    	
    }
}
