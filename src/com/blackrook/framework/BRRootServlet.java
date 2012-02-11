package com.blackrook.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.framework.tasks.BRQueryTask;

/**
 * Root control servlet for the Black Rook J2EE Framework.
 * This does all of the pool management and other things that should remain
 * transparent to users of the framework. 
 * @author Matthew Tropiano
 */
public abstract class BRRootServlet extends HttpServlet
{
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";

	private static final long serialVersionUID = 7057939105164581326L;

	private static final Random LAG_SIM_RANDOM = new Random();

	/**
	 * Gets the current connection's session id.
	 * Meant to be a convenience method. 
	 * @param request servlet request object.
	 */
	public static String getSessionId(HttpServletRequest request)
	{
		return request.getSession().getId();
		}

	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		BRRootManager.initializeRoot(getServletContext());
		directService(request,response,false);
		}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		BRRootManager.initializeRoot(getServletContext());
		directService(request,response,true);
		}

	/**
	 * Function that redirects a service call to the appropriate handler methods.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param post did this come from a POST request? if not, this is false ("GET", probably).
	 */
	@SuppressWarnings("unchecked")
	public final void directService(HttpServletRequest request, HttpServletResponse response, boolean post)
	{
		if (post)
		{
			if (ServletFileUpload.isMultipartContent(request))
			{
				FileItemFactory factory = new DiskFileItemFactory();
				ServletFileUpload upload = new ServletFileUpload(factory);
				try {
					List<FileItem> list = upload.parseRequest(request);
					ArrayList<FileItem> fitems = new ArrayList<FileItem>();
					HashMap<String,String> paramTable = new HashMap<String,String>(4);
					for (FileItem fit : list)
					{
						if (!fit.isFormField())
							fitems.add(fit);
						else
							paramTable.put(fit.getFieldName(),fit.getString());
						}
					list = null;
					FileItem[] fileItems = new FileItem[fitems.size()];
					fitems.toArray(fileItems);
					fitems = null;
					doMultiformPost(request, response, fileItems, paramTable);
				} catch (FileUploadException e) {
					e.printStackTrace(System.err);
					doServicePost(request,response);
					}
				}
			else
				doServicePost(request,response);
			}
		else
			doServiceGet(request,response);
		}
	
	/**
	 * The entry point for all Black Rook Framework Servlets on a GET request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void doServiceGet(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void doServicePost(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a multiform request.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param fileItems	the list of file items parsed in the multiform packet.
	 * @param paramMap the table of the parameters passed found in the multiform packet (THEY WILL NOT BE IN THE REQUEST).
	 */
	public abstract void doMultiformPost(HttpServletRequest request, HttpServletResponse response, FileItem[] fileItems, HashMap<String,String> paramMap);

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	protected final void sendError(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	protected final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}

	/**
	 * Forwards the client abruptly to another document or servlet (new client request). 
	 * @param response	servlet response object.
	 * @param url		target URL.
	 */
	protected final void sendRedirect(HttpServletResponse response, String url)
	{
		try{
			response.sendRedirect(url);
		} catch (Exception e) {
			throwException(e);
			}
		}
	
	/**
	 * Gets the MIME type of a file (uses a database that is more complete than javax.activation).
	 * @param filename the file name to use to figure out a MIME type.
	 * @return the MIME type, or <code>application/octet-stream</code>.
	 */
	public final String getMIMEType(String filename)
	{
		return BRRootManager.getMIMEType(filename);
		}
	
	/**
	 * Simulates latency on a response, for testing.
	 * Just calls {@link Common.sleep(long)} and varies the input value.
	 */
	public void simulateLag(int millis)
	{
		Common.sleep(LAG_SIM_RANDOM.nextInt(millis));
		}
	
	/**
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key taken from the mapping XMLs.
	 */
	public final void includeView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = BRRootManager.getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". It may not be declared in "+BRRootManager.MAPPING_XML_VIEWS);
		includeViewInline(request, response, path);
		}
	
	/**
	 * Includes the output of a view in the response, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public final void includeViewInline(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).include(request, response);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Surreptitiously forwards the request to a view.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key taken from the mapping XMLs.
	 */
	public final void sendToView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = BRRootManager.getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". It may not be declared in "+BRRootManager.MAPPING_XML_VIEWS);
		sendToViewInline(request, response, path);
		}
	
	/**
	 * Surreptitiously forwards the request to a view, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public final void sendToViewInline(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception e) {
			throwException(e);
			}
		}
	
	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	public final InputStream getResourceAsStream(String path) throws IOException
	{
		return BRRootManager.getResourceAsStream(path);
		}
	
	/**
	 * Gets a file for a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the file.
	 * @return a file representing the specified resource or null if it couldn't be opened.
	 */
	public final File getFile(String path)
	{
		return BRRootManager.getFile(path);
		}
	
	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute, by keyword.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public final BRQueryResult doQueryPooled(String poolname, String queryKey, Object ... parameters)
	{
		return BRRootManager.doQueryPooled(poolname, queryKey, parameters);
		}
	
	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * Assumes that the query passed is an actual query and not a lookup key.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public final BRQueryResult doQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		return BRRootManager.doQueryPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs an update query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKey the query statement to execute, by keyword.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public final BRQueryResult doUpdateQueryPooled(String poolname, String queryKey, Object ... parameters)
	{
		return BRRootManager.doUpdateQueryPooled(poolname, queryKey, parameters);
		}
	
	/**
	 * Attempts to grab an available connection from the pool and performs a query.
	 * @param poolname the SQL connection pool name to use.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public final BRQueryResult doUpdateQueryPooledInline(String poolname, String query, Object ... parameters)
	{
		return BRRootManager.doUpdateQueryPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the pool and performs an update query.
	 * @param poolname the SQL connection pool name to use.
	 * @param queryKeys the lookup keys of the update statements.
	 * @param parameters list of lists of parameters for the respective parameterized queries.
	 * @return the update results returned (usually number of rows affected).
	 */
	public final BRQueryResult[] doUpdateQueryBatchPooled(String poolname, String[] queryKeys, Object[][] parameters)
	{
		return BRRootManager.doUpdateQueryBatchPooled(poolname, queryKeys, parameters);
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
	public final BRQueryResult[] doUpdateQueryBatchPooledInline(String poolname, String[] query, Object[][] parameters)
	{
		return BRRootManager.doUpdateQueryBatchPooledInline(poolname, query, parameters);
		}

	/**
	 * Attempts to grab an available thread from a thread pool and starts a task
	 * that can be monitored by the caller.
	 * @param poolname the thread pool to use.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnTaskPooled(String poolname, BRFrameworkTask task)
	{
		return BRRootManager.spawnTaskPooled(poolname, task);
		}

	/**
	 * Attempts to grab an available thread from a thread pool and starts a runnable
	 * encapsulated as a BRFrameworkTask that can be monitored by the caller.
	 * @param poolname the thread pool to use.
	 * @param runnable the runnable to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnRunnablePooled(String poolname, Runnable runnable)
	{
		return BRRootManager.spawnRunnablePooled(poolname, runnable);
		}

	/**
	 * Attempts to grab an available connection from a connection pool and starts a query task
	 * that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param threadPoolName the thread pool to use.
	 * @param queryKey the query statement to execute, by keyword.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnQueryPooled(String sqlPoolName, String threadPoolName, String queryKey, Object ... parameters)
	{
		return BRRootManager.spawnQueryPooled(sqlPoolName, threadPoolName, queryKey, parameters);
		}

	/**
	 * Attempts to grab an available connection from a connection pool 
	 * and starts an update query task that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param threadPoolName the thread pool to use.
	 * @param queryKey the query statement to execute, by keyword.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing update query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnUpdateQueryPooled(String sqlPoolName, String threadPoolName, String queryKey, Object ... parameters)
	{
		return BRRootManager.spawnUpdateQueryPooled(sqlPoolName, threadPoolName, queryKey, parameters);
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
