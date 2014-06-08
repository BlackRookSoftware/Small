package com.blackrook.j2ee.small;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.blackrook.commons.Common;
import com.blackrook.j2ee.small.annotation.Attachment;
import com.blackrook.j2ee.small.annotation.Content;
import com.blackrook.j2ee.small.annotation.ControllerEntry;
import com.blackrook.j2ee.small.annotation.FilterChain;
import com.blackrook.j2ee.small.annotation.NoCache;
import com.blackrook.j2ee.small.annotation.View;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;

/**
 * Method descriptor class, specifically for controllers.
 * @author Matthew Tropiano
 */
public class ControllerMethodDescriptor extends MethodDescriptor
{
	private static final Class<?>[] NO_FILTERS = new Class<?>[0];

	/** Controller output handling types. */
	public static enum Output
	{
		CONTENT,
		ATTACHMENT,
		VIEW;
	}

	/** Output content. */
	private Output outputType;
	/** Forced MIME type. */
	private String mimeType;
	/** No cache? */
	private boolean noCache;
	/** Filter class list. */
	private Class<?>[] filterChain;

	ControllerMethodDescriptor(Method method)
	{
		super(method);
		this.outputType = null;
		this.noCache = method.isAnnotationPresent(NoCache.class);
		this.filterChain = NO_FILTERS;
		
		if (method.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = method.getAnnotation(FilterChain.class);
			this.filterChain = Arrays.copyOf(fc.value(), fc.value().length);
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
		
		// Search backwards for relevant filters.
		
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
	 * Gets this method's filter chain.
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
	}

}

