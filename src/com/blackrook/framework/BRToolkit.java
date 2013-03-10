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

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.db.DBConnectionPool;
import com.blackrook.db.DatabaseUtils;
import com.blackrook.db.QueryResult;
import com.blackrook.db.mysql.MySQLUtils;
import com.blackrook.db.sqlite.SQLiteUtils;
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
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";
	/** Pool settings XML file. */
	public static final String MAPPING_XML_POOLS = "/WEB-INF/brframework-pools.xml";
	/** View settings XML file. */
	public static final String MAPPING_XML_VIEWS = "/WEB-INF/brframework-views.xml";
	/** Query settings XML file. */
	public static final String MAPPING_XML_QUERIES = "/WEB-INF/brframework-queries.xml";
	/** MySQL type name. */
	public static final String SQL_TYPE_MYSQL = "mysql";
	public static final String SQL_TYPE_SQLITE = "sqlite";
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
		DatabaseUtils dbu = null;
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
		DatabaseUtils dbu = null;
		
		String db = struct.getAttribute(XML_SQL_DB);
		if (db.trim().length() == 0)
			throw new BRFrameworkException("Missing database path for SQL server pool.");

		try {dbu = new SQLiteUtils(getFile(db));}
		catch (Exception e) {throw new BRFrameworkException(e);}
		
		try {pool = new DBConnectionPool(dbu, 1);}
		catch (Exception e) {throw new BRFrameworkException(e);}
		
		return pool;
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
	 * Gets the path to a view by key name.
	 * @return the associated path or null if not found. 
	 */
	public String getViewByName(String key)
	{
		return jspMap.get(key);
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
	 * @return the task that was passed into this method.
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
	
}
