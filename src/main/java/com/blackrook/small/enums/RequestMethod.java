/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.enums;

/**
 * Enumeration of request methods for controller method invocation. 
 * @author Matthew Tropiano
 */
public enum RequestMethod
{
	/** Standard GET request. */
	GET,
	/** Standard POST request. */
	POST,
	/** Standard PUT request. */
	PUT,
	/** Standard PATCH request. */
	PATCH,
	/** Standard DELETE request. */
	DELETE,
	;
}
