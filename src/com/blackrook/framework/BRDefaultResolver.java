package com.blackrook.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.blackrook.commons.Common;
import com.blackrook.framework.BRFrameworkException;
import com.blackrook.framework.BRQueryResolver;
import com.blackrook.framework.BRViewResolver;

/**
 * Default resolver for all things. 
 * @author Matthew Tropiano
 */
public class BRDefaultResolver implements BRQueryResolver, BRViewResolver
{
	private BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}
	
	/** Always returns false. */
	@Override
	public boolean dontCacheView(String keyword)
	{
		return false;
		}

	/** Always returns <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code> for every keyword. */
	@Override
	public String resolveView(String keyword)
	{
		return "/WEB-INF/jsp/" + keyword + ".jsp";
		}

	@Override
	public boolean dontCacheQuery(String keyword)
	{
		return false;
		}

	@Override
	public String resolveQuery(String keyword)
	{
		StringBuffer sb = new StringBuffer();
		String src = "/WEB-INF/sql/" + keyword + ".sql";
		File f = getToolkit().getApplicationFile(src); 
		FileInputStream in = null;
		BufferedReader br = null;
		try {
			in = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null)
			{
				// cull unnecessary lines reasonably.
				String tline = line.trim();
				if (tline.length() == 0) // blank lines
					continue;
				if (tline.startsWith("--")) // line comments
					continue;
				
				sb.append(line).append('\n');
				}
		} catch (Exception e) {
			throw new BRFrameworkException("Could not read query contents from file: " + f.getAbsolutePath(), e);
		} finally {
			Common.close(br);
			Common.close(in);
			}
		
		return sb.toString();
		}

}
