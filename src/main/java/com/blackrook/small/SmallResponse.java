/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.blackrook.small.struct.HashDequeMap;

/**
 * Creates a generic response object for Small Responses.
 * @author Matthew Tropiano
 * @since 1.1.0
 * @since 1.2.1, implements AutoCloseable
 */
@SuppressWarnings("resource")
public class SmallResponse implements AutoCloseable
{
	private static final ThreadLocal<SimpleDateFormat> ISO_DATE = ThreadLocal.withInitial(()->
	{
		SimpleDateFormat out = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		out.setTimeZone(TimeZone.getTimeZone("GMT"));
		return out;
	});

	private static final Map<String, Deque<String>> EMPTY_HEADER_MAP 
		= Collections.unmodifiableMap(new HashMap<String, Deque<String>>(2));
	
	private HashDequeMap<String, String> headers;
	private int status;
	private Object content;
	
	private SmallResponse()
	{
		this.status = 200;
		this.headers = null;
		this.content = null;
	}
	
	/**
	 * Creates a new response with a default status.
	 * @param status the HTTP status to send.
	 * @return a new response.
	 */
	public static SmallResponse create(int status)
	{
		return (new SmallResponse())
			.status(status)
		;
	}
	
	/**
	 * Creates a new response with status and content.
	 * @param status the HTTP status to send.
	 * @param content the content object.
	 * @return a new response.
	 */
	public static SmallResponse create(int status, Object content)
	{
		return (new SmallResponse())
			.status(status)
			.content(content)
		;
	}
	
	/**
	 * Creates a new response with 200 OK status and content.
	 * @param content the content object.
	 * @return a new response.
	 */
	public static SmallResponse create(Object content)
	{
		return (new SmallResponse())
			.status(200)
			.content(content)
		;
	}
	
	/**
	 * Creates a new response with 200 OK status and a model-view driven content.
	 * @param model the model to render.
	 * @param viewName the name of the view to resolve and use.
	 * @return a new response.
	 */
	public static SmallResponse create(Object model, String viewName)
	{
		return (new SmallResponse())
			.status(200)
			.content(SmallModelView.create(model, viewName))
		;
	}
	
	/**
	 * Creates a new response with 200 OK status and a model-view driven content.
	 * @param status the HTTP status to send.
	 * @param model the model to render.
	 * @param viewName the name of the view to resolve and use.
	 * @return a new response.
	 */
	public static SmallResponse create(int status, Object model, String viewName)
	{
		return (new SmallResponse())
			.status(status)
			.content(SmallModelView.create(model, viewName))
		;
	}
	
	/**
	 * Sets the status on this response.
	 * @param status the status code.
	 * @return itself, for call chaining.
	 */
	public SmallResponse status(int status)
	{
		this.status = status;
		return this;
	}
	
	/**
	 * Sets a header value.
	 * If this header was added more than once, it is cleared before the add.
	 * @param headerName the header name.
	 * @param value the corresponding value.
	 * @return itself, for call chaining.
	 */
	public SmallResponse header(String headerName, String value)
	{
		if (this.headers != null && this.headers.containsKey(headerName))
			this.headers.remove(headerName);
		return addHeader(headerName, value);
	}
	
	/**
	 * Sets a header date value.
	 * If this header was added more than once, it is cleared before the add.
	 * @param headerName the header name.
	 * @param value the corresponding date value.
	 * @return itself, for call chaining.
	 * @since 1.2.0
	 */
	public SmallResponse dateHeader(String headerName, Date value)
	{
		return header(headerName, ISO_DATE.get().format(value));
	}
	
	/**
	 * Sets a header date value.
	 * If this header was added more than once, it is cleared before the add.
	 * @param headerName the header name.
	 * @param value the corresponding value.
	 * @return itself, for call chaining.
	 * @since 1.2.0
	 */
	public SmallResponse dateHeader(String headerName, long value)
	{
		return dateHeader(headerName, new Date(value));
	}
	
	/**
	 * Adds a header value.
	 * @param headerName the header name.
	 * @param value the corresponding value.
	 * @return itself, for call chaining.
	 */
	public SmallResponse addHeader(String headerName, String value)
	{
		if (this.headers == null)
			this.headers = new HashDequeMap<String, String>();
		this.headers.add(headerName, value);
		return this;
	}
	
	/**
	 * Adds a header date value.
	 * @param headerName the header name.
	 * @param value the corresponding date value.
	 * @return itself, for call chaining.
	 * @since 1.2.0
	 */
	public SmallResponse addDateHeader(String headerName, Date value)
	{
		return addHeader(headerName, ISO_DATE.get().format(value));
	}
	
	/**
	 * Adds a header date value.
	 * @param headerName the header name.
	 * @param value the corresponding date value in milliseconds since the Epoch.
	 * @return itself, for call chaining.
	 * @since 1.2.0
	 */
	public SmallResponse addDateHeader(String headerName, long value)
	{
		return addDateHeader(headerName, new Date(value));
	}
	
	/**
	 * Sets the content of the response.
	 * @param content the content.
	 * @return itself, for call chaining.
	 */
	public SmallResponse content(Object content)
	{
		this.content = content;
		return this;
	}
	
	/**
	 * Sets the MIME-Type (content type) header for the response.
	 * @param mimeType the MIME-Type.
	 * @return itself, for call chaining.
	 */
	public SmallResponse type(String mimeType)
	{
		return header("Content-Type", mimeType);
	}
	
	/**
	 * Sets the MIME-Type (content type) header for the response.
	 * @param mimeType the MIME-Type.
	 * @param charset the charset name.
	 * @return itself, for call chaining.
	 */
	public SmallResponse type(String mimeType, String charset)
	{
		return header("Content-Type", mimeType + "; charset=" + charset);
	}
	
	/**
	 * Sets that this response is an attachment.
	 * @return itself, for call chaining.
	 */
	public SmallResponse attachment()
	{
		return header("Content-Disposition", "attachment");
	}
	
	/**
	 * Sets that this response is an attachment with a filename.
	 * @param fileName the file name.
	 * @return itself, for call chaining.
	 */
	public SmallResponse attachment(String fileName)
	{
		return header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
	}
	
	/**
	 * @return the status code.
	 */
	public int getStatus()
	{
		return status;
	}
	
	/**
	 * @return the header map.
	 */
	public Map<String, Deque<String>> getHeaders()
	{
		return headers != null ? headers : EMPTY_HEADER_MAP;
	}
	
	/**
	 * @return the response content.
	 */
	public Object getContent()
	{
		return content;
	}

	@Override
	public void close() throws Exception
	{
		if (content instanceof AutoCloseable)
			((AutoCloseable)content).close();
	}
	
}
