package com.blackrook.small.struct;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.small.dispatch.controller.ControllerEntryPoint;
import com.blackrook.small.exception.SmallFrameworkParseException;
import com.blackrook.small.util.SmallUtil;

/**
 * A trie that organizes mapping URI patterns to ControllerHandlers.
 * @author Matthew Tropiano
 */
public class URITrie
{
	/** Token content for "default" path. */
	public static final String DEFAULT_TOKEN = "*";

	/** Root node. */
	private Node root; 

	/**
	 * Creates a new blank URI Trie.
	 */
	public URITrie()
	{
		this.root = Node.createRoot();
	}
	
	/**
	 * Adds a path to the trie.
	 * @param uri the request URI path.
	 * @param entryPoint the mapped entry point.
	 * @throws SmallFrameworkParseException if a path parsing error occurs.
	 * @throws PatternSyntaxException if a regex expression is invalid in one of the paths.
	 */
	public void add(String uri, ControllerEntryPoint entryPoint)
	{
		final int STATE_START = 0;
		final int STATE_PATH = 1;
		final int STATE_VARIABLE = 2;
		final int STATE_REGEX = 3;
		final int STATE_VARIABLE_END = 4;

		Node endNode = root;
		String currentVariable = null;
		
		uri = SmallUtil.trimSlashes(uri);
		
		if (!Utils.isEmpty(uri))
		{
			StringBuilder sb = new StringBuilder();
			int state = STATE_START;
			
			for (int i = 0; i < uri.length(); i++)
			{
				char c = uri.charAt(i);
				switch (state)
				{
					case STATE_START:
						if (c == '/')
						{
							// Do nothing
						}
						else if (c == '{')
							state = STATE_VARIABLE;
						else
						{
							sb.append(c);
							state = STATE_PATH;
						}
						break;
					
					case STATE_PATH:
						if (c == '/')
						{
							String token = sb.toString().trim();
							if (token.equals(DEFAULT_TOKEN))
								throw new SmallFrameworkParseException("Wildcard token must be the last segment.");
							
							endNode.edges.add(endNode = Node.createMatchNode(token));
							sb.delete(0, sb.length());
						}
						else
						{
							sb.append(c);
						}
						break;
					
					case STATE_VARIABLE:
						if (c == ':')
						{
							currentVariable = sb.toString();
							sb.delete(0, sb.length());
							state = STATE_REGEX;
						}
						else if (c == '}')
						{
							endNode.edges.add(endNode = Node.createVariableNode(sb.toString().trim(), null));
							sb.delete(0, sb.length());
							state = STATE_VARIABLE_END;
						}
						else
						{
							sb.append(c);
						}
						break;
					
					case STATE_REGEX:
						if (c == '}')
						{
							endNode.edges.add(endNode = Node.createVariableNode(currentVariable, Pattern.compile(sb.toString().trim())));
							sb.delete(0, sb.length());
							state = STATE_VARIABLE_END;
						}
						else
						{
							sb.append(c);
						}
						break;
					
					case STATE_VARIABLE_END:
						if (c == '/')
							state = STATE_START;
						else
							throw new SmallFrameworkParseException("Expected '/' to terminate path segment.");
						break;
						
				}// switch
			}// for
			
			if (state == STATE_VARIABLE)
				throw new SmallFrameworkParseException("Expected '}' to terminate variable segment.");
			if (state == STATE_REGEX)
				throw new SmallFrameworkParseException("Expected '}' to terminate variable regex segment.");
			
			if (sb.length() > 0)
			{
				String token = sb.toString().trim();
				if (token.equals(DEFAULT_TOKEN))
					endNode.edges.add(endNode = Node.createDefaultNode());
				else
					endNode.edges.add(endNode = Node.createMatchNode(token));
			}
				
			
		}
		
		endNode.entryPoint = entryPoint;
	}
	
	/**
	 * Attempts to resolve an endpoint for a given URI.
	 * @param uri the input URI.
	 * @return a result object detailing the search.
	 */
	public Result resolve(String uri)
	{
		Node next = root;
		Result out = new Result();
		
		uri = SmallUtil.trimSlashes(uri);
		Queue<String> pathTokens = new LinkedList<>();
		
		for (String p : uri.split("\\/"))
			pathTokens.add(p);

		while (next != null && next.type != NodeType.DEFAULT && !pathTokens.isEmpty())
		{
			String pathPart = pathTokens.poll();
			TreeSet<Node> edgeList = next.edges;
			next = null;
			for (Node edge : edgeList)
			{
				if (edge.matches(pathPart))
				{
					if (edge.type == NodeType.PATHVARIABLE)
						out.addVariable(edge.token, pathPart);
					next = edge;
					break;
				}
			}
		}
		
		if (next != null)
			out.entryPoint = next.entryPoint;
		
		return out;
	}
	
	private enum NodeType
	{
		ROOT,
		MATCH,
		PATHVARIABLE,
		DEFAULT;
	}

	/**
	 * A single node.
	 */
	private static class Node implements Comparable<Node>
	{
		NodeType type;
		String token;
		Pattern pattern;
		ControllerEntryPoint entryPoint;
		TreeSet<Node> edges;
		
		private Node(NodeType type, String token, Pattern pattern)
		{
			this.type = type;
			this.token = token;
			this.pattern = pattern;
			this.entryPoint = null;
			this.edges = new TreeSet<>();
		}
		
		static Node createRoot()
		{
			return new Node(NodeType.ROOT, null, null);
		}
		
		static Node createMatchNode(String token)
		{
			return new Node(NodeType.MATCH, token, null);
		}
		
		static Node createVariableNode(String token, Pattern pattern)
		{
			return new Node(NodeType.PATHVARIABLE, token, pattern);
		}

		static Node createDefaultNode()
		{
			return new Node(NodeType.DEFAULT, null, null);
		}

		@Override
		public int compareTo(Node n)
		{
			return type != n.type 
					? type.ordinal() - n.type.ordinal() 
					: !token.equals(n.token) 
						? token.compareTo(n.token) 
						: pattern != null 
							? -1 
							: 0
			;
		}
		
		/**
		 * Tests if this node matches a path part.
		 * @param pathPart the part of the path to test.
		 * @return
		 */
		private boolean matches(String pathPart)
		{
			if (type == NodeType.ROOT || type == NodeType.DEFAULT)
				return true;
			else if (type == NodeType.MATCH)
			{
				if (!Utils.isEmpty(token))
					return token.equals(pathPart);
				else if (!Utils.isEmpty(pattern))
					return pattern.matcher(pathPart).matches();
				else
					return false;
			}
			else if (type == NodeType.PATHVARIABLE)
			{
				if (!Utils.isEmpty(pattern))
					return pattern.matcher(pathPart).matches();
				else
					return true;
			}
			else
				return false;
		}
		
		@Override
		public String toString() 
		{
			return type.name() + " " + token + (pattern != null ? ":" + pattern.pattern() : "") + " " + entryPoint;
		}
		
	}
	
	/**
	 * Result class after a URITrie search.
	 */
	public static class Result
	{
		Map<String, String> pathVariables;
		ControllerEntryPoint entryPoint; 
		
		private Result()
		{
			this.pathVariables = null;
			this.entryPoint = null;
		}
		
		private void addVariable(String var, String value)
		{
			if (pathVariables == null)
				pathVariables = new HashMap<>();
			pathVariables.put(var, value);
		}
		
		/**
		 * Checks if this result has a found entry point in it.
		 * @return true if so, false if not.
		 */
		public boolean hasEndpoint()
		{
			return entryPoint != null;
		}
		
		/**
		 * Gets the found entry point, if any.
		 * @return an entry point to call, or null if no entry point.
		 */
		public ControllerEntryPoint getEntryPoint() 
		{
			return entryPoint;
		}
		
		/**
		 * Gets the map of found path variables, if any.
		 * @return the map of variables, or null if no variables.
		 */
		public Map<String, String> getPathVariables() 
		{
			return pathVariables;
		}
		
	}

}
