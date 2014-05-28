package com.blackrook.j2ee.small;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.blackrook.commons.Common;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;

/**
 * Default resolver for queries.
 * <p>This resolver hints that all queries should be cached, and the keyword resolves to:
 * <p><code>"/WEB-INF/sql/" + keyword + ".sql"</code>
 * <p>on the application path.
 * @author Matthew Tropiano
 */
public class DefaultQueryResolver implements QueryResolver
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
		File f = SmallToolkit.INSTANCE.getApplicationFile(src); 
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
			throw new SmallFrameworkException("Could not read query contents from file: " + f.getAbsolutePath(), e);
		} finally {
			Common.close(br);
			Common.close(in);
		}
		
		return sb.toString();
	}

}
