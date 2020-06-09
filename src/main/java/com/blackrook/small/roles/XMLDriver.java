/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
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
import com.blackrook.small.annotation.controller.ControllerEntry;

/**
 * Describes an XML conversion driver for Small.
 * <p>In order to return Objects from {@link Content}-annotated, {@link ControllerEntry}-annotated methods, a class 
 * that implements this class needs to be in the component search.
 * <p>Only one component needs to have this role. This is used for conversion if the output content MIME-type
 * is <code>application/xml</code> (and its family types). Unspecified output types are passed to a {@link JSONDriver}. 
 * @author Matthew Tropiano
 */
public interface XMLDriver
{
	/**
	 * Called when an object needs to be converted to XML from an object.
	 * @param writer the writer to write to.
	 * @param object the object to convert and write.
	 * @throws IOException if an error occurs during the write.
	 */
	void toXML(Writer writer, Object object) throws IOException;

	/**
	 * Called when an object needs to be converted to an object from XML.
	 * @param <T> the return type.
	 * @param reader the provided reader to read XML from.
	 * @param type the incoming type.
	 * @return the converted object.
	 * @throws IOException if an error occurs during the read.
	 */
	<T> T fromXML(Reader reader, Class<T> type) throws IOException;

	/**
	 * Converts an object to a XML string from an object.
	 * @param retval the object to convert and write.
	 * @return the resultant XML string.
	 * @throws IOException if an error occurs during the write.
	 */
	default String toXMLString(Object retval) throws IOException
	{
		StringWriter sw = new StringWriter(2048);
		toXML(sw, retval);
		return sw.toString();
	}
	
	/**
	 * Converts an object to a XML string from an object.
	 * @param <T> the return type.
	 * @param xml the input XML string.
	 * @param type the incoming type.
	 * @return the resultant XML string.
	 * @throws IOException if an error occurs during the write.
	 */
	default <T> T fromXMLString(String xml, Class<T> type) throws IOException
	{
		return fromXML(new StringReader(xml), type);
	}
	
}
