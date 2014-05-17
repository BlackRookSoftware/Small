package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method. Should be used on Controllers.
 * <p>
 * This turns the method return body content into an "attachment" via <code>Content-Disposition</code> headers.
 * Most effective on File return types.
 * <ul>
 * <li>If return type is a File, the content type is changed to the file's predicted MIME-type and the content is the file's content, verbatim. If the file is null, this sends a 404.</li>
 * <li>If return type is an XMLStruct, XML is sent back. Content type is application/xml, and the output filename is the page name.</li>
 * <li>If return type is a String, plain text is sent back. Content type is text/plain, and the output filename is the page name.</li>
 * <li>If return type is byte[], binary data is sent back. Content type is application/octet-stream, and the output filename is the page name.</li>
 * <li>If return type is JSONObject or anything else, it is converted to JSON and sent back. Content type is application/json, and the output filename is the page name.</li>
 * </ul>
 * @author Matthew Tropiano
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attachment
{
}
