package com.blackrook.j2ee.small;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.blackrook.j2ee.small.annotation.Attribute;
import com.blackrook.j2ee.small.annotation.AutoTrim;
import com.blackrook.j2ee.small.annotation.Content;
import com.blackrook.j2ee.small.annotation.CookieParameter;
import com.blackrook.j2ee.small.annotation.Header;
import com.blackrook.j2ee.small.annotation.HeaderMap;
import com.blackrook.j2ee.small.annotation.Model;
import com.blackrook.j2ee.small.annotation.Parameter;
import com.blackrook.j2ee.small.annotation.ParameterMap;
import com.blackrook.j2ee.small.annotation.Path;
import com.blackrook.j2ee.small.annotation.PathFile;
import com.blackrook.j2ee.small.annotation.PathQuery;
import com.blackrook.j2ee.small.annotation.PathRemainder;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.enums.ScopeType;

/**
 * Method descriptor class.
 * Parses a method's characteristics using reflection, 
 * yielding a digest of its important contents.
 * @author Matthew Tropiano 
 */
public class MethodDescriptor
{
	/** Parameter source types. */
	public static enum Source
	{
		PATH,
		PATH_FILE,
		PATH_QUERY,
		PATH_REMAINDER,
		SERVLET_REQUEST,
		SERVLET_RESPONSE,
		SESSION,
		SERVLET_CONTEXT,
		HEADER,
		HEADER_MAP,
		METHOD_TYPE,
		COOKIE,
		ATTRIBUTE,
		PARAMETER,
		PARAMETER_MAP,
		CONTENT,
		MODEL;
	}
	
	/**
	 * Parameter entry for the descriptor.
	 */
	public static class ParameterDescriptor
	{
		private Class<?> type;
		private Source sourceType;
		private ScopeType sourceScopeType;
		private String name;
		private boolean trim;
		
		protected ParameterDescriptor(Source sourceType, ScopeType scope, Class<?> type, String name, boolean trim)
		{
			this.type = type;
			this.sourceType = sourceType;
			this.sourceScopeType = scope;
			this.name = name;
			this.trim = trim;
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

		public boolean getTrim()
		{
			return trim;
		}
	}

	/** Method. */
	private Method method;
	/** Return type. */
	private Class<?> type;
	/** Parameter entry. */
	private ParameterDescriptor[] parameters;

	MethodDescriptor(Method method)
	{
		this.method = method;
		this.type = method.getReturnType();

		Annotation[][] pannotations = method.getParameterAnnotations();
		Class<?>[] ptypes = method.getParameterTypes();
		
		this.parameters = new ParameterDescriptor[ptypes.length];
		for (int i = 0; i < ptypes.length; i++)
		{
			Source source = null;
			ScopeType scope = null;
			String name = null;
			Class<?> paramType = ptypes[i];
			boolean trim = false;
			
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
				
				if (annotation.annotationType() == AutoTrim.class)
					trim = true;
				
				if (annotation.annotationType() == Path.class)
					source = Source.PATH;
				else if (annotation.annotationType() == PathFile.class)
					source = Source.PATH_FILE;
				else if (annotation.annotationType() == PathQuery.class)
					source = Source.PATH_QUERY;
				else if (annotation.annotationType() == PathRemainder.class)
					source = Source.PATH_REMAINDER;
				else if (annotation.annotationType() == Content.class)
					source = Source.CONTENT;
				else if (annotation.annotationType() == ParameterMap.class)
					source = Source.PARAMETER_MAP;
				else if (annotation.annotationType() == HeaderMap.class)
					source = Source.HEADER_MAP;
				else if (annotation.annotationType() == Header.class)
				{
					source = Source.HEADER;
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
			
			this.parameters[i] = new ParameterDescriptor(source, scope, paramType, name, trim);
		}
		
	}

	/**
	 * The actual method that this describes.
	 */
	public Method getMethod()
	{
		return method;
	}

	/**
	 * The method's return type.
	 */
	public Class<?> getType()
	{
		return type;
	}

	/**
	 * The method's parameter info.
	 */
	public ParameterDescriptor[] getParameterDescriptors()
	{
		return parameters;
	}

}

