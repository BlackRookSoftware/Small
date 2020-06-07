package com.blackrook.small.roles;

/**
 * Describes a MIME-Type driver for Small.
 * A component with this role is used for mapping file extensions to MIME-Types.
 * Small supplies a default implementation of this if a component is not found.
 * @author Matthew Tropiano
 */
public interface MIMETypeDriver
{
	/**
	 * Get the corresponding MIME-Type for a file extension.
	 * Must resolve case-insensitively.
	 * @param extension the file extension to look up (extension only, no name separator characters).
	 * @return the corresponding type or null if no type found.
	 */
	String getMIMEType(String extension);
}
