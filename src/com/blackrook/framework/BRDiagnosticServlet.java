package com.blackrook.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.framework.BRToolkit;
import com.blackrook.sync.ThreadPool;

/**
 * A servlet that just reports on the current running characteristics of the
 * Black Rook Simple Framework.
 * @author Matthew Tropiano
 */
public final class BRDiagnosticServlet extends BRRootServlet
{
	private static final long serialVersionUID = -183745067361189440L;

	private String viewKey;
	
	/**
	 * Creates a new diagnostic servlet instance that must take the name
	 * of the view to use after it is invoked.
	 * @param viewKey the view key to call.
	 */
	public BRDiagnosticServlet(String viewKey)
	{
		this.viewKey = viewKey;
		}
	
	@Override
	public void onGet(HttpServletRequest request, HttpServletResponse response)
	{
		processRequest(request, response);
		}
	
	@Override
	public void onPost(HttpServletRequest request, HttpServletResponse response)
	{
		processRequest(request, response);
		}

	/**
	 * Processes the request - fills the request with diagnostic info.
	 * @param request the servlet request.
	 * @param response the servlet response.
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit toolkit = getToolkit();

		HashMap<String, Object> map = null;
		
		map = new HashMap<String, Object>();
		for (String key : toolkit.getViewNames())
			map.put(key, toolkit.getViewByName(key));
		request.setAttribute("queries", map);

		map = new HashMap<String, Object>();
		for (String key : toolkit.getThreadPoolNames())
		{
			ThreadPool<BRFrameworkTask> pool = toolkit.getThreadPool(key);
			HashMap<String, Object> stats = new HashMap<String, Object>();
			
			stats.put("running", pool.getRunningCount());
			stats.put("waiting", pool.getWaitingCount());
			stats.put("total", pool.getRunningCount() + pool.getWaitingCount());
			
			map.put(key, toolkit.getViewByName(key));
			}
		request.setAttribute("threadpools", map);

		// TODO: Finish.
		
		sendToView(request, response, viewKey);
		}

	@Override
	public void onMultiformPost(HttpServletRequest request, HttpServletResponse response, FileItem[] fileItems, HashMap<String, String> paramMap)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onHead(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPut(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onDelete(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}
	
}
