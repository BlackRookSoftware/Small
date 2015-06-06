package com.blackrook.j2ee.small;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.small.descriptor.ControllerDescriptor;
import com.blackrook.j2ee.small.descriptor.ControllerMethodDescriptor;
import com.blackrook.j2ee.small.descriptor.FilterDescriptor;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.parser.MultipartParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartParserException;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.util.RequestUtil;
import com.blackrook.j2ee.small.util.ResponseUtil;

/**
 * The main dispatcher servlet for the controller portion of the framework.
 * @author Matthew Tropiano
 */
public final class SmallDispatcher extends HttpServlet
{
	private static final long serialVersionUID = -5986230302849170240L;
	
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.GET, null);
	}

	@Override
	public final void doHead(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.HEAD, null);
	}

	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.PUT, null);
	}

	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.DELETE, null);
	}

	@Override
	public final void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		callControllerEntry(request, response, RequestMethod.OPTIONS, null);
	}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		if (RequestUtil.isFormEncoded(request))
			callControllerEntry(request, response, RequestMethod.POST, null);
		else if (MultipartFormDataParser.isMultipart(request))
		{
			MultipartParser parser = RequestUtil.getMultipartParser(request);
			if (parser == null)
				ResponseUtil.sendError(response, 400, "The multipart POST request type is not supported.");
			else
			{
				try {
					parser.parse(request, SmallToolkit.INSTANCE.getTemporaryDirectory());
				} catch (UnsupportedEncodingException e) {
					ResponseUtil.sendError(response, 400, "The encoding type for the POST request is not supported.");
				} catch (MultipartParserException e) {
					ResponseUtil.sendError(response, 500, "The server could not parse the multiform request. " + e.getMessage());
				} catch (IOException e) {
					ResponseUtil.sendError(response, 500, "The server could not read the request. " + e.getMessage());
				}
				
				List<Part> parts = parser.getPartList();
				
				HashedQueueMap<String, Part> partMap = new HashedQueueMap<String, Part>();
				for (Part part : parts)
					partMap.enqueue(part.getName(), part);
				
				try {
					callControllerEntry(request, response, RequestMethod.POST, partMap);
				} finally {
					// clean up files.
					for (Part part : parts)
						if (part.isFile())
						{
							File tempFile = part.getFile();
							tempFile.delete();
						}
				}
			}
		}
		else
			callControllerEntry(request, response, RequestMethod.POST, null);
	}
	
	/**
	 * Fetches a regular controller entry and invokes the correct method.
	 */
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashedQueueMap<String, Part> multiformPartMap)
	{
		String path = SmallUtil.removeEndingSlash(request.getRequestURI());
		List<String> remainder = new List<String>(4);
		Class<?> controllerClass = SmallToolkit.INSTANCE.getControllerClassByPath(path, remainder);
		if (controllerClass == null)
		{
			ResponseUtil.sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
			return;
		}

		ControllerDescriptor entry = SmallToolkit.INSTANCE.getController(controllerClass);
		if (entry == null)
			ResponseUtil.sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String pageRemainder = "/" + Common.joinStrings("/", remainder);

			// get cookies from request.
			HashMap<String, Cookie> cookieMap = new HashMap<String, Cookie>();
			Cookie[] cookies = request.getCookies();
			if (cookies != null) for (Cookie c : cookies)
				cookieMap.put(c.getName(), c);
			
			ControllerMethodDescriptor cmd = entry.getDescriptorUsingPath(requestMethod, SmallUtil.removeBeginningSlash(pageRemainder));
			if (cmd != null)
			{
				for (Class<?> filterClass : entry.getFilterChain())
				{
					FilterDescriptor fd = SmallToolkit.INSTANCE.getFilter(filterClass);
					if (!fd.handleCall(requestMethod, request, response, pageRemainder, cookieMap, multiformPartMap))
						return;
				}

				for (Class<?> filterClass : cmd.getFilterChain())
				{
					FilterDescriptor fd = SmallToolkit.INSTANCE.getFilter(filterClass);
					if (!fd.handleCall(requestMethod, request, response, pageRemainder, cookieMap, multiformPartMap))
						return;
				}

				entry.handleCall(requestMethod, request, response, cmd, pageRemainder, cookieMap, multiformPartMap);
			}
			else
				SmallUtil.sendCode(response, 404, "Not found.");
		}
	}

	
}
