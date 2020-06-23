/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.multipart;

import java.io.File;

/**
 * Multipart part that is part of a multiform request.
 * @author Matthew Tropiano
 */
public class Part
{
	/** Part name. */
	private String name;
	/** Part filename, if file. */
	private String fileName;
	/** Part file handle. */
	private File file;
	/** Part content type. */
	private String contentType;
	/** Part value. */
	private String value;

	public Part() {}
	
	/**
	 * @return the name of this part (parameter name).
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
	 * @return the original file name of the file, or null if not a file.
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
	 * @return the file on this part, or null if not a file.
	 * @see #isFile()
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Returns the length of this part.
	 * If file, this is the file length in bytes. If value, this is the value length in characters.
	 * @return the length.
	 */
	public long getLength()
	{
		return isFile() ? file.length() : value.length();
	}
	
	/**
	 * @return the content type of the uploaded file, or null if not a file.
	 * @see #isFile()
	 */
	public String getContentType()
	{
		return contentType;
	}

	/**
	 * @return this part's value, if it is not a file.
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * Sets the name of this Part.
	 * @param name the name.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Sets the file name of this Part.
	 * @param fileName the file name.
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Sets the file on this part.
	 * @param file the file descriptor.
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

	/**
	 * Sets the content type on this part.
	 * @param contentType the content type (MIME type)
	 */
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	/**
	 * Sets the value of this part, if not a file.
	 * @param value the parameter value.
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

}
