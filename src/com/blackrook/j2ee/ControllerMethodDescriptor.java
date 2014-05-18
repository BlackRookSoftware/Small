package com.blackrook.j2ee;

import java.lang.reflect.Method;

import com.blackrook.j2ee.annotation.Attachment;
import com.blackrook.j2ee.annotation.Content;
import com.blackrook.j2ee.annotation.NoCache;
import com.blackrook.j2ee.annotation.ControllerEntry;
import com.blackrook.j2ee.annotation.View;
import com.blackrook.j2ee.exception.SimpleFrameworkSetupException;

/**
 * Method descriptor class, specifically for controllers.
 * @author Matthew Tropiano
 */
public class ControllerMethodDescriptor extends MethodDescriptor
{
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
	
	ControllerMethodDescriptor(Method method)
	{
		super(method);
		this.outputType = null;
		this.noCache = method.isAnnotationPresent(NoCache.class);
		
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
	
}

