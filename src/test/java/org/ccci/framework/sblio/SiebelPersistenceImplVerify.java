package org.ccci.framework.sblio;

import java.io.File;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.ccci.framework.sblio.obj.SiebelUser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SiebelPersistenceImplVerify
{
	private static String dssUser = "idm";
	private static String dssUrl = "Siebel.tcpip.none.none://harta807:2321/SBLCRMT/EAIObjMgr_enu";
	private static String dssPasswordFile = "C:\\apps\\apps-config\\siebel.txt";

	private static SiebelPersistence persistence = null;

	public SiebelPersistenceImplVerify() throws Exception
	{
		super();
	}

	@BeforeClass
	public static void startup() throws Exception
	{
		String dssPassword = read(new File(dssPasswordFile), "\n");

		persistence = new SiebelPersistenceImpl(dssUser, dssPassword, dssUrl);
	}

	@AfterClass
	public static void shutdown() throws Exception
	{
		if (persistence != null)
			persistence.close();

		persistence = null;
	}

	@Test
	public void getSiebelUserByLoginName() throws Exception
	{
		String loginName = "nathan4.kopp@ccci.org";

		SiebelUser siebelUser = getSiebelUserByLoginName(loginName);

		assertThat(siebelUser.getLoginName().toLowerCase(), is(loginName.toLowerCase()));
	}

	private SiebelUser getSiebelUserByLoginName(String loginName) throws Exception
	{
		SiebelUser siebelUser = new SiebelUser();

		siebelUser.setLoginName(loginName);

		if (persistence.siebelSelect(siebelUser) != 1)
			throw new Exception("Could not find user with login name " + loginName);

		return siebelUser;
	}

	@Test
	public void showUsers() throws Exception
	{
		List<String> logins = new ArrayList<String>();
		logins.add("nathan.kopp@ccci.org");
		for (int i = 1; i < 7; i++)
			logins.add("nathan" + i + ".kopp@ccci.org");

		showUsers(logins);
	}

	private void showUsers(List<String> logins) throws Exception
	{
		for (String loginName : logins)
		{
			try
			{
				SiebelUser siebelUser = getSiebelUserByLoginName(loginName);
				System.out.println(siebelUser.toString());
			}
			catch (Exception e)
			{
				System.out.println("Could not find user " + loginName + e.getMessage());
			}
		}
	}

	@Test
	public void changeFirstName() throws Exception
	{
		String loginName = "nathan4.kopp@ccci.org";

		SiebelUser siebelUser = getSiebelUserByLoginName(loginName);

		siebelUser.setId("");

		String firstName = UUID.randomUUID().toString().substring(0, 8);

		siebelUser.setFirstName(firstName);

		persistence.siebelUpsert(siebelUser);

		siebelUser = getSiebelUserByLoginName(loginName);

		assertThat(siebelUser.getFirstName().toLowerCase(), is(firstName.toLowerCase()));
	}

	@Test
	public void insertUser() throws Exception
	{
		String templateLogin = "nathan.kopp@ccci.org";
		String newUserLoginName = "nathan2.kopp@ccci.org";

		insertUser(templateLogin, newUserLoginName);

		SiebelUser siebelUser = getSiebelUserByLoginName(newUserLoginName);

		assertThat(siebelUser.getLoginName().toLowerCase(), is(newUserLoginName.toLowerCase()));
	}

	private void insertUser(String templateLogin, String newUserLoginName) throws Exception
	{
		SiebelUser siebelUser = getSiebelUserByLoginName(templateLogin);

		siebelUser.setId(null);

		siebelUser.setLoginName(newUserLoginName);

		persistence.siebelInsert(siebelUser);
	}

	@Test
	public void deleteUser() throws Exception
	{
		String loginName = "nathan2.kopp@ccci.org";

		deleteUser(loginName);
	}

	@Test
	public void deleteUsers() throws Exception
	{
		List<String> logins = Arrays.asList("nathan2.kopp@ccci.org", "nathan3.kopp@ccci.org", "nathan4.kopp@ccci.org", "nathan5.kopp@ccci.org", "nathan6.kopp@ccci.org", "nathan7.kopp@ccci.org",
				"nathan8.kopp@ccci.org");

		deleteUsers(logins);
	}

	private void deleteUsers(List<String> users) throws Exception
	{
		for (String loginName : users)
		{
			try
			{
				deleteUser(loginName);
			}
			catch (Exception e)
			{
				System.out.println("Could not delete user " + loginName + " " + e.getMessage());
			}
		}
	}

	private void deleteUser(String loginName) throws Exception
	{
		SiebelUser siebelUser = getSiebelUserByLoginName(loginName);

		persistence.siebelDelete(siebelUser);
	}

	@Test
	public void changeLoginName() throws Exception
	{
		String loginName = "nathan6.kopp@ccci.org";

		String newLoginName = "nathan7.kopp@ccci.org";

		changeLoginNames(loginName, newLoginName);
	}

	private void changeLoginNames(String loginName, String newLoginName) throws Exception
	{
		List<String> logins = Arrays.asList(loginName, newLoginName);

		showUsers(logins);

		changeLoginName(loginName, newLoginName);

		SiebelUser siebelUser = getSiebelUserByLoginName(newLoginName);

		assertThat(siebelUser.getLoginName().toLowerCase(), is(newLoginName.toLowerCase()));

		showUsers(logins);

		changeLoginName(newLoginName, loginName);

		siebelUser = getSiebelUserByLoginName(loginName);

		assertThat(siebelUser.getLoginName().toLowerCase(), is(loginName.toLowerCase()));

		showUsers(logins);
	}

	private void changeLoginName(String loginName, String newLoginName) throws Exception
	{
		SiebelUser siebelUser = getSiebelUserByLoginName(loginName);

		siebelUser.setLoginName(newLoginName);

		persistence.siebelUpsert(siebelUser);
	}

	private static String read(File file, String remove) throws Exception
	{
		return read(file).replace(remove, "");
	}

	private static String read(File file) throws Exception
	{
		StringBuffer content = new StringBuffer();

		FileInputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(file);
			for (int ch; (ch = inputStream.read()) != -1;)
				content.append((char) ch);
		}
		finally
		{
			if (inputStream != null)
				inputStream.close();
		}

		return content.toString();
	}
}
