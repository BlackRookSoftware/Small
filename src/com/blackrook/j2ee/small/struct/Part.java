package com.blackrook.j2ee.small.struct;

import java.io.File;

import com.blackrook.json.annotation.JSONIgnore;

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
	@JSONIgnore
	public File getFile()
	{
		return file;
	}

	/**
	 * Returns the length of this part.
	 * If file, this is the file length in bytes. If value, this is the value length in characters.
	 */
	public long getLength()
	{
		return isFile() ? file.length() : value.length();
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
