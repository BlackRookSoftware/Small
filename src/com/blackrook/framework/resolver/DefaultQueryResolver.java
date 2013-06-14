package com.blackrook.framework.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.blackrook.commons.Common;
import com.blackrook.framework.BRFrameworkException;
import com.blackrook.framework.BRQueryResolver;
import com.blackrook.framework.BRToolkit;

/**
 * Default query resolver. 
 * Always returns the contents of files read from <code>"/WEB-INF/sql/" + keyword + ".sql"</code> for every keyword.
 * @author Matthew Tropiano
 */
public class DefaultQueryResolver implements BRQueryResolver
{

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
		File f = BRToolkit.INSTANCE.getApplicationFile(src); 
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
