/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.roles;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import com.blackrook.small.annotation.controller.Content;
import com.blackrook.small.annotation.controller.EntryPath;

/**
 * Describes a JSON driver for Small.
 * <p>
 * In order to return Objects from {@link Content}-annotated, {@link EntryPath}-annotated methods, a class 
 * that implements this class needs to be in the component search.
 * <p>
 * Only one component needs to have this role. This is used for conversion if the output content MIME-type
 * is <code>application/json</code> or no MIME-type is specified. 
 * @author Matthew Tropiano
 */
public interface JSONDriver
{
	/**
	 * Called when an object needs to be converted to an object from JSON.
	 * @param reader the provided reader to read JSON from.
	 * @param type the target object type.
	 * @param <T> the return type.
	 * @return the converted object.
	 * @throws IOException if an error occurs during the read.
	 */
	<T> T fromJSON(Reader reader, Class<T> type) throws IOException;

	/**
	 * Called when an object needs to be converted to JSON from an object.
	 * @param writer the writer to write to.
	 * @param object the object to convert and write.
	 * @throws IOException if an error occurs during the write.
	 */
	void toJSON(Writer writer, Object object) throws IOException;
	
	/**
	 * Converts an object to a JSON string from an object.
	 * @param json the input JSON string.
	 * @param type the type to convert to.
	 * @param <T> the type result. 
	 * @return the resultant JSON string.
	 * @throws IOException if an error occurs during the write.
	 */
	default <T> T fromJSONString(String json, Class<T> type) throws IOException
	{
		return fromJSON(new StringReader(json), type);
	}
	
	/**
	 * Converts an object to a JSON string from an object.
	 * @param object the object to convert and write.
	 * @return the resultant JSON string.
	 * @throws IOException if an error occurs during the write.
	 */
	default String toJSONString(Object object) throws IOException
	{
		StringWriter sw = new StringWriter(2048);
		toJSON(sw, object);
		return sw.toString();
	}
	
}
