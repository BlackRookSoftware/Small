package com.blackrook.j2ee.small;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.small.controller.ControllerEntryPoint;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.filter.FilterProfile;
import com.blackrook.j2ee.small.parser.MultipartParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartParserException;
import com.blackrook.j2ee.small.struct.HashDequeMap;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.struct.URITrie.Result;
import com.blackrook.j2ee.small.util.SmallRequestUtil;
import com.blackrook.j2ee.small.util.SmallResponseUtil;
import com.blackrook.j2ee.small.util.SmallUtil;

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
		if (MultipartFormDataParser.isMultipart(request))
		{
			MultipartParser parser = SmallRequestUtil.getMultipartParser(request);
			if (parser == null)
				SmallResponseUtil.sendError(response, 400, "The multipart POST request type is not supported.");
			else
			{
				try {
					parser.parse(request, SmallToolkit.INSTANCE.getTemporaryDirectory());
				} catch (UnsupportedEncodingException e) {
					SmallResponseUtil.sendError(response, 400, "The encoding type for the POST request is not supported.");
				} catch (MultipartParserException e) {
					SmallResponseUtil.sendError(response, 500, "The server could not parse the multiform request. " + e.getMessage());
				} catch (IOException e) {
					SmallResponseUtil.sendError(response, 500, "The server could not read the request. " + e.getMessage());
				}
				
				List<Part> parts = parser.getPartList();
				
				HashDequeMap<String, Part> partMap = new HashDequeMap<>();
				for (Part part : parts)
					partMap.addLast(part.getName(), part);
				
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
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashDequeMap<String, Part> multiformPartMap)
	{
		String path = SmallUtil.trimSlashes(SmallRequestUtil.getPath(request));
		Result result = SmallToolkit.INSTANCE.getControllerEntryPointByURI(requestMethod, path);
		
		if (result == null || !result.hasEndpoint())
		{
			SmallResponseUtil.sendError(response, 404, "Not found. No handler for "+requestMethod.name()+ " '"+path+"'");
			return;
		}
		
		// get cookies from request.
		Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
		Cookie[] cookies = request.getCookies();
		if (cookies != null) for (Cookie c : cookies)
			cookieMap.put(c.getName(), c);
		
		// Get path variables.
		Map<String, String> pathVariables = result.getPathVariables() != null ? result.getPathVariables() : (new HashMap<String, String>());
		
		ControllerEntryPoint entryPoint = result.getEntryPoint();
		
		for (Class<?> filterClass : entryPoint.getFilterChain())
		{
			FilterProfile filterProfile = SmallToolkit.INSTANCE.getFilter(filterClass);
			if (!filterProfile.getEntryMethod().handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap))
				return;
		}

		// Call Entry
		entryPoint.handleCall(
			requestMethod, 
			request, 
			response, 
			pathVariables, 
			cookieMap, 
			multiformPartMap
		);
	}
	
}
