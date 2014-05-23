package com.blackrook.j2ee;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.Sizable;
import com.blackrook.commons.hash.HashMap;

/**
 * A trie that maps paths to descriptors.
 * @author Matthew Tropiano
 * @param <V> the value type corresponding to keys.
 */
public class PathTrie<V extends Object> implements Sizable
{
	/** Root Node. */
	private Node<V> root;
	/** Current size. */
	private int size;

	/**
	 * Creates a new trie.
	 */
	public PathTrie()
	{
		root = new Node<V>();
		size = 0;
	}

	/**
	 * Creates the segments necessary to find/store keys and values.
	 * @param key the key to generate significant segments for.
	 * @return the list of segments for the key.
	 */
	protected String[] getSegments(String key)
	{
		key = key.startsWith("/") ? key.substring(1) : key;
		key = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
		return key.split("(\\/)+");
	}
	
	/**
	 * Returns a value for the key provided.
	 * @param key the key.
	 * @return the corresponding value, or null if there is no value associated with that key.
	 */
	public V get(String key)
	{
		Result<V> out = getPartial(key);
		return out.offset == key.length() ? getPartial(key).value : null;
	}

	/**
	 * Returns a value for the key provided, but returns the last eligible object in the path.
	 * The remainder is returned in the {@link Result} object as an offset into the key.
	 * Remainders equaling the length of the key means that the search reached the end of the key.
	 * @param key the key.
	 * @return a trie {@link Result}. The value can be null.
	 */
	public Result<V> getPartial(String key)
	{
		String[] segments = getSegments(key);
		int segindex = 0;
		int offset = 0;
		
		Node<V> current = root;
		V lastEligible = null;
		int lastOffset = 0;
		while (segindex < segments.length && current != null && current.hasEdges())
		{
			offset += segments[segindex].length() + 1;
			current = current.edgeMap.get(segments[segindex]);
			if (current != null && current.value != null)
			{
				lastEligible = current.value;
				lastOffset = offset + 1;
			}
			segindex++;
		}
		
		if (lastEligible == null)
			return new Result<V>(null, Math.min(offset, key.length()));
		return new Result<V>(lastEligible, Math.min(lastOffset, key.length()));
	}

	/**
	 * Adds a value to this trie with a particular key.
	 * If the association exists, the value is replaced.
	 * @param key the key to use.
	 * @param value the corresponding value.
	 * @throws IllegalArgumentException if value is null.
	 */
	public void put(String key, V value)
	{
		if (value == null)
			throw new IllegalArgumentException("Value cannot be null.");
		
		String[] segments = getSegments(key);
		int segindex = 0;
		
		Node<V> current = root;
		Node<V> next = null;
		while (segindex < segments.length)
		{
			if ((next = current.edgeMap.get(segments[segindex])) == null)
			{
				next = new Node<V>();
				current.edgeMap.put(segments[segindex], next);
			}
			current = next;
			segindex++;
		}
		
		V prevval = current.value;
		current.value = value;
		if (prevval == null)
			size++;
	}
	
	@Override
	public int size()
	{
		return size;
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	/**
	 * A result of a passive search on a trie.
	 */
	public static class Result<V>
	{
		private V value;
		private int offset;
		
		Result(V value, int offset)
		{
			this.value = value;
			this.offset = offset;
		}
		
		public V getValue() 
		{
			return value;
		}
		
		public int getOffset() 
		{
			return offset;
		}
		
		@Override
		public String toString()
		{
			return value +":"+ offset;
		}
	}
	
	/**
	 * A single node in the Trie.
	 */
	protected static class Node<V>
	{
		/** Edge map. */
		private AbstractMap<String, Node<V>> edgeMap;
		/** Value stored at this node. Can be null. */
		private V value;
		
		protected Node()
		{
			edgeMap = new HashMap<String, Node<V>>(2, 1f);
			value = null;
		}
		
		/**
		 * Returns the edge map.
		 */
		public AbstractMap<String, Node<V>> getEdgeMap()
		{
			return edgeMap;
		}
		
		/**
		 * Gets the value on this node. Can be null.
		 */
		public V getValue()
		{
			return value;
		}
		
		/**
		 * Sets the value on this node.
		 */
		public void setValue(V value)
		{
			this.value = value;
		}
		
		/**
		 * Returns if this node can be cleaned up.
		 */
		public boolean isExpired()
		{
			return edgeMap.isEmpty() && value == null;
		}
		
		/**
		 * Returns if the node has possible paths.
		 */
		public boolean hasEdges()
		{
			return !edgeMap.isEmpty();
		}
		
	}

}
