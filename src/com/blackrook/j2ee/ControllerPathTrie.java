package com.blackrook.j2ee;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.Sizable;
import com.blackrook.commons.hash.HashMap;

/**
 * An abstract of a Trie - a directed graph used for searching for values
 * using a starting sequence or a partial key.
 * @author Matthew Tropiano
 * @param <S> the path segment class on each edge, created by the input key. 
 * @param <K> the object key type.
 * @param <V> the value type corresponding to keys.
 * TODO: Finish.
 */
public abstract class ControllerPathTrie<S extends Object, K extends Object, V extends Object> implements Sizable
{
	/** Root Node. */
	private Node<S, V> root;
	/** Current size. */
	private int size;

	/**
	 * Creates a new Trie.
	 */
	public ControllerPathTrie()
	{
		root = new Node<S, V>();
		size = 0;
	}
	
	/**
	 * Creates the segments necessary to find/store keys and values.
	 * @param key the key to generate significant segments for.
	 * @return the list of segments for the key.
	 */
	protected abstract S[] getSegments(K key);
	
	/**
	 * Returns a value for the key provided.
	 * @param key the key.
	 * @return the corresponding value, or null if there is no value associated with that key.
	 */
	public V get(K key)
	{
		//TODO: Finish.
		return null;
	}

	/**
	 * Adds a value to this trie with a particular key.
	 * If the association exists, the value is replaced.
	 * @param key the key to use.
	 * @param value the corresponding value.
	 */
	public void put(K key, V value)
	{
		//TODO: Finish.
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
	 * A single node in the Trie.
	 */
	protected static class Node<S, V>
	{
		/** Edge map. */
		private AbstractMap<S, Node<S, V>> edgeMap;
		/** Value stored at this node. Can be null. */
		private V value;
		
		protected Node()
		{
			edgeMap = new HashMap<S, Node<S, V>>(2, 1f);
			value = null;
		}
		
		/**
		 * Returns the edge map.
		 */
		public AbstractMap<S, Node<S, V>> getEdgeMap()
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
		
	}

}
