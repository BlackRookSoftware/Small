package com.blackrook.small;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.IterationTag;

import com.blackrook.small.annotation.Component;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.util.SmallUtil;

/**
 * A body tag that provides access to the framework toolkit.
 * @author Matthew Tropiano
 */
public abstract class SmallTag extends BodyTagSupport
{
	private static final long serialVersionUID = -3952524726929216608L;
	
	/**
	 * Response type for tag start.
	 */
	public enum StartResponse
	{
		EVALUATE_BODY(SmallTag.EVAL_BODY_INCLUDE),
		SKIP_BODY(SmallTag.SKIP_BODY);
		
		private final int eval;
		StartResponse(int eval) {this.eval = eval;}
	}
	
	/**
	 * Response type for tag end.
	 */
	public enum EndResponse
	{
		EVALUATE_BODY(SmallTag.EVAL_BODY_INCLUDE),
		EVALUATE_BODY_AGAIN(IterationTag.EVAL_BODY_AGAIN),
		EVALUATE_PAGE(SmallTag.EVAL_PAGE),
		SKIP_PAGE(SmallTag.SKIP_PAGE);
		
		private final int eval;
		EndResponse(int eval) {this.eval = eval;}
	}
	
	@Override
	public final int doStartTag() throws JspException 
	{
		return onStart(
			(HttpServletRequest)pageContext.getRequest(),
			(HttpServletResponse)pageContext.getResponse()
		).eval;
	}

	@Override
	public final int doEndTag() throws JspException
	{
		return onEnd(
			(HttpServletRequest)pageContext.getRequest(),
			(HttpServletResponse)pageContext.getResponse()
		).eval;
	}
	
	/**
	 * @return the writer for writing directly to the page's output stream. 
	 */
	public final JspWriter getWriter()
	{
		return pageContext.getOut();
	}
	
	/**
	 * Gets and auto-casts an object bean stored at the page context level.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param <T> object type.
	 * @return a typecast object on the application scope.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public final <T> T getPageBean(Class<T> clazz, String name)
	{
		return getPageBean(clazz, name, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @param <T> object type.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public final <T> T getPageBean(Class<T> clazz, String name, boolean create)
	{
		Object obj = pageContext.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.getDeclaredConstructor().newInstance() : null;
				pageContext.setAttribute(name, obj);
			} catch (Exception e) {
				throw new SmallFrameworkException(e);
			}
		}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
	} 
	
	/**
	 * Called at the start of the tag evaluation.
	 * Implementors are encouraged to override this method.
	 * By default, returns {@link StartResponse#EVALUATE_BODY}.
	 * @param request the servlet request.
	 * @param response the servlet response.
	 * @return a {@link StartResponse}.  
	 */
	public StartResponse onStart(HttpServletRequest request, HttpServletResponse response)
	{
		return StartResponse.EVALUATE_BODY;
	}

	/**
	 * Called at the end of the tag evaluation.
	 * Implementors are encouraged to override this method.
	 * By default, returns {@link EndResponse#EVALUATE_PAGE}.
	 * @param request the servlet request.
	 * @param response the servlet response.
	 * @return an {@link EndResponse}.  
	 */
	public EndResponse onEnd(HttpServletRequest request, HttpServletResponse response)
	{
		return EndResponse.EVALUATE_PAGE;
	}

	/**
	 * Returns a singleton component instantiated by Small.
	 * @param clazz the component class.
	 * @param <T> the object type.
	 * @return a singleton component annotated with {@link Component} by class.
	 */
	protected <T> T getComponent(Class<T> clazz)
	{
		return getEnvironment().getComponent(clazz);
	}

	/**
	 * @return the Small environment.
	 */
	protected SmallEnvironment getEnvironment()
	{
		return SmallUtil.getEnvironment(pageContext.getServletContext());
	}

}
