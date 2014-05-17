package com.blackrook.j2ee;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.blackrook.j2ee.annotation.Attachment;
import com.blackrook.j2ee.annotation.Attribute;
import com.blackrook.j2ee.annotation.Content;
import com.blackrook.j2ee.annotation.CookieParameter;
import com.blackrook.j2ee.annotation.Header;
import com.blackrook.j2ee.annotation.Model;
import com.blackrook.j2ee.annotation.NoCache;
import com.blackrook.j2ee.annotation.Parameter;
import com.blackrook.j2ee.annotation.Path;
import com.blackrook.j2ee.annotation.PathFile;
import com.blackrook.j2ee.annotation.PathQuery;
import com.blackrook.j2ee.annotation.RequestEntry;
import com.blackrook.j2ee.annotation.View;
import com.blackrook.j2ee.enums.RequestMethod;
import com.blackrook.j2ee.enums.ScopeType;
import com.blackrook.j2ee.exception.SimpleFrameworkSetupException;

/**
 * Method descriptor class.
 * @author 
 */
public class MethodDescriptor
{
	public static enum Source
	{
		PATH,
		PATH_FILE,
		PATH_QUERY,
		SERVLET_REQUEST,
		SERVLET_RESPONSE,
		SESSION,
		SERVLET_CONTEXT,
		HEADER_VALUE,
		METHOD_TYPE,
		COOKIE,
		ATTRIBUTE,
		PARAMETER,
		PARAMETER_MAP,
		CONTENT,
		MODEL;
	}
	
	public static enum Output
	{
		CONTENT,
		ATTACHMENT,
		VIEW;
	}

	/**
	 * Parameter entry for the descriptor.
	 */
	public static class ParameterInfo
	{
		private Class<?> type;
		private Source sourceType;
		private ScopeType sourceScopeType;
		private String name;
		
		private ParameterInfo(Source sourceType, ScopeType scope, Class<?> type, String name)
		{
			this.type = type;
			this.sourceType = sourceType;
			this.sourceScopeType = scope;
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public Class<?> getType()
		{
			return type;
		}

		public Source getSourceType()
		{
			return sourceType;
		}

		public ScopeType getSourceScopeType()
		{
			return sourceScopeType;
		}
	}

	/** Method. */
	private Method method;
	/** Return type. */
	private Class<?> type;
	/** Output content. */
	private Output outputType;
	/** Parameter entry. */
	private ParameterInfo[] parameters;
	/** No cache? */
	private boolean noCache;
	
	MethodDescriptor(String controllerName, Method method)
	{
		this.method = method;
		this.type = method.getReturnType();
		this.outputType = null;
		this.noCache = method.isAnnotationPresent(NoCache.class);
		
		if (method.isAnnotationPresent(RequestEntry.class))
		{
			if (method.isAnnotationPresent(Content.class))
				this.outputType = Output.CONTENT;
			else if (method.isAnnotationPresent(Attachment.class))
				this.outputType = Output.ATTACHMENT;
			else if (method.isAnnotationPresent(View.class))
				this.outputType = Output.VIEW;
			else if (this.type != Void.class && this.type != Void.TYPE)
				throw new SimpleFrameworkSetupException("Entry methods that don't return void must be annotated with @Content, @Attachment, or @View.");
		}
		
		Annotation[][] pannotations = method.getParameterAnnotations();
		Class<?>[] ptypes = method.getParameterTypes();
		
		this.parameters = new ParameterInfo[ptypes.length];
		for (int i = 0; i < ptypes.length; i++)
		{
			Source source = null;
			ScopeType scope = null;
			String name = null;
			Class<?> paramType = ptypes[i];
			
			if (paramType == RequestMethod.class)
				source = Source.METHOD_TYPE;
			else if (paramType == ServletContext.class)
				source = Source.SERVLET_CONTEXT;
			else if (paramType == HttpSession.class)
				source = Source.SESSION;
			else if (paramType == HttpServletRequest.class)
				source = Source.SERVLET_REQUEST;
			else if (paramType == HttpServletResponse.class)
				source = Source.SERVLET_RESPONSE;
			else for (int a = 0; a < pannotations[i].length; a++)
			{
				Annotation annotation = pannotations[i][a];
				if (annotation.annotationType() == Path.class)
					source = Source.PATH;
				else if (annotation.annotationType() == PathFile.class)
					source = Source.PATH_FILE;
				else if (annotation.annotationType() == PathQuery.class)
					source = Source.PATH_QUERY;
				else if (annotation.annotationType() == Content.class)
					source = Source.CONTENT;
				else if (annotation.annotationType() == Header.class)
				{
					source = Source.HEADER_VALUE;
					Header p = (Header)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Model.class)
				{
					source = Source.MODEL;
					Model p = (Model)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Parameter.class)
				{
					source = Source.PARAMETER;
					Parameter p = (Parameter)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Attribute.class)
				{
					source = Source.ATTRIBUTE;
					Attribute p = (Attribute)annotation;
					name = p.value();
					scope = p.scope();
				}
				else if (annotation.annotationType() == CookieParameter.class)
				{
					source = Source.COOKIE;
					CookieParameter p = (CookieParameter)annotation;
					name = p.value();
				}
			}
			
			this.parameters[i] = new ParameterInfo(source, scope, paramType, name);
		}
		
	}

	public Method getMethod()
	{
		return method;
	}

	public Class<?> getType()
	{
		return type;
	}

	public Output getOutputType()
	{
		return outputType;
	}

	public ParameterInfo[] getParameters()
	{
		return parameters;
	}

	public boolean isNoCache()
	{
		return noCache;
	}
	
}

