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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.db.DBConnectionPool;
import com.blackrook.db.DatabaseUtils;
import com.blackrook.db.QueryResult;
import com.blackrook.db.mysql.MySQLUtils;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;
import com.blackrook.sync.ThreadPool;

/**
 * The main manager class through which all things are
 * pooled and lent out to servlets that request it. 
 * @author Matthew Tropiano
 */
public final class BRToolkit
{
	/** Name for Black Rook Toolkit Application attribute. */
	public static final String DEFAULT_APPLICATION_NAME = "__BRTOOLKIT__";
	/** Pool settings XML file. */
	public static final String MAPPING_XML_POOLS = "/WEB-INF/brframework-pools.xml";
	/** View settings XML file. */
	public static final String MAPPING_XML_VIEWS = "/WEB-INF/brframework-views.xml";
	/** Query settings XML file. */
	public static final String MAPPING_XML_QUERIES = "/WEB-INF/brframework-queries.xml";
	/** MySQL type name. */
	public static final String SQL_TYPE_MYSQL = "mysql";
	public static final String XML_VIEW = "view";
	public static final String XML_VIEW_KEY = "name";
	public static final String XML_VIEW_LOCATION = "src";
	public static final String XML_QUERY = "query";
	public static final String XML_QUERY_KEY = "name";
	public static final String XML_QUERY_LOCATION = "src";
	public static final String XML_SQL = "connectionpool";
	public static final String XML_SQL_NAME = "name";
	public static final String XML_SQL_TYPE = "type";
	public static final String XML_SQL_USER = "user";
	public static final String XML_SQL_PASSWORD = "password";
	public static final String XML_SQL_HOST = "host";
	public static final String XML_SQL_PORT = "port";
	public static final String XML_SQL_DB = "database";
	public static final String XML_SQL_CONNECTIONS = "connections";
	public static final String XML_THREADS = "threadpool";
	public static final String XML_THREADS_NAME = "name";
	public static final String XML_THREADS_SIZE = "size";

	/** The map for JSP pages. */
	private CaseInsensitiveHashMap<String> jspMap;
	/** The map for queries. */
	private CaseInsensitiveHashMap<String> queryMap;
	/** The cache for queries. */
	private CaseInsensitiveHashMap<String> queryCache;
	/** Database connection pool. */
	private CaseInsensitiveHashMap<DBConnectionPool> connectionPool;
	/** Database connection pool. */
	private CaseInsensitiveHashMap<ThreadPool<BRFrameworkTask>> threadPool;
	/** Application context path. */
	private String realAppPath = null;
	/** MIME Type Map. */
	private BRMIMETypes mimeTypeMap = new BRMIMETypes();

	/** Singleton toolkit instance. */
	private static BRToolkit INSTANCE = null;
	
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
	 * Gets the toolkit from the application context.
	 * @param context the servlet context.
	 */
	static BRToolkit getToolkit(ServletContext context)
	{
		if (INSTANCE != null)
			return INSTANCE;
		return createToolkit(context);
		}
	
	/**
	 * Returns the instance of instance.
	 */
	static BRToolkit getInstance()
	{
		return INSTANCE;
		}
	
	/**
	 * Constructs a new root toolkit used by all servlets and filters.
	 * @param context the servlet context to use.
	 */
	private BRToolkit(ServletContext context)
	{
		jspMap = new CaseInsensitiveHashMap<String>();
		queryMap = new CaseInsensitiveHashMap<String>(25);
		queryCache = new CaseInsensitiveHashMap<String>(25);
		connectionPool = new CaseInsensitiveHashMap<DBConnectionPool>();
		threadPool = new CaseInsensitiveHashMap<ThreadPool<BRFrameworkTask>>();
		realAppPath = context.getRealPath(".");
		
		XMLStruct xml = null;
		InputStream in = null;
		
		try {
			in = getResourceAsStream(MAPPING_XML_POOLS);
			if (in == null)
				throw new BRFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML_POOLS);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.getName().equalsIgnoreCase("pools")) for (XMLStruct struct : root)
				{
					if (struct.getName().equalsIgnoreCase(XML_SQL))
						initializeSQL(struct);
					else if (struct.getName().equalsIgnoreCase(XML_THREADS))
						initializeThreadPool(struct);
					}
				}
		} catch (Exception e) {
			throw new BRFrameworkException(e);
		} finally {
			Common.close(in);
			}

		try {
			in = getResourceAsStream(MAPPING_XML_VIEWS);
			if (in == null)
				throw new BRFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML_VIEWS);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.getName().equalsIgnoreCase("views")) for (XMLStruct struct : root)
				{
					if (struct.getName().equalsIgnoreCase(XML_VIEW))
						initializeView(struct);
					}
				}
		} catch (Exception e) {
			throw new BRFrameworkException(e);
		} finally {
			Common.close(in);
			}

		try {
			in = getResourceAsStream(MAPPING_XML_QUERIES);
			if (in == null)
				throw new BRFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML_QUERIES);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.getName().equalsIgnoreCase("queries")) for (XMLStruct struct : root)
				{
					if (struct.getName().equalsIgnoreCase(XML_QUERY))
						initializeQuery(struct);
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
		DatabaseUtils dbu = null;
		String user = null;
		String password = null;
		
		String name = struct.getAttribute(XML_SQL_NAME, "default");
		if (name == null || name.trim().length() == 0)
			throw new BRFrameworkException("Missing <name> key for SQL server pool.");
		
		String type = struct.getAttribute(XML_SQL_TYPE);
		if (SQL_TYPE_MYSQL.equalsIgnoreCase(type))
		{
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
			}
		else
			throw new BRFrameworkException("Unsupported SQL Server pool type: "+type);
	
	
		int conn = struct.getAttributeInt(XML_SQL_CONNECTIONS, 10);
	
		DBConnectionPool pool = null;
	
		try {pool = new DBConnectionPool(dbu, conn, user, password);}
		catch (Exception e) {throw new BRFrameworkException(e);}
		connectionPool.put(name, pool);
		}

	/**
	 * Initializes a view.
	 */
	private void initializeView(XMLStruct struct)
	{
		String key = struct.getAttribute(XML_VIEW_KEY);
		String location = struct.getAttribute(XML_VIEW_LOCATION);
		if (key.length() == 0 && location.length() == 0)
			throw new BRFrameworkException("Missing name,src in view.");
		else if (key.length() == 0)
			throw new BRFrameworkException("Missing name for location: "+location);
		else if (location.length() == 0)
			throw new BRFrameworkException("Missing src for name: "+key);
		jspMap.put(key, location);
		}

	/**
	 * Initializes a query.
	 */
	private void initializeQuery(XMLStruct struct)
	{
		String key = struct.getAttribute(XML_QUERY_KEY);
		String location = struct.getAttribute(XML_QUERY_LOCATION);
		if (key.length() == 0 && location.length() == 0)
			throw new BRFrameworkException("Missing name,src in view.");
		else if (key.length() == 0)
			throw new BRFrameworkException("Missing name for location: "+location);
		else if (location.length() == 0)
			throw new BRFrameworkException("Missing src for name: "+key);
		queryMap.put(key, location);
		}

	/**
	 * Initializes a thread pool.
	 */
	private void initializeThreadPool(XMLStruct struct)
	{
		String name = struct.getAttribute(XML_THREADS_NAME, "default");
		int size = struct.getAttributeInt(XML_THREADS_SIZE, 10);
		threadPool.put(name, new ThreadPool<BRFrameworkTask>(name, size));
		}

	/**
	 * The actual caching method.
	 */
	private String cacheQuery(String key) throws IOException
	{
		String out = null;
		synchronized (queryCache)
		{
			// threads will immediately give up the lock around here if one
			// thread finishes the caching.
			out = queryCache.get(key);
			if (out == null)
			{
				String src = queryMap.get(key);
				if (src != null)
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(getResourceAsStream(src)));
					StringBuffer sb = new StringBuffer();
					String line = null;
					while ((line = br.readLine()) != null)
					{
						// cull unnecessary lines reasonably.
						String tline = line.trim();
						if (tline.length() == 0) // blank lines
							continue;
						if (tline.startsWith("--")) // line comments
							continue;
						
						sb.append(line).append('\n');
						}
					br.close();
					out = sb.toString();
					queryCache.put(key, out);
					}
				}
			}
		return out;
		}

	/**
	 * Gets/loads the query to be used.
	 */
	private String getQuery(String key) throws IOException
	{
		String out = queryCache.get(key);
		if (out == null)
			out = cacheQuery(key);
		return out;
		}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	public final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}

	/**
	 * Gets the path to a view by key name.
	 * @return the associated path or null if not found. 
	 */
	public String getViewByName(String key)
	{
		return jspMap.get(key);
		}

	/**
	 * Gets the MIME type of a file (uses a database that is more complete than javax.activation).
	 * @param filename the file name to use to figure out a MIME type.
	 * @return the MIME type, or <code>application/octet-stream</code>.
	 */
	public String getMIMEType(String filename)
	{
		return mimeTypeMap.getType(Common.getFileExtension(filename));
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
		String query = null;
		try {
			query = getQuery(queryKey);
			if (query == null)
				throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
		} catch (IOException e) {
			throw new BRFrameworkException(e);
			}
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
		PreparedStatement st = null;
		ResultSet rs = null;
		QueryResult result = null;		
		try {
			DBConnectionPool pool = connectionPool.get(poolname);
			Connection conn = pool.getAvailableConnection();
			st = conn.prepareStatement(query);
			int i = 1;
			for (Object obj : parameters)
				st.setObject(i++, obj);
			rs = st.executeQuery();
			result = new QueryResult(rs);
			rs.close();
			st.close();
			conn.close(); // should release
			return result;
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (rs != null) try {rs.close();} catch (SQLException e) {};
			if (st != null) try {st.close();} catch (SQLException e) {};
			}
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
		String query = null;
		try {
			query = getQuery(queryKey);
			if (query == null)
				throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
		} catch (IOException e) {
			throw new BRFrameworkException(e);
			}
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
		PreparedStatement st = null;
		QueryResult result = null;		
		try {
			DBConnectionPool pool = connectionPool.get(poolname);
			Connection conn = pool.getAvailableConnection();
			st = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			int i = 1;
			for (Object obj : parameters)
				st.setObject(i++, obj);
			int out = st.executeUpdate();
			result = new QueryResult(out, st.getGeneratedKeys());
			st.close();
			conn.close(); // should release
			return result;
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (st != null) try {st.close();} catch (SQLException e) {};
			}
		}

	/**
	 * Attempts to grab an available connection from the pool and performs an update query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKeys the lookup keys of the update statements.
	 * @param parameters list of lists of parameters for the respective parameterized queries.
	 * @return the update results returned (usually number of rows affected).
	 */
	public QueryResult[] doUpdateQueryBatchPooled(String poolname, String[] queryKeys, Object[][] parameters)
	{
		String[] query = new String[queryKeys.length];
		for (int i = 0; i < query.length; i++)
		{
			try {
				query[i] = getQuery(queryKeys[i]);
				if (query[i] == null)
					throw new BRFrameworkException("Query could not be loaded/cached - "+queryKeys[i]);
			} catch (IOException e) {
				throw new BRFrameworkException(e);
				}
			}
		return doUpdateQueryBatchPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs a series
	 * of queries. These queries are batched together as a single transaction - if one
	 * fails, the entire transaction is not committed.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statements to execute.
	 * @param parameters list of lists of parameters for the respective parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public QueryResult[] doUpdateQueryBatchPooledInline(String poolname, String[] query, Object[][] parameters)
	{
		QueryResult[] result = new QueryResult[query.length];
		Exception ex = null;
		try{
			DBConnectionPool pool = connectionPool.get(poolname);
			Connection conn = pool.getAvailableConnection();
			conn.setAutoCommit(false);
			for (int n = 0; ex == null && n < query.length; n++)
			{
				PreparedStatement st = null;
				try {
					st = conn.prepareStatement(query[n], Statement.RETURN_GENERATED_KEYS);
					int i = 1;
					for (Object obj : parameters)
						st.setObject(i++, obj);
					int out = st.executeUpdate();
					result[n] = new QueryResult(out, st.getGeneratedKeys());
				} catch (SQLException e) {
					ex = e;
				} finally {
					if (st != null) try {st.close();} catch (SQLException e) {};
					}
				}
			
			if (ex != null)
			{
				conn.setAutoCommit(true);
				conn.close(); // should release
				throw new BRFrameworkException(ex);
				}
			else
			{
				conn.commit();
				conn.setAutoCommit(true);
				conn.close(); // should release
				}
			
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
		} catch (InterruptedException e) {
			throw new BRFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
			}
		return result;
		}

	/**
	 * Attempts to grab an available thread from a thread pool and starts a task
	 * that can be monitored by the caller.
	 * @param poolname the thread pool to use.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public BRFrameworkTask spawnTaskPooled(String poolname, BRFrameworkTask task)
	{
		threadPool.get(poolname).execute(task);
		return task;
		}

	/**
	 * Attempts to grab an available thread from a thread pool and starts a runnable
	 * encapsulated as a BRFrameworkTask that can be monitored by the caller.
	 * @param poolname the thread pool to use.
	 * @param runnable the runnable to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public BRFrameworkTask spawnRunnablePooled(String poolname, Runnable runnable)
	{
		BRFrameworkTask task = new BRRunnableTask(runnable);
		threadPool.get(poolname).execute(task);
		return task;
		}

	/**
	 * Attempts to grab an available connection from a connection pool and starts a query task
	 * that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param threadPoolName the thread pool to use.
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing query thread, or null if connection acquisition died somehow.
	 */
	public BRQueryTask spawnQueryPooled(String sqlPoolName, String threadPoolName, String queryKey, Object ... parameters)
	{
		String query = null;
		
		try {
			query = getQuery(queryKey);
		} catch (IOException e) {
			throw new BRFrameworkException(e);
			}

		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);

		BRQueryTask queryTask = new BRQueryTask(sqlPoolName, query, true, parameters);
		threadPool.get(threadPoolName).execute(queryTask);
		return queryTask;
		}

	/**
	 * Attempts to grab an available connection from a connection pool 
	 * and starts an update query task that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param threadPoolName the thread pool to use.
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing update query thread, or null if connection acquisition died somehow.
	 */
	public BRQueryTask spawnUpdateQueryPooled(String sqlPoolName, String threadPoolName, String queryKey, Object ... parameters)
	{
		try {
			String query = getQuery(queryKey);
			if (query == null)
				throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);
			BRQueryTask queryTask = new BRQueryTask(sqlPoolName, query, true, parameters);
			threadPool.get(threadPoolName).execute(queryTask);
			return queryTask;
		} catch (IOException e) {
			throw new BRFrameworkException(e);
			}
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
		File inFile = getFile(path);
		return inFile != null ? new FileInputStream(inFile) : null;
		}
	
	/**
	 * Gets a file that is on the application path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public File getFile(String path)
	{
		File inFile = new File(realAppPath+"/"+path);
		return inFile.exists() ? inFile : null;
		}

	/**
	 * Gets the current connection's session id.
	 * Meant to be a convenience method. 
	 * @param request servlet request object.
	 */
	public String getSessionId(HttpServletRequest request)
	{
		return request.getSession().getId();
		}

	/**
	 * Escapes a string so that it can be input safely into a URL string.
	 */
	public String urlEscape(String inString)
	{
		StringBuffer sb = new StringBuffer();
		char[] inChars = inString.toCharArray();
		int i = 0;
		while (i < inChars.length)
		{
			char c = inChars[i];
			if (c > 255)
				sb.append(String.format("%%%02x%%%02x", ((short)c) >>> 8, ((short)c) & 0x0ff)); // big endian
			else if (!((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5a) || (c >= 0x61 && c <= 0x7a)))
				sb.append(String.format("%%%02x", (short)c));
			else
				sb.append(c);
			i++;
			}
		return sb.toString();
		}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the request, or <code>null</code>, if the session is null or the attribute does not exist.
	 */
	public <T> T getRequestBean(HttpServletRequest request, String name, Class<T> clazz)
	{
		T out = null;
		try {
			out = getRequestBean(request, name, clazz, false);
		} catch (Exception e) {
			throwException(e);
			} 
		return out;
		}

	/**
	 * Gets and auto-casts an object bean stored at the request level.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @param create if true, instantiate this class in the request (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the request.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public <T> T getRequestBean(HttpServletRequest request, String name, Class<T> clazz, boolean create)
	{
		Object obj = request.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				request.setAttribute(name, obj);
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the session, or <code>null</code>, if the session is null or the attribute does not exist.
	 */
	public <T> T getSessionBean(HttpServletRequest request, String name, Class<T> clazz)
	{
		T out = null;
		try {
			out = getSessionBean(request, name, clazz, false);
		} catch (Exception e) {
			throwException(e);
			} 
		return out;
		}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the session, a new instance if it doesn't exist, or null if the session is null.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public <T> T getSessionBean(HttpServletRequest request, String name, Class<T> clazz, boolean create)
	{
		HttpSession session = request.getSession();
		if (session == null)
			return null;
		
		Object obj = session.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				session.setAttribute(name, obj);
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true if it exists, false otherwise.
	 */
	public boolean getParameterExist(HttpServletRequest request, String paramName)
	{
		return request.getParameter(paramName) != null;
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false.
	 * This flavor of <code>getParameterBoolean</code> assumes that the parameter
	 * received is a string that is either "true" or not "true".
	 * @see {@link Common#parseBoolean(String)}
	 */
	public boolean getParameterBoolean(HttpServletRequest request, String paramName)
	{
		return Common.parseBoolean(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false, if the string found in the request evaluates
	 * to <code>trueValue</code>. The value of <code>trueValue</code> can be <code>null</code>,
	 * meaning that the parameter was not received.
	 */
	public boolean getParameterBoolean(HttpServletRequest request, String paramName, String trueValue)
	{
		String out = request.getParameter(paramName);
		return out != null && out.equalsIgnoreCase(trueValue);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns the empty string if it doesn't exist.
	 */
	public String getParameterString(HttpServletRequest request, String paramName)
	{
		String out = request.getParameter(paramName);
		return out != null ? out : "";
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a byte and returns 0 if it doesn't exist.
	 */
	public byte getParameterByte(HttpServletRequest request, String paramName)
	{
		return Common.parseByte(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a short and returns 0 if it doesn't exist.
	 */
	public short getParameterShort(HttpServletRequest request, String paramName)
	{
		return Common.parseShort(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a char and returns '\0' if it doesn't exist.
	 */
	public char getParameterChar(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses an integer and returns 0 if it doesn't exist.
	 */
	public int getParameterInt(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a long integer and returns 0L if it doesn't exist.
	 */
	public long getParameterLong(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a float and returns 0.0f if it doesn't exist.
	 */
	public float getParameterFloat(HttpServletRequest request, String paramName)
	{
		return Common.parseFloat(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a double and returns 0.0 if it doesn't exist.
	 */
	public double getParameterDouble(HttpServletRequest request, String paramName)
	{
		return Common.parseDouble(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns <code>def</code> if it doesn't exist.
	 */
	public String getParameterString(HttpServletRequest request, String paramName, String def)
	{
		String out = request.getParameter(paramName);
		return out != null ? out : def;
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a byte and returns <code>def</code> if it doesn't exist.
	 */
	public byte getParameterByte(HttpServletRequest request, String paramName, byte def)
	{
		return Common.parseByte(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a short and returns <code>def</code> if it doesn't exist.
	 */
	public short getParameterShort(HttpServletRequest request, String paramName, short def)
	{
		return Common.parseShort(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a char and returns <code>def</code> if it doesn't exist.
	 */
	public char getParameterChar(HttpServletRequest request, String paramName, char def)
	{
		return Common.parseChar(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses an integer and returns <code>def</code> if it doesn't exist.
	 */
	public int getParameterInt(HttpServletRequest request, String paramName, int def)
	{
		return Common.parseInt(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a long integer and returns <code>def</code> if it doesn't exist.
	 */
	public long getParameterLong(HttpServletRequest request, String paramName, long def)
	{
		return Common.parseLong(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a float and returns <code>def</code> if it doesn't exist.
	 */
	public float getParameterFloat(HttpServletRequest request, String paramName, float def)
	{
		return Common.parseFloat(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a double and returns <code>def</code> if it doesn't exist.
	 */
	public double getParameterDouble(HttpServletRequest request, String paramName, double def)
	{
		return Common.parseDouble(request.getParameter(paramName), def);
		}
	
}
