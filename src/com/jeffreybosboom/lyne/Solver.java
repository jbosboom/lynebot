package com.jeffreybosboom.lyne;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;

/**
 * Solves puzzles.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/23/2014
 */
public class Solver {
	/**
	 * Solves the given puzzle using a backtracking search.
	 * @param p the puzzle to search
	 * @return a Puzzle with exactly one possibility for each edge, or null if
	 * the puzzle has no solution
	 */
	public static Puzzle solve(Puzzle p) {
		Optional<Pair<Node, Node>> maybe = p.pairs()
				.filter(a -> p.possibilities(a.first, a.second).size() > 1)
				.sorted((a, b) -> Integer.compare(p.possibilities(a.first, a.second).size(), p.possibilities(b.first, b.second).size()))
				.findFirst();
		if (!maybe.isPresent())
			return p;
		Pair<Node, Node> edge = maybe.get();
		ImmutableSet<Node.Kind> possibilities = p.possibilities(edge.first, edge.second);
		for (Node.Kind k : possibilities)
			try {
				return solve(p.remove(edge.first, edge.second, k));
			} catch (ContradictionException e) {}
		return null;
	}
}
