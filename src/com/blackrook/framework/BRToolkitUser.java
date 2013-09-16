package com.blackrook.framework;

/**
 * Classes that extend this class are aware of {@link BRToolkit}.
 * @author Matthew Tropiano
 */
public class BRToolkitUser
{
	/**
	 * Gets the Black Rook Framework Toolkit.
	 */
	protected final BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}
	
}
