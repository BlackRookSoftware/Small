package com.blackrook.j2ee.small.struct;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.SortedList;
import com.blackrook.j2ee.small.SmallUtil;
import com.blackrook.j2ee.small.descriptor.ControllerEntryPoint;

/**
 * A trie that organizes mapping URI patterns to ControllerHandlers.
 * @author Matthew Tropiano
 */
public class URITrie
{
	// TODO: Finish

	Node root; 

	public URITrie()
	{
		this.root = Node.createRoot();
	}
	
	
	/**
	 * Attempts to resolve an endpoint for a given URI.
	 * @param uri the input URI.
	 * @return a result object detailing the search.
	 */
	public Result resolve(String uri)
	{
		// TODO: Finish.
		Node next = root;
		Node lastMatch = null;
		
		Result out = new Result();
		
		uri = SmallUtil.trimSlashes(uri);
		Queue<String> pathTokens = new Queue<>();
		
		for (String p : uri.split("\\/"))
			pathTokens.add(p);

		while (next != null)
		{
			String pathPart = pathTokens.dequeue();
			for (Node edge : next.edges)
			{
				if (edge.matches(pathPart))
				{
					if (edge.remainder)
						lastMatch = edge;
					if (edge.type == NodeType.PATHVARIABLE)
						out.pathVariables.put(edge.token, pathPart);
					
				}
			}
		}
		

	}
	
	private enum NodeType
	{
		ROOT,
		MATCH,
		PATHVARIABLE;
	}

	/**
	 * A single node.
	 */
	private static class Node implements Comparable<Node>
	{
		NodeType type;
		String token;
		Pattern pattern;
		
		boolean remainder;
		ControllerEntryPoint entryMethod;
		SortedList<Node> edges;
		
		private Node(NodeType type, String token, Pattern pattern, boolean remainder)
		{
			this.type = type;
			this.token = token;
			this.pattern = pattern;
			this.remainder = remainder;
			this.entryMethod = null;
			this.edges = new SortedList<>();
		}
		
		static Node createRoot()
		{
			return new Node(NodeType.ROOT, null, null, false);
		}
		
		static Node createMatchNode(String token, boolean remainder)
		{
			return new Node(NodeType.MATCH, token, null, remainder);
		}
		
		static Node createVariableNode(String token, Pattern pattern, boolean remainder)
		{
			return new Node(NodeType.PATHVARIABLE, token, pattern, remainder);
		}

		@Override
		public int compareTo(Node n)
		{
			return type != n.type 
					? type.ordinal() - n.type.ordinal() 
					: !token.equals(n.token) 
						? token.compareTo(n.token) 
						: 0;
		}
		
		/**
		 * Tests if this node matches a path part.
		 * @param pathPart the part of the path to test.
		 * @return
		 */
		private boolean matches(String pathPart)
		{
			if (type == NodeType.ROOT)
				return true;
			else if (type == NodeType.MATCH)
			{
				if (!Common.isEmpty(token))
					return token.equals(pathPart);
				else if (!Common.isEmpty(pattern))
					return pattern.matcher(pathPart).matches();
				else
					return false;
			}
			else if (type == NodeType.PATHVARIABLE)
			{
				if (!Common.isEmpty(pattern))
					return pattern.matcher(pathPart).matches();
				else
					return true;
			}
			else
				return false;
		}
		
	}
	
	/**
	 * Result class after a URITrie search.
	 */
	public static class Result
	{
		HashMap<String, String> pathVariables;
		String matchedPath; 
		String remainingPath; 
		ControllerEntryPoint entryPoint; 
		
		private Result()
		{
			this.pathVariables = new HashMap<>();
			this.matchedPath = null;
			this.remainingPath = null;
			this.entryPoint = null;
		}
		
	}

}
