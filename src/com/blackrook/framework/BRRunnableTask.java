package com.blackrook.framework;

/**
 * A task that runs encapsulated runnables.
 * @author Matthew Tropiano
 */
public class BRRunnableTask extends BRFrameworkTask
{
	/** The runnable task to run. */
	private Runnable runnable;
	
	/**
	 * Creates a new runnable task.
	 */
	public BRRunnableTask(Runnable runnable)
	{
		this.runnable = runnable;
		}
	
	@Override
	protected void doTask() throws Throwable
	{
		runnable.run();
		}

}
