package com.blackrook.j2ee.small.struct;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blackrook.commons.list.SortedList;
import com.blackrook.j2ee.small.descriptor.ControllerEntryMethod;

/**
 * A trie that organizes mapping URI patterns to ControllerHandlers.
 * @author Matthew Tropiano
 */
public class URITrie
{
	// TODO: Finish
	
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
		ControllerEntryMethod entryMethod;
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
		
	}
	
	/*
		URITrie
			root : URITrie.Node
			resolve(String uri) : URITrie.Result
		
		.Node -> Comparable<Node>
			type : NodeType
			token : String			// MATCH key or PATH_VARIABLE name
			pattern : Pattern		// if not null, must match pattern to be viable path.
			remainder : boolean 	// if true, tentatively "succeed" here and return remainder if no further matches
			descriptor : ControllerMethodDescriptor
			edges : SortedList<URITrie.Node>
			*compareTo(Node) : boolean
				type, token
			
		.Context
			pathTokens : Queue<String>
			currentNode : URITrie.Node
			matchedPath : StringBuilder
			remainingPath : StringBuilder
			Context(String) : <constructor>
			
		.Result
			pathVariables : HashMap<String, String>
			matchedPath : String
			remainingPath : String
			entryPoint : ControllerMethodDescriptor
		
	 */
	
	
}
