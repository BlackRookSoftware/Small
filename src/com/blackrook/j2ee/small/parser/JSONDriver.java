package com.blackrook.j2ee.small.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.blackrook.j2ee.small.annotation.controller.ControllerEntry;

/**
 * The JSON driver for Small.
 * <p>In order to return Objects from {@link ControllerEntry}-annotated methods, a class 
 * that implements this class needs to be set on initialize. 
 * @author Matthew Tropiano
 */
public interface JSONDriver
{
	/**
	 * Called when an object needs to be converted to an object from JSON.
	 * @param reader the provided reader to read JSON from.
	 * @param type the target object type.
	 * @return the converted object.
	 * @throws IOException if an error occurs during the read.
	 */
	Object fromJSON(Reader reader, Class<?> type) throws IOException;

	/**
	 * Called when an object needs to be converted to JSON from an object.
	 * @param writer the writer to write to.
	 * @param object the object to convert and write.
	 * @throws IOException if an error occurs during the write.
	 */
	void toJSON(Writer writer, Object object) throws IOException;
	
}
