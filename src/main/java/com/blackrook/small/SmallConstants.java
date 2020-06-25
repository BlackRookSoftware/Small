/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

/**
 * Small Framework Constants.
 * @author Matthew Tropiano
 */
public interface SmallConstants
{
	/** The attribute name for the application environment on the servlet context. */
	String SMALL_APPLICATION_ENVIRONMENT_ATTRIBUTE = "small.application.environment";
	/** The attribute name for the application configuration object on the servlet context. */
	String SMALL_APPLICATION_CONFIGURATION_ATTRIBUTE = "small.application.configuration";
	/** The attribute name for the path remainder that gets set on the request context on each request (default paths only). */
	String SMALL_REQUEST_ATTRIBUTE_PATH_REMAINDER = "small.request.path.remainder";
}
