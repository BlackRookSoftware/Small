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
package com.blackrook.framework;

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
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.list.List;
import com.blackrook.db.DBConnectionPool;
import com.blackrook.db.DBUtils;
import com.blackrook.db.QueryResult;
import com.blackrook.db.mysql.MySQLUtils;
import com.blackrook.db.sqlite.SQLiteUtils;
import com.blackrook.framework.controller.BRController;
import com.blackrook.framework.filter.BRFilter;
import com.blackrook.framework.util.BRUtil;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;

/**
 * The main manager class through which all things are
 * pooled and lent out to servlets that request it. 
 * @author Matthew Tropiano
 * TODO: Handle filter system.
 */
public final class BRToolkit
{
	/** Name for Black Rook Toolkit Application attribute. */
	public static final String DEFAULT_APPLICATION_NAME = "__BRTOOLKIT__";
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";
	/** Application settings XML file. */
	public static final String MAPPING_XML = "/WEB-INF/brframework-config.xml";
	
	public static final String XML_VIEW = "viewresolver";
	public static final String XML_VIEW_CLASS = "class";
	public static final String XML_QUERY = "queryresolver";
	public static final String XML_QUERY_CLASS = "class";
	public static final String XML_CONTROLLER = "controller";
	public static final String XML_CONTROLLER_NAME = "path";
	public static final String XML_CONTROLLER_CLASS = "class";
	public static final String XML_FILTER = "filter";
	public static final String XML_FILTER_NAME = "path";
	public static final String XML_FILTER_CLASS = "class";
	public static final String XML_CONTROLLERROOT = "controllerroot";
	public static final String XML_CONTROLLERROOT_PACKAGE = "package";
	public static final String XML_CONTROLLERROOT_EXTENSION = "extension";
	public static final String XML_CONTROLLERROOT_PREFIX = "classprefix";
	public static final String XML_CONTROLLERROOT_SUFFIX = "classsuffix";
	
	public static final String SQL_TYPE_MYSQL = "mysql";
	public static final String SQL_TYPE_SQLITE = "sqlite";
	public static final String XML_SQL = "connectionpool";
	public static final String XML_SQL_NAME = "name";
	public static final String XML_SQL_TYPE = "type";
	public static final String XML_SQL_USER = "user";
	public static final String XML_SQL_PASSWORD = "password";
	public static final String XML_SQL_HOST = "host";
	public static final String XML_SQL_PORT = "port";
	public static final String XML_SQL_DB = "database";
	public static final String XML_SQL_CONNECTIONS = "connections";

	/** The cache map for JSP pages. */
	private HashMap<String, String> viewCache;
	/** List of view resolvers. */
	private List<BRViewResolver> viewResolvers;
	/** The cache for queries. */
	private HashMap<String, String> queryCache;
	/** List of query resolvers. */
	private List<BRQueryResolver> queryResolvers;

	/** The controllers that were instantiated. */
	private HashMap<String, BRController> controllerCache;
	/** List of controller entries. */
	private HashMap<String, String> controllerEntries;
	/** Controller root package. */
	private String controllerRootPackage;
	/** Controller root extension to ignore. */
	private String controllerRootExtension;
	/** Controller root prefix. */
	private String controllerRootPrefix;
	/** Controller root suffix. */
	private String controllerRootSuffix;

	/** The filters that were instantiated. */
	private HashedQueueMap<String, BRFilter> filterCache;
	/** List of filter entries. */
	private HashedQueueMap<String, String> filterEntries;
	
	/** Database connection pool. */
	private HashMap<String, DBConnectionPool> connectionPool;

	/** Servlet context. */
	private ServletContext servletContext;
	
	/** Application context path. */
	private String realAppPath = null;

	/** Singleton toolkit instance. */
	static BRToolkit INSTANCE = null;

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
	 * Returns servlet context that constructed this.
	 */
	public ServletContext getServletContext()
	{
		return servletContext;
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
		connectionPool = new CaseInsensitiveHashMap<DBConnectionPool>();
		controllerCache = new HashMap<String, BRController>();
		controllerEntries = new HashMap<String, String>();
		filterCache = new HashedQueueMap<String, BRFilter>();
		filterEntries = new HashedQueueMap<String, String>();
		
		realAppPath = context.getRealPath(".");
		
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
					else if (struct.isName(XML_CONTROLLER))
						initializeController(struct);
					else if (struct.isName(XML_CONTROLLERROOT))
						initializeControllerRoot(struct);
					else if (struct.isName(XML_FILTER))
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
		String name = struct.getAttribute(XML_SQL_NAME, "default");
		if (name == null || name.trim().length() == 0)
			throw new BRFrameworkException("Missing <name> key for SQL server pool.");
		
		String type = struct.getAttribute(XML_SQL_TYPE);
		if (SQL_TYPE_MYSQL.equalsIgnoreCase(type))
			connectionPool.put(name, initializeMYSQL(struct));
		else if (SQL_TYPE_SQLITE.equalsIgnoreCase(type))
			connectionPool.put(name, initializeSQLite(struct));
		else
			throw new BRFrameworkException("Unsupported SQL Server pool type: "+type);
		}

	/**
	 * Creates a MYSQL pool using the gathered settings. 
	 */
	private DBConnectionPool initializeMYSQL(XMLStruct struct)
	{
		DBConnectionPool pool = null;
		DBUtils dbu = null;
		String user = null;
		String password = null;
				
		user = struct.getAttribute(XML_SQL_USER, "root");
		if (user.trim().length() == 0)
			throw new BRFrameworkException("Missing user for SQL server pool.");
		password = struct.getAttribute(XML_SQL_PASSWORD, "");

		String host = struct.getAttribute(XML_SQL_HOST, "localhost");

		String db = struct.getAttribute(XML_SQL_DB, user);
		if (db.trim().length() == 0)
			throw new BRFrameworkException("Missing URL for SQL server pool.");
		
		int port = struct.getAttributeInt(XML_SQL_PORT, MySQLUtils.DEFAULT_PORT);
		
		try {dbu = new MySQLUtils(db, host, port);}
		catch (Exception e) {throw new BRFrameworkException(e);}

		int conn = struct.getAttributeInt(XML_SQL_CONNECTIONS, 10);

		try {pool = new DBConnectionPool(dbu, conn, user, password);}
		catch (Exception e) {throw new BRFrameworkException(e);}
		
		return pool;
		}
	
	/**
	 * Creates a SQLite "pool" using the gathered settings.
	 * One connection only. 
	 */
	private DBConnectionPool initializeSQLite(XMLStruct struct)
	{
		DBConnectionPool pool = null;
		DBUtils dbu = null;
		
		String db = struct.getAttribute(XML_SQL_DB);
		if (db.trim().length() == 0)
			throw new BRFrameworkException("Missing database path for SQL server pool.");

		try {
			File f = new File(db);
			if (!f.exists())
			{
				Common.createPathForFile(getApplicationFilePath(db));
				f = new File(getApplicationFilePath(db));
				}
			dbu = new SQLiteUtils(f);
		} catch (Exception e) {throw new BRFrameworkException(e);}
		
		try {pool = new DBConnectionPool(dbu, 1);}
		catch (Exception e) {throw new BRFrameworkException(e);}
		
		return pool;
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
	 * Initializes a controller.
	 */
	private void initializeController(XMLStruct struct)
	{
		String name = struct.getAttribute(XML_CONTROLLER_NAME);
		String className = struct.getAttribute(XML_CONTROLLER_CLASS);

		if (name == null)
			throw new BRFrameworkException("Controller in declaration does not have a name.");
		if (className == null)
			throw new BRFrameworkException("Controller \""+name+"\" does not declare a class.");
		
		controllerEntries.put(name, className);
		}

	/**
	 * Initializes the controller root resolver.
	 */
	private void initializeControllerRoot(XMLStruct struct)
	{
		String pkg = struct.getAttribute(XML_CONTROLLERROOT_PACKAGE).trim();
		controllerRootExtension = struct.getAttribute(XML_CONTROLLERROOT_EXTENSION, "view").trim();
		controllerRootPrefix = struct.getAttribute(XML_CONTROLLERROOT_PREFIX, "").trim();
		controllerRootSuffix = struct.getAttribute(XML_CONTROLLERROOT_SUFFIX, "Controller").trim();

		if (pkg == null)
			throw new BRFrameworkException("Controller root declaration must specify a root package.");
		
		controllerRootPackage = pkg;
		}

	/**
	 * Initializes a filter.
	 */
	private void initializeFilter(XMLStruct struct)
	{
		String name = struct.getAttribute(XML_FILTER_NAME);
		String className = struct.getAttribute(XML_FILTER_CLASS);

		if (name == null)
			throw new BRFrameworkException("Filter in declaration does not have a name.");
		if (className == null)
			throw new BRFrameworkException("Filter \""+name+"\" does not declare a class.");
		
		filterEntries.enqueue(name, className);
		}

	/**
	 * Returns a database connection pool by key name.
	 * @param key the name of the connection pool.
	 * @return the connection pool connected to the key or null if none are attached to that name.
	 */
	DBConnectionPool getConnectionPool(String key)
	{
		return connectionPool.get(key);
		}
	
	/**
	 * Returns a controller for a path.
	 * @param path the path of the controller.
	 * @return a controller instance to call or null to trigger a 404.
	 */
	BRController getController(String path)
	{
		BRController out = getControllerUsingDefinitions(path);
		if (out != null)
			return out;
		
		return getControllerUsingRoot(path);
		}
	
	// Get controller using path definitions.
	private BRController getControllerUsingDefinitions(String path)
	{
		if (controllerCache.containsKey(path))
			return controllerCache.get(path);
		
		if (!controllerEntries.containsKey(path))
			return null;
			
		synchronized (controllerCache)
		{
			// in case a thread already completed it.
			if (controllerCache.containsKey(path))
				return controllerCache.get(path);
			
			BRController out = instantiateControllerByEntry(path);
			
			// add to cache and return.
			controllerCache.put(path, out);
			return out;
			}
		}
	
	// Creates a controller by its controller entry.
	private BRController instantiateControllerByEntry(String path)
	{			
		String className = controllerEntries.get(path);
		Class<?> controllerClass = null;
		try {
			controllerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new BRFrameworkException("Class in controller declaration could not be found: "+className);
			}
		
		BRController out = null;
		
		try {
			out = (BRController)BRUtil.getBean(controllerClass);
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in controller declaration is not an instance of BRController: "+className);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in controller declaration could not be instantiated: "+className);
			}
		
		return out;
		}
	
	// Get controller using root definition.
	private BRController getControllerUsingRoot(String path)
	{
		if (controllerCache.containsKey(path))
			return controllerCache.get(path);
		
		synchronized (controllerCache)
		{
			// in case a thread already completed it.
			if (controllerCache.containsKey(path))
				return controllerCache.get(path);
			
			BRController out = instantiateControllerByRoot(path);

			// add to cache and return.
			controllerCache.put(path, out);
			return out;
			}

		}
	
	// Instantiates a controller via rot resolver.
	private BRController instantiateControllerByRoot(String path)
	{
		String className = getClassNameForController(path);
		BRController out = null;
		
		try {
			out = (BRController)BRUtil.getBean(Class.forName(className));
		} catch (ClassCastException e) {
			throw new BRFrameworkException("Class in controller declaration is not an instance of BRController: "+className);
		} catch (Exception e) {
			throw new BRFrameworkException("Class in controller declaration could not be instantiated: "+className);
			}
		
		return out;
		}
	
	// Gets the classname of a path.
	private String getClassNameForController(String path)
	{
		String pkg = controllerRootPackage + ".";
		String cls = "";
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
		
		int extIndex = controllerRootExtension.length() > 0 ? cls.indexOf("."+controllerRootExtension) : -1;
		
		cls = cls.substring(0, 1).toUpperCase() + (extIndex >= 0 ? cls.substring(1, extIndex) : cls.substring(1));
		cls = controllerRootPrefix + cls + controllerRootSuffix;
		
		return pkg + cls;
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

	// Get filters using path definitions.
	private BRFilter[] getFiltersUsingDefinitions(String path)
	{
		return null;
		}

	// Creates a controller by its controller entry.
	// TODO: Rewrite to use a better search-first filter.
	private BRFilter instantiateFilterByEntry(String className)
	{			
		Class<?> filterClass = null;
		try {
			filterClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new BRFrameworkException("Class in controller declaration could not be found: "+className);
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
	 * Attempts to grab an available connection from the pool and performs a query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public QueryResult doQueryPooled(String poolname, String queryKey, Object ... parameters)
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
	 */
	public QueryResult doQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		Connection conn = null;
		QueryResult result = null;		
		try {
			DBConnectionPool pool = connectionPool.get(poolname);
			conn = pool.getAvailableConnection();
			result = DBUtils.doQuery(conn, query, parameters);
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
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public QueryResult doUpdateQueryPooled(String poolname, String queryKey, Object ... parameters)
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
	 */
	public QueryResult doUpdateQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		Connection conn = null;
		QueryResult result = null;
		DBConnectionPool pool = connectionPool.get(poolname);
		if (pool == null)
			throw new BRFrameworkException("Connection pool \""+poolname+"\" does not exist.");
		try {
			conn = pool.getAvailableConnection();
			result = DBUtils.doQueryUpdate(conn, query, parameters);
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
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	public InputStream getResourceAsStream(String path) throws IOException
	{
		File inFile = getApplicationFile(path);
		return inFile != null ? new FileInputStream(inFile) : null;
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
	 * Logs a message out via the servlet context.
	 * @param message the formatted message to log.
	 * @param args the arguments for the formatted message.
	 * @see String#format(String, Object...)
	 */
	public void log(String message, Object ... args)
	{
		servletContext.log(String.format(message + "\n", args));
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
		servletContext.log(String.format(message + "\n", args), throwable);
		}
	
}
