package com.blackrook.j2ee.small.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.xml.sax.SAXException;

import com.blackrook.commons.Reflect;
import com.blackrook.j2ee.small.parser.MultipartParser;
import com.blackrook.j2ee.small.parser.RFCParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.lang.json.JSONConversionException;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONReader;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;

/**
 * Utility class for {@link HttpServletRequest} manipulation.
 * @author Matthew Tropiano
 */
public final class RequestUtil
{
	private RequestUtil() {}

	/**
	 * Reads XML data from the request and returns an XMLStruct.
	 */
	public static XMLStruct readXML(HttpServletRequest request) throws SAXException, IOException
	{
		XMLStruct xml = null;
		ServletInputStream sis = request.getInputStream();
		try {
			xml = XMLStructFactory.readXML(sis);
		} catch (IOException e) {
			throw e;
		} catch (SAXException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return xml;
	}

	/**
	 * Checks if the request is XML-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isXML(HttpServletRequest request)
	{
		String type = request.getContentType();
		return type.startsWith("application/xml") 
			|| type.startsWith("application/xop+xml")
			|| type.startsWith("application/rss+xml")
			;
	}

	/**
	 * Reads JSON data from the request and returns a JSONObject.
	 */
	public static JSONObject readJSON(HttpServletRequest request) throws UnsupportedEncodingException, JSONConversionException, IOException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
		String charset = "UTF-8";
		while (parser.hasTokens())
		{
			String nextToken = parser.nextToken();
			if (nextToken.startsWith("charset="))
				charset = nextToken.substring("charset=".length()).trim();
		}
	
		JSONObject jsonObject = null;
		ServletInputStream sis = request.getInputStream();
		try {
			jsonObject = JSONReader.readJSON(new InputStreamReader(sis, charset));
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (JSONConversionException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return jsonObject;
	}

	/**
	 * Checks if the request is JSON-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isJSON(HttpServletRequest request)
	{
		return request.getContentType().startsWith("application/json");
	}

	/**
	 * Reads plaintext content.
	 * @param request the request object.
	 * @return the read string in native encoding (Java UTF16).
	 */
	public static String readPlainText(HttpServletRequest request) throws UnsupportedEncodingException, IOException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
		String charset = "UTF-8";
		while (parser.hasTokens())
		{
			String nextToken = parser.nextToken();
			if (nextToken.startsWith("charset="))
				charset = nextToken.substring("charset=".length()).trim();
		}
	
		StringBuffer sb = new StringBuffer();
		ServletInputStream sis = request.getInputStream();
		try {
	
			int buf = 0;
			char[] c = new char[16384];
			InputStreamReader ir = new InputStreamReader(sis, charset);
			while ((buf = ir.read(c)) >= 0)
				sb.append(c, 0, buf);
				
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return sb.toString();
	}

	/**
	 * Checks if this request is a regular form POST. 
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isFormEncoded(HttpServletRequest request)
	{
		return request.getContentType().equals("application/x-www-form-urlencoded");
	}

	/**
	 * Returns the appropriate multipart parser for a request.
	 * @param request the request.
	 */
	public static MultipartParser getMultipartParser(HttpServletRequest request)
	{
		String contentType = request.getContentType();
		
		if (contentType.startsWith("multipart/form-data"))
			return new MultipartFormDataParser();
		else
			return null;
	}

	/**
	 * Get the base page parsed out of the request URI.
	 */
	public static String getPage(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int slashIndex = requestURI.lastIndexOf('/');
		int endIndex = requestURI.indexOf('?');
		if (endIndex >= 0)
			return requestURI.substring(slashIndex + 1, endIndex);
		else
			return requestURI.substring(slashIndex + 1); 
	}

	/**
	 * Get content data.
	 */
	public static <T> T getContentData(HttpServletRequest request, Class<T> type) 
		throws UnsupportedEncodingException, JSONConversionException, SAXException, IOException
	{
		if (RequestUtil.isJSON(request))
		{
			JSONObject json = RequestUtil.readJSON(request);
			if (type == JSONObject.class)
				return type.cast(json);
			else
				return json.newObject(type);
		}
		else if (RequestUtil.isXML(request))
		{
			XMLStruct xmlStruct = RequestUtil.readXML(request);
			if (type == XMLStruct.class)
				return type.cast(xmlStruct);
			else
				throw new ClassCastException("Expected XMLStruct class type for XML data.");
		}
		else
		{
			String content = RequestUtil.readPlainText(request);
			return Reflect.createForType(content, type);
		}
	}
	
}
