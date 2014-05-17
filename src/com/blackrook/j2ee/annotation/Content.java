package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * <li>If the content type is any of the <code>application/xml</code> equivalents, it is parsed as XML and passed in as an XMLStruct.</li>
 * <li>If the content type is <code>application/json</code>, it is parsed as JSON and converted to the parameter type.</li>
 * </ul>
 * <p>
 * If on a method, this turns the returned object after running the method into data that is put into the response body.
 * <ul>
 * <li>If return type is a File, the content type is changed to the file's predicted MIME-type and the content is the file's content, verbatim. IF the file is null, this sends a 404.</li>
 * <li>If return type is an XMLStruct, XML is sent back. Content type is application/xml.</li>
 * <li>If return type is a String, plain text is sent back. Content type is text/plain.</li>
 * <li>If return type is byte[], binary data is sent back. Content type is application/octet-stream.</li>
 * <li>If return type is JSONObject or anything else, it is converted to JSON and sent back. Content type is application/json.</li>
 * </ul>
 * @author Matthew Tropiano
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Content
{
}
