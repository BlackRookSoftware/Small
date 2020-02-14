/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.controller;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method. Should be used on Controllers.
 * <p>
 * This turns the method return body content into an "attachment" via <code>Content-Disposition</code> headers (file downloads).
 * <p>Most effective on {@link File} return types.
 * <ul>
 * <li>If return type is a {@link File}, the content type is changed to the file's predicted MIME-type, the content is the file's content, verbatim, and the output filename is the file's name. If the file is null, this sends a 404.</li>
 * <li>If return type is a {@link String}, {@link StringBuilder}, or {@link StringBuffer}, plain text is sent back. Content type is <code>text/plain</code>.</li>
 * <li>If return type is byte[], binary data is sent back. Content type is <code>application/octet-stream</code>, and the output filename is the page name.</li>
 * <li>If return type is {@link Object}, an Object Converter is used for converting.</li>
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
