package com.blackrook.framework.tasks;

import com.blackrook.framework.BRFrameworkTask;

/**
 * A task that runs an encapsulated runnable.
 * Cannot track progress, just if it is done or not.
 * The {@link #getProgress(float)} and {@link #getProgressMax(float)} methods will both
 * return 0f in this class.
 * @author Matthew Tropiano
 */
public class BREncapsulatedTask extends BRFrameworkTask
{
	/** The encapsulated runnable. */
	protected Runnable runnable;

	/**
	 * Creates a new encapsulated task.
	 */
	public BREncapsulatedTask(Runnable runnable)
	{
		if (runnable == null)
			throw new IllegalArgumentException("You cannot encapsulate a null runnable.");
		this.runnable = runnable;
		}
	
	@Override
	public void doTask() throws Exception
	{
		runnable.run();
		}

}
