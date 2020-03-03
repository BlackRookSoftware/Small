/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
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
	String SMALL_APPLICATION_ENVIRONMENT_ARTTRIBUTE = "small.application.environment.class";

	String INIT_PARAM_APPLICATION_PACKAGE_ROOTS = "small.application.package.root";
	String INIT_PARAM_APPLICATION_JSON_DRIVER = "small.application.driver.json.class";
	String INIT_PARAM_APPLICATION_XML_HANDLER_CLASS_PREFIX = "small.application.xml.handler.class.";
	String INIT_PARAM_APPLICATION_XML_HANDLER_DRIVER_PREFIX = "small.application.xml.handler.driver.";
}
