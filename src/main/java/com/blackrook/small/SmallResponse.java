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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Generic response object for Small.
 * @author Matthew Tropiano
 * @since 1.1.0
 * @since 1.2.1, implements AutoCloseable
 * @since 1.4.0, is an interface. See {@link GenericSmallResponse} for implementation.
 */
public interface SmallResponse extends AutoCloseable
{
	/**
	 * @return the response HTTP status code.
	 */
	public int getStatus();
	
	/**
	 * @return the header map for adding headers to the response.
	 */
	Map<String, List<String>> getHeaders();
	
	/**
	 * @return the response content.
	 */
	public Object getContent();

	@Override
	default void close() throws Exception
	{
		Object content = getContent();
		if (content instanceof AutoCloseable)
			((AutoCloseable)content).close();
	}
	
	/**
	 * Creates a new response with a default status.
	 * @param status the HTTP status to send.
	 * @return a new response.
	 */
	@SuppressWarnings("resource")
	public static GenericSmallResponse create(int status)
	{
		return (new GenericSmallResponse())
			.status(status)
		;
	}
	
	/**
	 * Creates a new response with status and content.
	 * @param status the HTTP status to send.
	 * @param content the content object.
	 * @return a new response.
	 */
	@SuppressWarnings("resource")
	public static GenericSmallResponse create(int status, Object content)
	{
		return (new GenericSmallResponse())
			.status(status)
			.content(content)
		;
	}
	
	/**
	 * Creates a new response with 200 OK status and content.
	 * @param content the content object.
	 * @return a new response.
	 */
	@SuppressWarnings("resource")
	public static GenericSmallResponse create(Object content)
	{
		return (new GenericSmallResponse())
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
	@SuppressWarnings("resource")
	public static GenericSmallResponse create(Object model, String viewName)
	{
		return (new GenericSmallResponse())
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
	@SuppressWarnings("resource")
	public static GenericSmallResponse create(int status, Object model, String viewName)
	{
		return (new GenericSmallResponse())
			.status(status)
			.content(SmallModelView.create(model, viewName))
		;
	}
	
	/**
	 * Creates a new response from another SmallResponse, copying the status, content, and the headers.
	 * @param smallResponse the source SmallResponse.
	 * @return a new response.
	 */
	public static GenericSmallResponse create(SmallResponse smallResponse)
	{
		GenericSmallResponse out = new GenericSmallResponse();
		out.status(smallResponse.getStatus());
		out.content(smallResponse.getContent());
		for (Map.Entry<String, List<String>> header : smallResponse.getHeaders().entrySet())
			for (String value : header.getValue())
				out.addHeader(header.getKey(), value);
		return out;
	}
	
	/**
	 * Creates a generic response object for Small Responses.
	 * @since 1.4.0
	 */
	public class GenericSmallResponse implements SmallResponse
	{
		private static final ThreadLocal<SimpleDateFormat> ISO_DATE = ThreadLocal.withInitial(()->
		{
			SimpleDateFormat out = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			out.setTimeZone(TimeZone.getTimeZone("GMT"));
			return out;
		});

		private static final Map<String, List<String>> EMPTY_HEADER_MAP 
			= Collections.unmodifiableMap(new HashMap<String, List<String>>(2));
		
		private Map<String, List<String>> headers;
		private int status;
		private Object content;
		
		private GenericSmallResponse()
		{
			this.status = 200;
			this.headers = null;
			this.content = null;
		}
		
		/**
		 * Sets the status on this response.
		 * @param status the status code.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse status(int status)
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
		public GenericSmallResponse header(String headerName, String value)
		{
			if (headers != null && headers.containsKey(headerName))
				headers.remove(headerName);
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
		public GenericSmallResponse dateHeader(String headerName, Date value)
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
		public GenericSmallResponse dateHeader(String headerName, long value)
		{
			return dateHeader(headerName, new Date(value));
		}
		
		/**
		 * Adds a header value.
		 * @param headerName the header name.
		 * @param value the corresponding value.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse addHeader(String headerName, String value)
		{
			if (headers == null)
				headers = new HashMap<>();
			List<String> valueList;
			if ((valueList = headers.get(headerName)) == null)
				headers.put(headerName, valueList = new LinkedList<>());
			valueList.add(value);
			return this;
		}
		
		/**
		 * Adds a header date value.
		 * @param headerName the header name.
		 * @param value the corresponding date value.
		 * @return itself, for call chaining.
		 * @since 1.2.0
		 */
		public GenericSmallResponse addDateHeader(String headerName, Date value)
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
		public GenericSmallResponse addDateHeader(String headerName, long value)
		{
			return addDateHeader(headerName, new Date(value));
		}
		
		/**
		 * Sets the content of the response.
		 * @param content the content.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse content(Object content)
		{
			this.content = content;
			return this;
		}
		
		/**
		 * Sets the MIME-Type (content type) header for the response.
		 * @param mimeType the MIME-Type.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse type(String mimeType)
		{
			return header("Content-Type", mimeType);
		}
		
		/**
		 * Sets the MIME-Type (content type) header for the response.
		 * @param mimeType the MIME-Type.
		 * @param charset the charset name.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse type(String mimeType, String charset)
		{
			return header("Content-Type", mimeType + "; charset=" + charset);
		}
		
		/**
		 * Sets that this response is an attachment.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse attachment()
		{
			return header("Content-Disposition", "attachment");
		}
		
		/**
		 * Sets that this response is an attachment with a filename.
		 * @param fileName the file name.
		 * @return itself, for call chaining.
		 */
		public GenericSmallResponse attachment(String fileName)
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
		public Map<String, List<String>> getHeaders()
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

}
