package com.blackrook.j2ee.small.descriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.j2ee.small.SmallUtil;
import com.blackrook.j2ee.small.annotation.Attachment;
import com.blackrook.j2ee.small.annotation.Content;
import com.blackrook.j2ee.small.annotation.ControllerEntry;
import com.blackrook.j2ee.small.annotation.FilterChain;
import com.blackrook.j2ee.small.annotation.NoCache;
import com.blackrook.j2ee.small.annotation.View;
import com.blackrook.j2ee.small.descriptor.ControllerProfile.Output;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.util.RequestUtil;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLWriter;

/**
 * Method descriptor class, specifically for controllers.
 * @author Matthew Tropiano
 */
public class ControllerEntryPoint extends EntryPoint<ControllerProfile>
{
	private static final String PREFIX_REDIRECT = "redirect:";
	private static final Class<?>[] NO_FILTERS = new Class<?>[0];
	private static final RequestMethod[] REQUEST_METHODS_GET = new RequestMethod[]{RequestMethod.GET};

	/** Entry path. */
	private RequestMethod[] requestMethods;
	/** Full entry path. */
	private String path;
	/** Output content. */
	private Output outputType;
	/** Forced MIME type. */
	private String mimeType;
	/** No cache? */
	private boolean noCache;
	/** Filter class list. */
	private Class<?>[] filterChain;

	/**
	 * Creates an entry method around a service profile instance.
	 * @param controllerProfile the service instance.
	 * @param method the method invoked.
	 */
	public ControllerEntryPoint(ControllerProfile controllerProfile, Method method)
	{
		super(controllerProfile, method);
		
		this.outputType = null;
		this.noCache = method.isAnnotationPresent(NoCache.class);
		this.filterChain = NO_FILTERS;

		ControllerEntry controllerEntry = method.getAnnotation(ControllerEntry.class);
		
		if (Common.isEmpty(controllerEntry.method()))
			this.requestMethods = REQUEST_METHODS_GET;
		else
			this.requestMethods = controllerEntry.method();
		
		this.path = controllerProfile.getPath() + '/' + SmallUtil.trimSlashes(controllerEntry.value());

		if (method.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = method.getAnnotation(FilterChain.class);
			if (Common.isEmpty(fc.value()))
				this.filterChain = Common.joinArrays(controllerProfile.getFilterChain(), fc.value());
			else
				this.filterChain = controllerProfile.getFilterChain();
		}
		else
		{
			this.filterChain = controllerProfile.getFilterChain();
		}
		
		if (method.isAnnotationPresent(ControllerEntry.class))
		{
			Class<?> type = getType();
			
			if (method.isAnnotationPresent(Content.class))
			{
				Content c = method.getAnnotation(Content.class);
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @Content cannot return void.");
				this.outputType = Output.CONTENT;
				this.mimeType = Common.isEmpty(c.value()) ? null : c.value();
			}
			else if (method.isAnnotationPresent(Attachment.class))
			{
				Attachment a = method.getAnnotation(Attachment.class);
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @Attachment cannot return void.");
				this.outputType = Output.ATTACHMENT;
				this.mimeType = Common.isEmpty(a.value()) ? null : a.value();
			}
			else if (method.isAnnotationPresent(View.class))
			{
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @View cannot return void.");
				this.outputType = Output.VIEW;
			}
			else if (type != Void.class && type != Void.TYPE)
				throw new SmallFrameworkSetupException("Entry methods that don't return void must be annotated with @Content, @Attachment, or @View.");
		}
		
	}

	/**
	 * Gets the request methods on this entry point.
	 */
	public RequestMethod[] getRequestMethods() 
	{
		return requestMethods;
	}
	
	/**
	 * Gets the full path of the entry method.
	 * @return
	 */
	public String getPath() 
	{
		return path;
	}
	
	/**
	 * Returns the forced MIME type to use.
	 * If null, the dispatcher decides it.
	 */
	public String getMimeType()
	{
		return mimeType;
	}

	/**
	 * The method's output type, if controller call.
	 */
	public Output getOutputType()
	{
		return outputType;
	}

	/**
	 * If true, a directive is sent to the client to not cache the response data.  
	 */
	public boolean isNoCache()
	{
		return noCache;
	}

	/**
	 * Gets this method's full filter chain (package to controller to this method).
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
	}

	/**
	 * Completes a full controller request call.
	 */
	public void handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		String pathRemainder,
		HashMap<String, String> pathVariableMap, 
		HashMap<String, Cookie> cookieMap, 
		HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invoke(requestMethod, request, response, pathRemainder, pathVariableMap, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Controller method.", e);
		}
		
		if (noCache)
		{
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		
		if (outputType != null)
		{
			String fname = null;
			switch (outputType)
			{
				case VIEW:
				{
					String viewKey = String.valueOf(retval);
					if (viewKey.startsWith(PREFIX_REDIRECT))
						SmallUtil.sendRedirect(response, viewKey.substring(PREFIX_REDIRECT.length()));
					else
						SmallUtil.sendToView(request, response, getServiceProfile().getViewResolver().resolveView(viewKey));
					break;
				}
				case ATTACHMENT:
					fname = RequestUtil.getPage(request);
					// fall through.
				case CONTENT:
				{
					Class<?> returnType = getType();
					
					// File output.
					if (File.class.isAssignableFrom(returnType))
					{
						File outfile = (File)retval;
						if (outfile == null || !outfile.exists())
							SmallUtil.sendCode(response, 404, "File not found.");
						else if (Common.isEmpty(mimeType))
							SmallUtil.sendFileContents(response, mimeType, outfile);
						else
							SmallUtil.sendFileContents(response, outfile);
					}
					// XML output.
					else if (XMLStruct.class.isAssignableFrom(returnType))
					{
						byte[] data;
						try {
							StringWriter sw = new StringWriter();
							(new XMLWriter()).writeXML((XMLStruct)retval, sw);
							data = getStringData(sw.toString());
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/xml", fname, new ByteArrayInputStream(data), data.length);
					}
					// JSON output.
					else if (JSONObject.class.isAssignableFrom(returnType))
					{
						byte[] data;
						try {
							data = getStringData(JSONWriter.writeJSONString((JSONObject)retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					// StringBuffer data output.
					else if (StringBuffer.class.isAssignableFrom(returnType))
					{
						sendStringData(response, mimeType, fname, ((StringBuffer)retval).toString());
					}
					// StringBuilder data output.
					else if (StringBuilder.class.isAssignableFrom(returnType))
					{
						sendStringData(response, mimeType, fname, ((StringBuilder)retval).toString());
					}
					// String data output.
					else if (String.class.isAssignableFrom(returnType))
					{
						sendStringData(response, mimeType, fname, (String)retval);
					}
					// binary output.
					else if (byte[].class.isAssignableFrom(returnType))
					{
						byte[] data = (byte[])retval;
						if (Common.isEmpty(mimeType))
							SmallUtil.sendData(response, getMimeType(), null, new ByteArrayInputStream(data), data.length);
						else
							SmallUtil.sendData(response, "application/octet-stream", null, new ByteArrayInputStream(data), data.length);
					}
					// Object JSON output.
					else
					{
						byte[] data;
						try {
							data = getStringData(JSONWriter.writeJSONString(retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					break;
				}
				default:
					// Do nothing.
					break;
			}
		}
	}
	
	/**
	 * Writes string data to the response.
	 * @param response the response object.
	 * @param mimeType the response MIME-Type.
	 * @param fileName the file name.
	 * @param data the string data to send.
	 */
	private void sendStringData(HttpServletResponse response, String mimeType, String fileName, String data)
	{
		byte[] bytedata = getStringData(data);
		if (Common.isEmpty(mimeType))
			SmallUtil.sendData(response, mimeType, fileName, new ByteArrayInputStream(bytedata), bytedata.length);
		else
			SmallUtil.sendData(response, "text/plain; charset=utf-8", fileName, new ByteArrayInputStream(bytedata), bytedata.length);
	}

	/**
	 * Converts a string to byte data.
	 */
	private byte[] getStringData(String data)
	{
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SmallFrameworkException(e);
		}
	}

}

