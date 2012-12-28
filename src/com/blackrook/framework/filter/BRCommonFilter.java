package com.blackrook.framework.filter;

import java.io.IOException;
import java.io.InputStream;

import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRRootFilter;
import com.blackrook.framework.BRToolkit;

/**
 * The common root filter.
 * @author Matthew Tropiano
 */
public abstract class BRCommonFilter extends BRRootFilter
{
	/** Default Filter Thread Pool. */
	private String filterDefaultThreadPool;
	
	/**
	 * Creates a new filter that uses the default thread pool.
	 */
	protected BRCommonFilter()
	{
		this(BRToolkit.DEFAULT_POOL_NAME);
		}
	
	/**
	 * Creates a new filter that uses the thread pool provided.
	 */
	protected BRCommonFilter(String defaultThreadPoolName)
	{
		super();
		filterDefaultThreadPool = defaultThreadPoolName;
		}
	
	/**
	 * Gets the name of the default thread pool that this filter uses.
	 */
	public String getDefaultThreadPool()
	{
		return filterDefaultThreadPool;
		}

	/**
	 * Sets the name of the default thread pool that this filter uses.
	 */
	public void setDefaultThreadPool(String filterDefaultThreadPool)
	{
		this.filterDefaultThreadPool = filterDefaultThreadPool;
		}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	protected final InputStream getResourceAsStream(String path) throws IOException
	{
		return getToolkit().getResourceAsStream(path);
		}
	
	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a task that can be monitored by the caller.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	protected final BRFrameworkTask spawnTask(BRFrameworkTask task)
	{
		return getToolkit().spawnTaskPooled(filterDefaultThreadPool, task);
		}

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a runnable encapsulated as a 
	 * BRFrameworkTask that can be monitored by the caller.
	 * @param runnable the runnable to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	protected final BRFrameworkTask spawnRunnable(Runnable runnable)
	{
		return getToolkit().spawnRunnablePooled(filterDefaultThreadPool, runnable);
		}

}
