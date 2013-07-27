package com.blackrook.framework.multipart;

import java.io.File;

/**
 * Multipart part that is part of a multiform request.
 * @author Matthew Tropiano
 */
public class Part
{
	/** Part name. */
	String name;
	/** Part filename, if file. */
	String fileName;
	/** Part file handle. */
	File file;
	/** Part content type. */
	String contentType;
	/** Part value. */
	String value;

	
	Part() {}
	
	/**
	 * Gets the name of this part (parameter name).
	 */
	public String getName()
	{
		return name;
		}

	/**
	 * Returns if this part refers to a file.
	 * @return true if so, false if not.
	 */
	public boolean isFile()
	{
		return fileName != null;
		}

	/**
	 * Returns the original file name of the file, or null if not a file.
	 * @see #isFile()
	 */
	public String getFileName()
	{
		return fileName;
		}

	/**
	 * Returns a handle to this file, or null if not a file.
	 * @see #isFile()
	 */
	public File getFile()
	{
		return file;
		}

	/**
	 * Returns the content type of the uploaded file, or null if not a file.
	 * @see #isFile()
	 */
	public String getContentType()
	{
		return contentType;
		}

	/**
	 * This part's value, if it is not a file.
	 */
	public String getValue()
	{
		return value;
		}

}
