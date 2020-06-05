/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * A class that reads an incoming XML document and returns a deserialized object.
 * There is one driver for each handled class.
 * @author Matthew Tropiano
 */
public interface XMLDriver
{
	/**
	 * Called when an object needs to be converted to an object from XML.
	 * @param reader the provided reader to read XML from.
	 * @return the converted object.
	 * @throws IOException if an error occurs during the read.
	 */
	Object fromXML(Reader reader) throws IOException;

	/**
	 * Called when an object needs to be converted to XML from an object.
	 * @param writer the writer to write to.
	 * @param object the object to convert and write.
	 * @throws IOException if an error occurs during the write.
	 */
	void toXML(Writer writer, Object object) throws IOException;

	/**
	 * Converts an object to a XML string from an object.
	 * @param json the input XML string.
	 * @return the resultant XML string.
	 * @throws IOException if an error occurs during the write.
	 */
	default Object fromXMLString(String json) throws IOException
	{
		return fromXML(new StringReader(json));
	}
	
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
	
}
