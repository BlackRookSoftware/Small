/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
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
