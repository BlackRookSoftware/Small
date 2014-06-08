package com.blackrook.j2ee.small.struct;

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

	public Part() {}
	
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
	 * <p>
	 * <b>NOTE: You cannot guarantee that this file handle will be valid after the 
	 * handled request, as it may be deleted after the controller processes the POST!</b>
	 * <p>
	 * If you need to keep this file around, it should be copied to another file!
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

	/**
	 * Sets the name of this Part.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Sets the file name of this Part.
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Sets the file on this part.
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

	/**
	 * Sets the content type on this part.
	 */
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	/**
	 * Sets the value of this part, if not a file.
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

}
