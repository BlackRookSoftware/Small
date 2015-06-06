package com.blackrook.j2ee.small.annotation;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Annotates a method. Should be used on Controllers.
 * <p>
 * This turns the method return body content into an "attachment" via <code>Content-Disposition</code> headers (file downloads).
 * <p>Most effective on {@link File} return types.
 * <ul>
 * <li>If return type is a {@link File}, the content type is changed to the file's predicted MIME-type, the content is the file's content, verbatim, and the output filename is the file's name. If the file is null, this sends a 404.</li>
 * <li>If return type is an {@link XMLStruct}, XML is sent back. Content type is <code>application/xml</code>, and the output filename is the page name.</li>
 * <li>If return type is a {@link String}, {@link StringBuilder}, or {@link StringBuffer}, plain text is sent back. Content type is <code>text/plain</code>.</li>
 * <li>If return type is byte[], binary data is sent back. Content type is <code>application/octet-stream</code>, and the output filename is the page name.</li>
 * <li>If return type is {@link JSONObject}, {@link Object}, or anything else, it is converted to JSON and sent back. Content type is <code>application/json</code>, and the output filename is the page name.</li>
 * </ul>
 * If a String value is given on this annotation, it is interpreted as the forced MIME-Type to use, but only for File, String and binary output.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see Content
 * @see View
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attachment
{
	/** 
	 * Forced MIME-Type.
	 * @return MIME-Type name. Default is empty string. 
	 */
	String value() default "";
}
