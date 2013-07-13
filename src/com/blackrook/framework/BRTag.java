package com.blackrook.framework;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * A (non-body) tag that provides access to the framework toolkit.
 * @author Matthew Tropiano
 */
public abstract class BRTag extends TagSupport
{
	private static final long serialVersionUID = -3952524726929216608L;
	
	/**
	 * Response type for tag start.
	 */
	public enum StartResponse
	{
		EVALUATE_BODY(Tag.EVAL_BODY_INCLUDE),
		SKIP_BODY(Tag.SKIP_BODY);
		
		private final int eval;
		StartResponse(int eval) {this.eval = eval;}
		}
	
	/**
	 * Response type for tag end.
	 */
	public enum EndResponse
	{
		EVALUATE_BODY(Tag.EVAL_BODY_INCLUDE),
		EVALUATE_BODY_AGAIN(IterationTag.EVAL_BODY_AGAIN),
		EVALUATE_PAGE(Tag.EVAL_PAGE),
		SKIP_PAGE(Tag.SKIP_PAGE);
		
		private final int eval;
		EndResponse(int eval) {this.eval = eval;}
		}
	
	/**
	 * Gets the Black Rook Framework Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}

	/**
	 * Gets the servlet context.
	 */
	public final ServletContext getServletContext()
	{
		return getToolkit().getServletContext();
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
	public int doEndTag() throws JspException
	{
		return onEnd(
			(HttpServletRequest)pageContext.getRequest(),
			(HttpServletResponse)pageContext.getResponse()
			).eval;
		}
	
	/**
	 * Called at the start of the tag evaluation.
	 * @param request the servlet request.
	 * @param response the servlet response.
	 * @return a {@link StartResponse}.  
	 */
	public StartResponse onStart(HttpServletRequest request, HttpServletResponse response)
	{
		return StartResponse.SKIP_BODY;
		}

	/**
	 * Called at the end of the tag evaluation.
	 * @param request the servlet request.
	 * @param response the servlet response.
	 * @return an {@link EndResponse}.  
	 */
	public EndResponse onEnd(HttpServletRequest request, HttpServletResponse response)
	{
		return EndResponse.EVALUATE_PAGE;
		}

	/**
	 * Returns the writer for writing directly to the page's output stream. 
	 */
	public JspWriter getWriter()
	{
		return pageContext.getOut();
		}
	
	/**
	 * Gets and auto-casts an object bean stored at the page context level.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public <T> T getPageBean(Class<T> clazz, String name)
	{
		return getPageBean(clazz, name, true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public <T> T getPageBean(Class<T> clazz, String name, boolean create)
	{
		Object obj = pageContext.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				pageContext.setAttribute(name, obj);
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		} 
	
	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	public final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}


}
