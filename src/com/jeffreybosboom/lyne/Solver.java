package com.jeffreybosboom.lyne;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Solves puzzles.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/23/2014
 */
public class Solver {
	/**
	 * Solves the given puzzle using a backtracking search.
	 * @param p the puzzle to search
	 * @return solution paths (one per color), or null
	 */
	public static Set<List<Node>> solve(Puzzle p) {
		Optional<Pair<Node, Node>> maybe = p.pairs()
				.filter(a -> p.possibilities(a.first, a.second).size() > 1)
				.sorted((a, b) -> Integer.compare(p.possibilities(a.first, a.second).size(), p.possibilities(b.first, b.second).size()))
				.findFirst();
		if (!maybe.isPresent())
			return solutionPaths(p);
		Pair<Node, Node> edge = maybe.get();
		ImmutableSet<Node.Kind> possibilities = p.possibilities(edge.first, edge.second);
		for (Node.Kind k : possibilities)
			try {
				return solve(p.set(edge.first, edge.second, k));
			} catch (ContradictionException e) {}
		return null;
	}

	/**
	 * Returns the paths through the given solved puzzle, one per color.  Note
	 * there may be more than one possible path for a given puzzle and color;
	 * this method makes an arbitrary choice.
	 * @param puzzle a solved puzzle
	 * @return the solution paths, one per color
	 * @throws ContradictionException
	 */
	private static Set<List<Node>> solutionPaths(Puzzle puzzle) {
		puzzle.getClass();
		checkArgument(puzzle.pairs().allMatch(a -> puzzle.possibilities(a.first, a.second).size() == 1));
		return puzzle.terminals().map(pair -> {
			List<Node> path = new ArrayList<>();
			path.add(pair.first);
			path = findPath(puzzle, path, pair.second, new HashSet<>());
			if (path == null)
				throw new ContradictionException();
			return path;
		}).collect(Collectors.toSet());
	}

	private static List<Node> findPath(Puzzle puzzle, List<Node> path, Node dest, Set<Pair<Node, Node>> usedEdges) {
		Node cur = path.get(path.size()-1);
		if (cur.equals(dest) && puzzle.nodes().filter(n -> n.kind() == dest.kind()).allMatch(path::contains))
			return path;

		Iterator<Node> maybeNext = puzzle.neighbors(cur)
				.filter(n -> puzzle.possibilities(cur, n).contains(dest.kind()))
				//ensure we pick the other terminal last
				.sorted((n1, n2) -> n1 == dest ? 1 : n2 == dest ? -1 : 0)
				.filter(n -> !usedEdges.contains(Pair.sorted(cur, n)))
				.iterator();
		while (maybeNext.hasNext()) {
			Node next = maybeNext.next();
			path.add(next);
			Pair<Node, Node> edge = Pair.sorted(cur, next);
			usedEdges.add(edge);
			List<Node> recurse = findPath(puzzle, path, dest, usedEdges);
			if (recurse != null)
				return recurse;
			path.remove(path.size()-1);
			usedEdges.remove(edge);
		}
		return null;
	}
}
