/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.enums;

/**
 * Attribute scope type.
 * @author Matthew Tropiano
 */
public enum ScopeType
{
	/** Request scope. */
	REQUEST,
	/** Session scope. */
	SESSION,
	/** Application (servlet context) scope. */
	APPLICATION;
}
