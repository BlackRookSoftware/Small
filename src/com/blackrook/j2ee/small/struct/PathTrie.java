package com.blackrook.j2ee.small.struct;

import com.blackrook.commons.AbstractTrieMap;

/**
 * A trie that maps paths to descriptors.
 * @author Matthew Tropiano
 * @param <V> the value type corresponding to keys.
 */
public class PathTrie<V extends Object> extends AbstractTrieMap<String, V, String>
{
	private static final String[] NO_STRINGS = new String[0];
	
	/**
	 * Creates a new path trie.
	 */
	public PathTrie()
	{
		super();
	}

	/**
	 * Creates the segments necessary to find/store keys and values.
	 * @param key the key to generate significant segments for.
	 * @return the list of segments for the key.
	 */
	protected String[] getSegmentsForKey(String key)
	{
		key = key.startsWith("/") ? key.substring(1) : key;
		key = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
		if (key.length() == 0)
			return NO_STRINGS;
		return key.split("(\\/)+");
	}
	
}
