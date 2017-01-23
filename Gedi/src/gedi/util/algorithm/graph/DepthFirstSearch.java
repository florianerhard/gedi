package gedi.util.algorithm.graph;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * General purpose dfs
 * @author erhard
 *
 * @param <N>
 */
public class DepthFirstSearch {

	
	public static <N> void dfs(N node, BiPredicate<N, N> parentToChildAction, BiConsumer<N,List<N>> adjacency) {
		dfs(node,parentToChildAction,null,adjacency);
	}

	public static <N> void dfs(N node, BiPredicate<N, N> parentToChildAction,
			BiConsumer<N, N> childToParentAction, BiConsumer<N,List<N>> adjacency) {
		
		HashMap<N,N> parentToLastVisitedChild = new HashMap<N,N>();
		
		ArrayList<N> adjBuffer = new ArrayList<N>();
		
		Stack<N> stack = new Stack<N>();
		stack.push(node);
		while(!stack.isEmpty()) {
			
			N n = stack.pop();
			
			N lastVisited = parentToLastVisitedChild.get(n);
			if (lastVisited!=null) {
				// already been visited
				childToParentAction.accept(lastVisited, n);
				parentToLastVisitedChild.put(n, stack.peek());
			}
			else {
				// not yet visited
				adjBuffer.clear();
				adjacency.accept(n, adjBuffer);
				for (int i=adjBuffer.size()-1; i>=0; i--) {
					N c = adjBuffer.get(i);
					if (parentToChildAction.test(n, c)) {
						if (childToParentAction!=null)
							stack.push(n);
						stack.push(c);
						if (childToParentAction!=null)
							parentToLastVisitedChild.put(n,c);
					}
				}
			}
		}
	}
	
}
