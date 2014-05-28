package org.ccci.framework.sblio;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

public class SiebelPersistencePool
{
    private static final int DEFAULT_IDLE_TIMEOUT = 300000;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final int DEFAULT_MAX_IDLE = 8;
    private static final int DEFAULT_MAX_ACTIVE = 20;

	private GenericObjectPool pool = null;

	Logger logger = Logger.getLogger(getClass());

	public SiebelPersistencePool(String url, String username, String password, int maxActive, int maxIdle, int minIdle, int idleTimeout)
	{
		logger.debug("creating siebel persistence pool (simple)");

		pool = new GenericObjectPool(new SiebelPersistenceFactory(url, username, password, null));

		pool.setMaxActive(maxActive);
		pool.setMaxIdle(maxIdle);
		pool.setMinIdle(minIdle);
		pool.setMinEvictableIdleTimeMillis(idleTimeout);

		pool.setTestOnBorrow(true);
		pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
	}

	public SiebelPersistencePool(String url, String username, String password)
	{
		this(url, username, password, DEFAULT_MAX_ACTIVE, DEFAULT_MAX_IDLE, DEFAULT_MIN_IDLE, DEFAULT_IDLE_TIMEOUT);
	}

	public SiebelPersistence getResource() throws Exception
	{
		logger.debug("get resource");

		SiebelPersistence siebelPersistence = (SiebelPersistence)pool.borrowObject();

		logger.debug("got resource");

		return siebelPersistence;
	}

	public void releaseResource(SiebelPersistence siebelPersistence) throws Exception
	{
		logger.debug("release resource");

		siebelPersistence.reset();

		pool.returnObject(siebelPersistence);
	}
}
