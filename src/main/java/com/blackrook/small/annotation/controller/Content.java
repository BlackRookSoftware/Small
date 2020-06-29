/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

import javax.servlet.ServletInputStream;

import com.blackrook.small.SmallModelView;
import com.blackrook.small.SmallResponse;
import com.blackrook.small.annotation.Controller;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.XMLDriver;

/**
 * Should be used on {@link EntryPath}-annotated methods on {@link Controller}-annotated classes.
 * <p>
 * On <b>parameters</b>, the request body content is passed in as the following: 
 * <ul>
 * 		<li>
 * 			If the content type is any of the <code>application/xml</code> equivalents 
 * 			and a {@link XMLDriver} component is found, the driver is used to convert to the target type.</li>
 * 		<li>
 * 			If the content type is <code>application/json</code> and a {@link JSONDriver} 
 * 			is found, the driver is used to convert to the target type.</li>
 * 		<li>Else, if the content is anything else, the following types are valid as a target type:
 * 			<ul>
 * 				<li>All primitives (if input is parseable text).</li>
 * 				<li>All boxed primitives (if input is parseable text).</li>
 * 				<li>byte[], {@link ByteArrayInputStream}, {@link ServletInputStream}, {@link InputStream} (for binary).</li>
 * 				<li>{@link String}, {@link StringReader}, {@link BufferedReader}, {@link InputStreamReader}, {@link Reader} (for characters).</li>
 * 			</ul>
 * 		<li>Otherwise, an error is thrown.</li>
 * </ul>
 * Note that its use on a parameter is worthless if the request method is not {@link RequestMethod#POST}, {@link RequestMethod#PUT}, or {@link RequestMethod#PATCH}.
 * <p>
 * If on a <b>method</b>, this converts the returned object after running the method into data that is put into the response body.
 * <ul>
 * <li>If return type is a {@link SmallResponse}, its content is the object to convert (other headers and statuses are set on the response), and conversion continues below for the content...</li>
 * <li>If return type is a {@link SmallModelView}, the view is resolved and written to the response.</li>
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
 * @see EntryPath
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
