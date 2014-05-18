/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package com.blackrook.j2ee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.servlet.ServletContext;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.component.QueryResolver;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;
import com.blackrook.sql.SQLConnectionPool;
import com.blackrook.sql.SQLConnector;

/**
 * The main manager class through which all things are
 * pooled and lent out to servlets that request it. 
 * @author Matthew Tropiano
 */
public final class Toolkit
{
	/** Name for Black Rook Toolkit Application attribute. */
	public static final String DEFAULT_APPLICATION_NAME = "__BRTOOLKIT__";
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";
	/** Application settings XML file. */
	public static final String MAPPING_XML = "/WEB-INF/simpleframework-config.xml";
	
	private static final String XML_QUERY = "queryresolver";
	private static final String XML_QUERY_CLASS = "class";
	private static final String XML_CONTROLLERROOT = "controllerroot";
	private static final String XML_CONTROLLERROOT_PACKAGE = "package";
	private static final String XML_CONTROLLERROOT_PREFIX = "prefix";
	private static final String XML_CONTROLLERROOT_SUFFIX = "suffix";
	private static final String XML_CONTROLLERROOT_INDEXCONTROLLERCLASS = "indexclass";
	
	private static final String XML_SQL = "connectionpool";
	private static final String XML_SQL_NAME = "name";
	private static final String XML_SQL_DRIVER = "driver";
	private static final String XML_SQL_URL = "url";
	private static final String XML_SQL_USER = "user";
	private static final String XML_SQL_PASSWORD = "password";
	private static final String XML_SQL_CONNECTIONS = "connections";

	/** Singleton toolkit instance. */
	static Toolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;
	/** Application context path. */
	private String realAppPath;

	/** The cache for queries. */
	private HashMap<String, String> queryCache;
	/** List of query resolvers. */
	private List<QueryResolver> queryResolvers;

	/** Controller root package. */
	private String controllerRootPackage;
	/** Controller root prefix. */
	private String controllerRootPrefix;
	/** Controller root suffix. */
	private String controllerRootSuffix;
	/** Controller root index controller. */
	private String controllerRootIndexClass;

	/** Database connection pool. */
	private HashMap<String, SQLConnectionPool> connectionPool;

	/**
	 * Constructs a new Toolkit.
	 * @param context the servlet context.
	 */
	synchronized static Toolkit create(ServletContext context)
	{
		if (INSTANCE != null)
			return INSTANCE;
		return INSTANCE = new Toolkit(context);
	}

	public String getControllerRootPackage()
	{
		return controllerRootPackage;
	}

	public String getControllerRootPrefix()
	{
		return controllerRootPrefix;
	}

	public String getControllerRootSuffix()
	{
		return controllerRootSuffix;
	}

	public String getControllerRootIndexClass()
	{
		return controllerRootIndexClass;
	}

	/**
	 * Gets a file that is on the application path. 
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	File getApplicationFile(String path)
	{
		File inFile = new File(realAppPath+"/"+path);
		return inFile.exists() ? inFile : null;
	}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	String getApplicationFilePath(String relativepath)
	{
		return realAppPath + "/" + relativepath;
	}

	/**
	 * Returns servlet context that constructed this.
	 */
	ServletContext getServletContext()
	{
		return servletContext;
	}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	@SuppressWarnings("resource")
	InputStream getResourceAsStream(String path) throws IOException
	{
		File inFile = getApplicationFile(path);
		return inFile != null ? new FileInputStream(inFile) : null;
	}

	/**
	 * Gets a query by keyword.
	 * @return the associated query or null if not found. 
	 */
	String getQueryByName(String keyword)
	{
		String out = queryCache.get(keyword);
		if  (out == null)
		{
			for (QueryResolver resolver : queryResolvers)
				if ((out = resolver.resolveQuery(keyword)) != null)
				{
					if (!resolver.dontCacheQuery(keyword))
						queryCache.put(keyword, out);
					break;					
				}
		}
		
		return out;
	}

	/**
	 * Attempts to get a connection pool by a certain name.
	 * @param name the name of the connection pool.
	 * @return a valid connection pool.
	 * @throws SimpleFrameworkException if the pool could not be found.
	 */
	SQLConnectionPool getSQLPool(String name)
	{
		SQLConnectionPool pool = connectionPool.get(name);
		if (pool == null)
			throw new SimpleFrameworkException("Connection pool \""+name+"\" does not exist.");
		return pool;
	}
	
	/**
	 * Returns a list of cached query keyword names.
	 */
	String[] getCachedQueryKeywordNames()
	{
		return getKeys(queryCache);
	}
	
	/**
	 * Returns a list of connection pool names.
	 */
	String[] getConnectionPoolNames()
	{
		return getKeys(connectionPool);
	}
	
	/**
	 * Returns a database connection pool by key name.
	 * @param key the name of the connection pool.
	 * @return the connection pool connected to the key or null if none are attached to that name.
	 */
	SQLConnectionPool getConnectionPool(String key)
	{
		return connectionPool.get(key);
	}

	// gets String keys from a map.
	private String[] getKeys(HashMap<String, ?> map)
	{
		List<String> outList = new List<String>();
		
		Iterator<String> it = map.keyIterator();
		while(it.hasNext())
			outList.add(it.next());
		
		String[] out = new String[outList.size()];
		outList.toArray(out);
		return out;
	}
	
	/**
	 * Constructs a new root toolkit used by all servlets and filters.
	 * @param context the servlet context to use.
	 */
	private Toolkit(ServletContext context)
	{
		servletContext = context;
		queryCache = new CaseInsensitiveHashMap<String>(25);
		queryResolvers = new List<QueryResolver>(25);
		connectionPool = new CaseInsensitiveHashMap<SQLConnectionPool>();
		
		realAppPath = context.getRealPath("/");
		
		XMLStruct xml = null;
		InputStream in = null;
		
		try {
			in = getResourceAsStream(MAPPING_XML);
			if (in == null)
				throw new SimpleFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.isName("config")) for (XMLStruct struct : root)
				{
					if (struct.isName(XML_SQL))
						initializeSQL(struct);
					else if (struct.isName(XML_QUERY))
						initializeQueryResolver(struct);
					else if (struct.isName(XML_CONTROLLERROOT))
						initializeControllerRoot(struct);
/*					else if (struct.isName(XML_FILTERPATH))
						initializeFilter(struct);
*/				}
			}
		} catch (Exception e) {
			throw new SimpleFrameworkException(e);
		} finally {
			Common.close(in);
		}
	}

	/**
	 * Initializes an SQL connection pool.
	 */
	private void initializeSQL(XMLStruct struct)
	{
		SQLConnectionPool pool = null;
		SQLConnector dbu = null;

		String name = struct.getAttribute(XML_SQL_NAME, "default");

		String driver = struct.getAttribute(XML_SQL_DRIVER);
		if (driver.trim().length() == 0)
			throw new SimpleFrameworkException("Missing driver for SQL server pool.");

		String url = struct.getAttribute(XML_SQL_URL);
		if (url.trim().length() == 0)
			throw new SimpleFrameworkException("Missing URL for SQL server pool.");

		String user = struct.getAttribute(XML_SQL_USER);
		if (user != null) 
			user = user.trim();
		
		String password = struct.getAttribute(XML_SQL_PASSWORD, "");

		try {
			dbu = new SQLConnector(driver, url);
		} catch (Exception e) {
			throw new SimpleFrameworkException(e);
		}

		int conn = struct.getAttributeInt(XML_SQL_CONNECTIONS, 10);

		try {
			if (user != null)
				pool = new SQLConnectionPool(dbu, conn, user, password);
			else
				pool = new SQLConnectionPool(dbu, conn);
				
		} catch (Exception e) {
			throw new SimpleFrameworkException(e);
		}
		
		connectionPool.put(name, pool);
	}

	/**
	 * Initializes a query resolver.
	 */
	private void initializeQueryResolver(XMLStruct struct)
	{
		String clazz = struct.getAttribute(XML_QUERY_CLASS).trim();
		if (clazz.length() == 0)
			throw new SimpleFrameworkException("Missing class in query resolver delaration.");
		
		Class<?> clz = null;
		QueryResolver resolver = null;
		
		try {
			clz = Class.forName(clazz);
		} catch (Exception e) {
			throw new SimpleFrameworkException("Class in query resolver could not be found: "+clazz);
		}
		
		try {
			resolver = (QueryResolver)FrameworkUtil.getBean(clz);
		} catch (ClassCastException e) {
			throw new SimpleFrameworkException("Class in query resolver is not an instance of BRQueryResolver: "+clz.getName());
		} catch (Exception e) {
			throw new SimpleFrameworkException("Class in query resolver could not be instantiated: "+clz.getName());
		}

		if (resolver != null)
			queryResolvers.add(resolver);
	}

	/**
	 * Initializes the controller root resolver.
	 */
	private void initializeControllerRoot(XMLStruct struct)
	{
		String pkg = struct.getAttribute(XML_CONTROLLERROOT_PACKAGE).trim();
		controllerRootPrefix = struct.getAttribute(XML_CONTROLLERROOT_PREFIX, "").trim();
		controllerRootSuffix = struct.getAttribute(XML_CONTROLLERROOT_SUFFIX, "Controller").trim();
		controllerRootIndexClass = struct.getAttribute(XML_CONTROLLERROOT_INDEXCONTROLLERCLASS).trim();
		
		if (pkg == null)
			throw new SimpleFrameworkException("Controller root declaration must specify a root package.");
		
		controllerRootPackage = pkg;
	}


}
