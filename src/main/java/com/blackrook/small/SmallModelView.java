/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.controller.View;

/**
 * A view model-and-view container.
 * Can be returned on {@link View}-annotated {@link Controller} methods. 
 * @author Matthew Tropiano
 */
public class SmallModelView
{
	private Object model;
	private String viewName;
	
	private SmallModelView()
	{
		this.model = null;
		this.viewName = null;
	}
	
	/**
	 * Creates a new model-view container. No model.
	 * @param viewName the name of the view to resolve and use.
	 * @return a new {@link SmallModelView}.
	 * @since [NOW]
	 */
	public static SmallModelView create(String viewName)
	{
		return create(null, viewName);
	}
	
	/**
	 * Creates a new model-view container.
	 * @param model the model to render.
	 * @param viewName the name of the view to resolve and use.
	 * @return a new {@link SmallModelView}.
	 */
	public static SmallModelView create(Object model, String viewName)
	{
		SmallModelView out = new SmallModelView();
		out.model = model;
		out.viewName = viewName;
		return out;
	}
	
	/**
	 * @return the view model.
	 */
	public Object getModel()
	{
		return model;
	}
	
	/**
	 * @return the view name.
	 */
	public String getViewName()
	{
		return viewName;
	}
}
