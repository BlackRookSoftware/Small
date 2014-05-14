package com.blackrook.j2ee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ToolkitComponent
{
	/**
	 * Gets a file that is on the application path. 
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	protected final File getApplicationFile(String path)
	{
		return Toolkit.INSTANCE.getApplicationFile(path);
	}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	protected final String getApplicationFilePath(String relativepath)
	{
		return Toolkit.INSTANCE.getApplicationFilePath(relativepath);
	}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 * @throws IOException if the stream cannot be opened.
	 */
	protected final InputStream getResourceAsStream(String path) throws IOException
	{
		return Toolkit.INSTANCE.getResourceAsStream(path);
	}

}
