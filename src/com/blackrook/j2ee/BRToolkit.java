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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import javax.servlet.ServletContext;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.BRTransaction.Level;
import com.blackrook.j2ee.util.BRUtil;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;
import com.blackrook.sql.SQLConnectionPool;
import com.blackrook.sql.SQLConnector;
import com.blackrook.sql.SQLUtil;
import com.blackrook.sql.SQLResult;

/**
 * The main manager class through which all things are
 * pooled and lent out to servlets that request it. 
 * @author Matthew Tropiano
 */
public final class BRToolkit
{
	/** Name for Black Rook Toolkit Application attribute. */
	public static final String DEFAULT_APPLICATION_NAME = "__BRTOOLKIT__";
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";
	/** Application settings XML file. */
	public static final String MAPPING_XML = "/WEB-INF/brframework-config.xml";
	
	private static final String XML_VIEW = "viewresolver";
	private static final String XML_VIEW_CLASS = "class";
	private static final String XML_QUERY = "queryresolver";
	private static final String XML_QUERY_CLASS = "class";
	private static final String XML_FILTERPATH = "filterpath";
	private static final String XML_FILTERPATH_PACKAGE = "package";
	private static final String XML_FILTERPATH_CLASSES = "classes";
	private static final String XML_CONTROLLERROOT = "controllerroot";
	private static final String XML_CONTROLLERROOT_PACKAGE = "package";
	private static final String XML_CONTROLLERROOT_PREFIX = "prefix";
	private static final String XML_CONTROLLERROOT_SUFFIX = "suffix";
	private static final String XML_CONTROLLERROOT_METHODPREFIX = "methodprefix";
	private static final String XML_CONTROLLERROOT_INDEXCONTROLLERCLASS = "indexclass";
	
	private static final String XML_SQL = "connectionpool";
	private static final String XML_SQL_NAME = "name";
	private static final String XML_SQL_DRIVER = "driver";
	private static final String XML_SQL_URL = "url";
	private static final String XML_SQL_USER = "user";
	private static final String XML_SQL_PASSWORD = "password";
	private static final String XML_SQL_CONNECTIONS = "connections";

	/** Singleton toolkit instance. */
	static BRToolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;
	/** Application context path. */
	private String realAppPath;

	/** The cache map for JSP pages. */
	private HashMap<String, String> viewCache;
	/** List of view resolvers. */
	private List<BRViewResolver> viewResolvers;
	/** The cache for queries. */
	private HashMap<String, String> queryCache;
	/** List of query resolvers. */
	private List<BRQueryResolver> queryResolvers;

	/** Controller root package. */
	private String controllerRootPackage;
	/** Controller root prefix. */
	private String controllerRootPrefix;
	/** Controller root suffix. */
	private String controllerRootSuffix;
	/** Controller root method prefix. */
	private String controllerRootMethodPrefix;
	/** Controller root index controller. */
	private String controllerRootIndexClass;
	/** Map of package to filter classes. */
	private HashMap<String, String[]> filterEntries;

	/** The controllers that were instantiated. */
	private HashMap<String, BRControllerEntry> controllerCache;
	/** The filters that were instantiated. */
	private HashMap<String, BRFilter> filterCache;

	/** Database connection pool. */
	private HashMap<String, SQLConnectionPool> connectionPool;

	/**
	 * Constructs a new Toolkit.
	 * @param context the servlet context.
	 */
	synchronized static BRToolkit createToolkit(ServletContext context)
	{
		if (INSTANCE != null)
			return INSTANCE;
		return INSTANCE = new BRToolkit(context);
		}

	/**
	 * Gets a file that is on the application path. 
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public File getApplicationFile(String path)
	{
		File inFile = new File(realAppPath+"/"+path);
		return inFile.exists() ? inFile : null;
		}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public String getApplicationFilePath(String relativepath)
	{
		return realAppPath + "/" + relativepath;
		}

	/**
	 * Returns servlet context that constructed this.
	 */
	public ServletContext getServletContext()
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
	public InputStream getResourceAsStream(String path) throws IOException
	{
		File inFile = getApplicationFile(path);
		return inFile != null ? new FileInputStream(inFile) : null;
		}

	/**
	 * Gets the path to a view by keyword.
	 * @return the associated path or null if not found. 
	 */
	public String getViewByName(String keyword)
	{
		String out = viewCache.get(keyword);
		if  (out == null)
		{
			for (BRViewResolver resolver : viewResolvers)
				if ((out = resolver.resolveView(keyword)) != null)
				{
					if (!resolver.dontCacheView(keyword))
						viewCache.put(keyword, out);
					break;					
					}
			}
		
		return out;
		}

	/**
	 * Gets a query by keyword.
	 * @return the associated query or null if not found. 
	 */
	public String getQueryByName(String keyword)
	{
		String out = queryCache.get(keyword);
		if  (out == null)
		{
			for (BRQueryResolver resolver : queryResolvers)
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
	 * Logs a message out via the servlet context.
	 * @param message the formatted message to log.
	 * @param args the arguments for the formatted message.
	 * @see String#format(String, Object...)
	 */
	public void log(String message, Object ... args)
	{
		servletContext.log(String.format(message, args));
		}

	/**
	 * Logs a message out via the servlet context.
	 * @param throwable the throwable to 
	 * @param message the formatted message to log.
	 * @param args the arguments for the formatted message.
	 * @see String#format(String, Object...)
	 */
	public void log(Throwable throwable, String message, Object ... args)
	{
		servletContext.log(String.format(message, args), throwable);
		}

	/**
	 * Generates a transaction for multiple queries in one set.
	 * This transaction performs all of its queries through one connection.
	 * The connection is held by this transaction until it is finished via {@link SQLTransaction#finish()}.
	 * @param poolname the name of the connection pool to use.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link SQLTransaction} object to handle a contiguous transaction.
	 * @throws BRFrameworkException if the transaction could not be created.
	 */
	BRTransaction startTransaction(String poolname, Level transactionLevel)
	{
		SQLConnectionPool pool = connectionPool.get(poolname);
		if (pool == null)
			throw new BRFrameworkException("Connection pool \""+poolname+"\" does not exist.");
		
		Connection connection = null;
		try {
			connection = pool.getAvailableConnection();
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
			}
		
		return new BRTransaction(this, connection, transactionLevel);
		}
	
	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute (by key).
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	SQLResult doQueryPooled(String poolname, String queryKey, Object ... parameters)
	{
		String query = getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
		return doQueryPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * Assumes that the query passed is an actual query and not a lookup key.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query causes an error.
	 */
	SQLResult doQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		Connection conn = null;
		SQLResult result = null;		
		try {
			SQLConnectionPool pool = connectionPool.get(poolname);
			if (pool == null)
				throw new BRFrameworkException("Connection pool \""+poolname+"\" does not exist.");
			conn = pool.getAvailableConnection();
			result = SQLUtil.doQuery(conn, query, parameters);
			conn.close(); // should release
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
			}
		return result;
		}

	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * The result is returned as a series of objects with the data filled in.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute (by key).
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	<T> T[] doQueryPooled(String poolname, Class<T> type, String queryKey, Object ... parameters)
	{
		String query = getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
		return doQueryPooledInline(poolname, type, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * The result is returned as a series of objects with the data filled in.
	 * Assumes that the query passed is an actual query and not a lookup key.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query causes an error.
	 */
	<T> T[] doQueryPooledInline(String poolname, Class<T> type, String query, Object ... parameters)
	{
		Connection conn = null;
		T[] result = null;		
		try {
			SQLConnectionPool pool = connectionPool.get(poolname);
			if (pool == null)
				throw new BRFrameworkException("Connection pool \""+poolname+"\" does not exist.");
			conn = pool.getAvailableConnection();
			result = SQLUtil.doQuery(type, conn, query, parameters);
			conn.close(); // should release
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
			}
		return result;
		}

	/**
	 * Attempts to grab an available connection from the pool and performs an update query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute (by key).
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	SQLResult doUpdateQueryPooled(String poolname, String queryKey, Object ... parameters)
	{
		String query = getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
		return doUpdateQueryPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query causes an error.
	 */
	SQLResult doUpdateQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		Connection conn = null;
		SQLResult result = null;
		SQLConnectionPool pool = connectionPool.get(poolname);
		if (pool == null)
			throw new BRFrameworkException("Connection pool \""+poolname+"\" does not exist.");
		try {
			conn = pool.getAvailableConnection();
			result = SQLUtil.doQueryUpdate(conn, query, parameters);
			conn.close(); // should release
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
			}
		return result;
		}
	
	/**
	 * Returns a list of view names.
	 */
	String[] getCachedViewNames()
	{
		return getKeys(viewCache);
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

	/**
	 * Returns a controller for a path.
	 * @param path the path of the controller.
	 * @return a controller instance to call or null to trigger a 404.
	 */
	BRControllerEntry getController(String path)
	{
		if (controllerCache.containsKey(path))
			return controllerCache.get(path);
		
		synchronized (controllerCache)
		{
			// in case a thread already completed it.
			if (controllerCache.containsKey(path))
				return controllerCache.get(path);
			
			BRControllerEntry out = instantiateController(path);
	
			if (out == null)
				return null;
			
			// add to cache and return.
			controllerCache.put(path, out);
			return out;
			}
		}

	/**
	 * Returns a filter for a path.
	 * @param path the path of the filter.
	 * @return a filter instance to call or null for no filter.
	 */
	BRFilter[] getFilters(String path)
	{
		BRFilter[] out = getFiltersUsingDefinitions(path);
		if (out != null)
			return out;
		
		return null;
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
	private BRToolkit(ServletContext context)
	{
		servletContext = context;
		viewCache = new CaseInsensitiveHashMap<String>(25);
		viewResolvers = new List<BRViewResolver>(25);
		queryCache = new CaseInsensitiveHashMap<String>(25);
		queryResolvers = new List<BRQueryResolver>(25);
		connectionPool = new CaseInsensitiveHashMap<SQLConnectionPool>();
		controllerCache = new HashMap<String, BRControllerEntry>();
		filterEntries = new HashMap<String, String[]>();
		filterCache = new HashMap<String, BRFilter>();
		
		realAppPath = context.getRealPath("/");
		
		XMLStruct xml = null;
		InputStream in = null;
		
		try {
			in = getResourceAsStream(MAPPING_XML);
			if (in == null)
				throw new BRFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.isName("config")) for (XMLStruct struct : root)
				{
					if (struct.isName(XML_SQL))
						initializeSQL(struct);
					else if (struct.isName(XML_VIEW))
						initializeViewResolver(struct);
					else if (struct.isName(XML_QUERY))
						initializeQueryResolver(struct);
					else if (struct.isName(XML_CONTROLLERROOT))
						initializeControllerRoot(struct);
					else if (struct.isName(XML_FILTERPATH))
						initializeFilter(struct);
					}
				}
		} catch (Exception e) {
			throw new BRFrameworkException(e);
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
			throw new BRFrameworkException("Missing driver for SQL server pool.");

		String url = struct.getAttribute(XML_SQL_URL);
		if (url.trim().length() == 0)
			throw new BRFrameworkException("Missing URL for SQL server pool.");

		String user = struct.getAttribute(XML_SQL_USER);
		if (user != null) 
			user = user.trim();
		
		String password = struct.getAttribute(XML_SQL_PASSWORD, "");

		try {
			dbu = new SQLConnector(driver, url);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}

		int conn = struct.getAttributeInt(XML_SQL_CONNECTIONS, 10);

		try {
			if (user != null)
				pool = new SQLConnectionPool(dbu, conn, user, password);
			else
				pool = new SQLConnectionPool(dbu, conn);
				
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		
		connectionPool.put(name, pool);
		}

	/**
	 * Initializes a view resolver.
	 */
	private void initializeViewResolver(XMLStruct struct)
	{
		String clazz = struct.getAttribute(XML_VIEW_CLASS).trim();
		if (clazz.length() == 0)
			throw new BRFrameworkException("Missing class in view resolver delaration.");

		Class<?> clz = null;
		BRViewResolver resolver = null;
		
		try {
			clz = Class.forName(clazz);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in view resolver could not be found: "+clazz);
			}
		
		try {
			resolver = (BRViewResolver)BRUtil.getBean(clz);
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in view resolver is not an instance of BRViewResolver: "+clz.getName());
		} catch (Exception e) {
			throw new BRFrameworkException("Class in view resolver could not be instantiated: "+clz.getName());
			}

		if (resolver != null)
			viewResolvers.add(resolver);
		}

	/**
	 * Initializes a query resolver.
	 */
	private void initializeQueryResolver(XMLStruct struct)
	{
		String clazz = struct.getAttribute(XML_QUERY_CLASS).trim();
		if (clazz.length() == 0)
			throw new BRFrameworkException("Missing class in query resolver delaration.");
		
		Class<?> clz = null;
		BRQueryResolver resolver = null;
		
		try {
			clz = Class.forName(clazz);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in query resolver could not be found: "+clazz);
			}
		
		try {
			resolver = (BRQueryResolver)BRUtil.getBean(clz);
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in query resolver is not an instance of BRQueryResolver: "+clz.getName());
		} catch (Exception e) {
			throw new BRFrameworkException("Class in query resolver could not be instantiated: "+clz.getName());
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
		controllerRootMethodPrefix = struct.getAttribute(XML_CONTROLLERROOT_METHODPREFIX, "call").trim();
		controllerRootIndexClass = struct.getAttribute(XML_CONTROLLERROOT_INDEXCONTROLLERCLASS).trim();
		
		if (pkg == null)
			throw new BRFrameworkException("Controller root declaration must specify a root package.");
		
		controllerRootPackage = pkg;
		}

	/**
	 * Initializes a filter.
	 */
	private void initializeFilter(XMLStruct struct)
	{
		String pkg = struct.getAttribute(XML_FILTERPATH_PACKAGE);
		String classString = struct.getAttribute(XML_FILTERPATH_CLASSES);

		if (pkg == null)
			throw new BRFrameworkException("Filter in declaration does not declare a package.");
		if (classString == null)
			throw new BRFrameworkException("Filter for package \""+pkg+"\" does not declare a class.");
		
		String[] classes = classString.split("(\\s|\\,)+");
		
		filterEntries.put(pkg, classes);
		}

	// Instantiates a controller via root resolver.
	private BRControllerEntry instantiateController(String path)
	{
		String className = getClassNameForController(path);
		
		if (className == null)
			return null;
		
		Class<?> controllerClass = null;
		try {
			controllerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
			//throw new BRFrameworkException("Class in controller declaration could not be found: "+className);
			}
		
		BRControllerEntry out = null;
		
		try {
			out = new BRControllerEntry(controllerClass, controllerRootMethodPrefix);
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in controller declaration is not an instance of BRController: "+className);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in controller declaration could not be instantiated: "+className);
			}
		
		int lastIndex = 0;
		String[] filterClasses = null;
		do {
			lastIndex = className.lastIndexOf(".");
			if (lastIndex >= 0)
			{
				className = className.substring(0, lastIndex);
				filterClasses = filterEntries.get(className);
				}
		} while (lastIndex >= 0 && filterClasses == null);
		
		if (filterClasses != null) for (String fc : filterClasses)
		{
			BRFilter filter = null;
			if ((filter = filterCache.get(fc)) == null)
				filter = instantiateFilter(fc);
			out.addFilter(filter);
			}
		
		return out;
		}
	
	// Creates a filter by its entry.
	private BRFilter instantiateFilter(String className)
	{			
		Class<?> filterClass = null;
		try {
			filterClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new BRFrameworkException("Class in filter declaration could not be found: "+className);
			}
		
		BRFilter out = null;
		
		try {
			out = (BRFilter)BRUtil.getBean(filterClass);
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in filter declaration is not an instance of BRFilter: "+className);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in filter declaration could not be instantiated: "+className);
			}
		
		return out;
		}

	// Gets the classname of a path.
	private String getClassNameForController(String path)
	{
		String pkg = controllerRootPackage + ".";
		String cls = "";
		
		if (Common.isEmpty(path))
		{
			if (controllerRootIndexClass == null)
				return null;
			cls = controllerRootIndexClass;
			return cls;
			}
		else
		{
			String[] dirs = path.substring(1).split("[/]+");
			if (dirs.length > 1)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < dirs.length - 1; i++)
				{
					sb.append(dirs[i]);
					sb.append('.');
					}
				pkg += sb.toString();

				}

			cls = dirs[dirs.length - 1];
			cls = pkg + controllerRootPrefix + Character.toUpperCase(cls.charAt(0)) + cls.substring(1) + controllerRootSuffix;
			
			// if class is index folder without using root URL, do not permit use.
			if (cls.equals(controllerRootIndexClass))
				return null;

			return cls;
			}
		}
	
	// Get filters using path definitions.
	private BRFilter[] getFiltersUsingDefinitions(String path)
	{
		return null;
		}
	
}
