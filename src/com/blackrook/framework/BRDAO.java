package com.blackrook.framework;

import com.blackrook.framework.BRToolkit;

/**
 * Data access object for submitting database queries.
 * @author Matthew Tropiano
 */
public abstract class BRDAO
{
	/**
	 * Base constructor.
	 */
	protected BRDAO()
	{
		}

	/**
	 * Gets the Black Rook Framework Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}

}
