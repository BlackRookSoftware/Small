package com.blackrook.j2ee;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes the framework on startup.
 * Created via servlet container listener.
 * @author Matthew Tropiano
 */
public class Initializer implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		Toolkit.create(sce.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
	}

}
