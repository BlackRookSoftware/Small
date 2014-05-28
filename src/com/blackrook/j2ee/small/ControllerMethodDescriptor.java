package com.blackrook.j2ee.small;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.blackrook.j2ee.small.annotation.Attachment;
import com.blackrook.j2ee.small.annotation.Content;
import com.blackrook.j2ee.small.annotation.ControllerEntry;
import com.blackrook.j2ee.small.annotation.FilterChain;
import com.blackrook.j2ee.small.annotation.NoCache;
import com.blackrook.j2ee.small.annotation.View;
import com.blackrook.j2ee.small.exception.SimpleFrameworkSetupException;

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
				if (type == Void.class || type == Void.TYPE)
					throw new SimpleFrameworkSetupException("Entry methods that are annotated @Content cannot return void.");
				this.outputType = Output.CONTENT;
			}
			else if (method.isAnnotationPresent(Attachment.class))
			{
				if (type == Void.class || type == Void.TYPE)
					throw new SimpleFrameworkSetupException("Entry methods that are annotated @Attachment cannot return void.");
				this.outputType = Output.ATTACHMENT;
			}
			else if (method.isAnnotationPresent(View.class))
			{
				if (type == Void.class || type == Void.TYPE)
					throw new SimpleFrameworkSetupException("Entry methods that are annotated @View cannot return void.");
				this.outputType = Output.VIEW;
			}
			else if (type != Void.class && type != Void.TYPE)
				throw new SimpleFrameworkSetupException("Entry methods that don't return void must be annotated with @Content, @Attachment, or @View.");
		}
		
		// Search backwards for relevant filters.
		
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

