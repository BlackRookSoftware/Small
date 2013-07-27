package com.blackrook.framework;

import java.io.File;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.framework.multipart.MultipartParser;
import com.blackrook.framework.multipart.MultipartParserException;
import com.blackrook.framework.multipart.Part;

/**
 * The main dispatcher servlet for the controller portion of the framework.
 * @author Matthew Tropiano
 */
public final class BRDispatcherServlet extends HttpServlet
{
	private static final long serialVersionUID = 4733160851384500294L;

	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRController servlet = getControllerUsingPath(path);
		if (servlet == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
			servlet.onGet(request, response);
		}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRController servlet = getControllerUsingPath(path);
		if (servlet == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else if (MultipartParser.isMultipart(request))
		{
			MultipartParser parser = null;
			try {
				parser = new MultipartParser(request, new File(System.getProperty("java.io.tmpdir")));
			} catch (UnsupportedEncodingException e) {
				sendCode(response, 500, "The encoding type for the POST request is not supported.");
			} catch (MultipartParserException e) {
				sendCode(response, 500, "The server could not parse the multiform request.");
				}
			
			servlet.onMultipartPost(request, response, parser.getPartList());
			
			// clean up files.
			for (Part part : parser.getPartList())
				if (part.isFile())
				{
					File tempFile = part.getFile();
					tempFile.delete();
					}
			}
		else
			servlet.onPost(request, response);
		}
	
	@Override
	public final void doHead(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRController servlet = getControllerUsingPath(path);
		if (servlet == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
			servlet.onHead(request,response);
		}
	
	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRController servlet = getControllerUsingPath(path);
		if (servlet == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
			servlet.onPut(request,response);
		}
	
	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRController servlet = getControllerUsingPath(path);
		if (servlet == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
			servlet.onDelete(request,response);
		}

	/**
	 * Get the base path parsed out of the request URI.
	 */
	private final String getPath(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int contextPathLen = request.getContextPath().length();
		int queryIndex = requestURI.indexOf('?');
		if (queryIndex >= 0)
			return requestURI.substring(contextPathLen, queryIndex);
		else
			return requestURI.substring(contextPathLen); 
		}
	
	/**
	 * Gets the controller to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a controller, or null if no controller by that name. 
	 * This servlet sends a 404 back if this happens.
	 * @throws BRFrameworkException if a huge error occurred.
	 */
	private final BRController getControllerUsingPath(String uriPath)
	{
		return BRToolkit.INSTANCE.getController(uriPath);
		}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	private final void sendCode(HttpServletResponse response, int statusCode, String message)
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
	private final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}

}
