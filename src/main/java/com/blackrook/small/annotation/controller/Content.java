/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.controller;

import java.io.File;
import java.io.Reader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.XMLDriver;

/**
 * Annotates a parameter or method. Should be used on Controllers.
 * <p>
 * On <b>parameters</b>, the request body content is passed in as the following: 
 * <ul>
 * 		<li>If the content type is <code>text/plain</code>, 
 * 			<ul>
 * 				<li>
 * 					...and the parameter type is a primitive value, boxed primitive value, 
 * 					primitive/boxed array, char[], byte[], or String, the content is parsed 
 * 					and cast to the appropriate type, if possible.
 * 				</li>
 * 			</ul>
 * 		</li>
 * 		<li>If the content type is any of the <code>application/xml</code> equivalents and a {@link XMLDriver} component is found, the driver is used to convert to the target type.</li>
 * 		<li>If the content type is <code>application/json</code> and a {@link JSONDriver} is found, the driver is used to convert to the target type.</li>
 * 		<li>Otherwise, an error is thrown.</li>
 * </ul>
 * Note that its use on a parameter is worthless if the request method is not {@link RequestMethod#POST}, {@link RequestMethod#PUT}, or {@link RequestMethod#PATCH}.
 * <p>
 * If on a <b>method</b>, this converts the returned object after running the method into data that is put into the response body.
 * <ul>
 * <li>If return type is a {@link File}, 
 * 		<ul>
 * 			<li>...the content type is changed to the file's predicted MIME-type and the content is the file's content, verbatim. Unknown type is <code>application/octet-stream</code>.</li> 
 * 			<li>...and is null, this sends a 404.</li>
 * 		</ul>
 * </li>
 * <li>If return type is a {@link Reader}, {@link CharSequence}, {@link String}, {@link StringBuilder}, or {@link StringBuffer}, plain text is sent back. Content type is <code>text/plain</code> if unspecified.</li>
 * <li>If return type is byte[] or {@link ByteBuffer} binary data is sent back. Content type is <code>application/octet-stream</code> if unspecified.</li>
 * <li>If return type is any other object type,
 * 		<ul>
 * 			<li>...and a {@link XMLDriver} component is found, and the specified content type is <code>application/xml</code> or an XML subtype, the object is converted to XML.</li>
 * 			<li>...and a {@link JSONDriver} component is found, content type is <code>application/json</code> and the object is converted.</li>
 * 		</ul>
 * </li>
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
