package com.blackrook.j2ee.small.struct;

/**
 * A trie that organizes mapping URI patterns to ControllerHandlers.
 * @author Matthew Tropiano
 */
public class URITrie
{
	// TODO: Finish
	
	
	/*
		URITrie
			root : URITrie.Node
			resolve(String uri) : URITrie.Result
		
		.NodeType
			ROOT
			MATCH
			PATHVARIABLE
			
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
