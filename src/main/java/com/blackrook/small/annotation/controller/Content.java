package com.blackrook.small.annotation.controller;

import java.io.File;
import java.io.Reader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

import com.blackrook.small.enums.RequestMethod;

/**
 * Annotates a parameter or method. Should be used on Controllers.
 * <p>
 * On parameters, the request body content is passed in as the following: 
 * <ul>
 * <li>
 * If the content type is <code>text/plain</code>, and the 
 * parameter type is a primitive value, boxed primitive value, primitive/boxed array, char[], byte[], 
 * or String, the content is parsed and cast to the appropriate type, if possible.
 * </li>
 * <li>If the content type is any of the <code>application/xml</code> equivalents, a SAX Reader is made for the XML.</li>
 * <li>If the content type is <code>application/json</code> and a JSONDriver is specified, it is parsed as JSON and converted to the parameter type.</li>
 * <li>Otherwise, it is read as-is and will be converted to an appropriate type.</li>
 * </ul>
 * Note that its use on a parameter is worthless if the request method is not {@link RequestMethod#POST} or {@link RequestMethod#PUT}.
 * <p>
 * If on a method, this turns the returned object after running the method into data that is put into the response body.
 * <ul>
 * <li>If return type is a {@link File}, 
 * 		<ul>
 * 			<li>...the content type is changed to the file's predicted MIME-type and the content is the file's content, verbatim. Unknown type is <code>application/octet-stream</code>.</li> 
 * 			<li>...and is null, this sends a 404.</li>
 * 		</ul>
 * </li>
 * <li>If return type is a {@link Reader}, {@link CharSequence}, {@link String}, {@link StringBuilder}, or {@link StringBuffer}, plain text is sent back. Content type is <code>text/plain</code>.</li>
 * <li>If return type is byte[] or {@link ByteBuffer} binary data is sent back. Content type is <code>application/octet-stream</code>.</li>
 * <li>If return type is Object and a JSONDriver is specified, content type is <code>application/json</code> and the object is converted.</li>
 * </ul>
 * If a String value is given on this annotation, it is interpreted as the forced MIME-Type to use, but only for File, String and binary output.
 * 
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see Attachment
 * @see View
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Content
{
	/** @return the forced MIME-Type. */
	String value() default "";
}
