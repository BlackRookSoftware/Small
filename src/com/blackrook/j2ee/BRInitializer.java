package com.blackrook.j2ee;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class BRInitializer implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		BRToolkit.createToolkit(sce.getServletContext());
		}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		}

}
