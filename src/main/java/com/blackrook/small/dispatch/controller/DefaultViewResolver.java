/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;


/**
 * Default resolver for views.
 * <p>Maps every view to: <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code>
 * @author Matthew Tropiano
 */
public class DefaultViewResolver implements ViewResolver
{
	/** Always returns <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code> for every keyword. */
	@Override
	public String resolveView(String keyword)
	{
		return "/WEB-INF/jsp/" + keyword + ".jsp";
	}

}
