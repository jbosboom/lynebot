package com.jeffreybosboom.lyne;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.jeffreybosboom.lyne.rules.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Solves puzzles.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/23/2014
 */
public class Solver {
	private static final Function<Puzzle, Puzzle> ONE_TIME_INFERENCE = Function.<Puzzle>identity()
			.andThen(new ColorColorRule())
			.andThen(new ColorOctagonRule())
			.andThen(new TerminalTerminalRule())
			;
	private static final Function<Puzzle, Puzzle> MULTI_TIME_INFERENCE = fixpoint(Function.<Puzzle>identity()
			.andThen(fixpoint(new DesiredEdgesRule()))
			.andThen(fixpoint(new CrossingEdgesRule()))
			.andThen(fixpoint(new OctagonOneEdgeOfColorRule()))
	);

	/**
	 * Solves the given puzzle using a backtracking search.
	 * @param p the puzzle to search
	 * @return solution paths (one per color), or null
	 */
	public static Set<List<Node>> solve(Puzzle p) {
		return solve_recurse(ONE_TIME_INFERENCE.apply(p));
	}

	private static Set<List<Node>> solve_recurse(Puzzle p) {
		p = MULTI_TIME_INFERENCE.apply(p);
		final Puzzle p_ = p;
		Optional<Pair<Node, Node>> maybe = p.edges()
				.filter(a -> p_.possibilities(a.first, a.second).size() > 1)
				.sorted((a, b) -> Integer.compare(p_.possibilities(a.first, a.second).size(), p_.possibilities(b.first, b.second).size()))
				.findFirst();
		if (!maybe.isPresent())
			return solutionPaths(p);

		Pair<Node, Node> edge = maybe.get();
		ImmutableSet<Node.Kind> possibilities = p.possibilities(edge.first, edge.second);
		for (Node.Kind k : possibilities)
			try {
				Set<List<Node>> recurse = solve_recurse(p.set(edge.first, edge.second, k));
				if (recurse != null) return recurse;
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
		checkArgument(puzzle.edges().allMatch(a -> puzzle.possibilities(a.first, a.second).size() == 1));
		ImmutableSet.Builder<List<Node>> pathsBuilder = ImmutableSet.builder();
		for (Iterator<Pair<Node, Node>> it = puzzle.terminals().iterator(); it.hasNext();) {
			Pair<Node, Node> pair = it.next();
			List<Node> path = new ArrayList<>();
			path.add(pair.first);
			path = findPath(puzzle, path, pair.second, new HashSet<>());
			if (path == null) return null;
			pathsBuilder.add(path);
		}
		ImmutableSet<List<Node>> paths = pathsBuilder.build();
		Multiset<Node> counts = HashMultiset.create();
		paths.stream().forEachOrdered(counts::addAll);
		//ensure each node appears enough times over all the paths
		if (!puzzle.nodes().allMatch(n -> counts.count(n) == (n.desiredEdges()+1)/2))
			return null;
		return paths;
	}

	private static List<Node> findPath(Puzzle puzzle, List<Node> path, Node dest, Set<Pair<Node, Node>> usedEdges) {
		Node cur = path.get(path.size()-1);
		if (cur.equals(dest))
			return path;

		Iterator<Node> maybeNext = puzzle.neighbors(cur)
				.filter(n -> puzzle.possibilities(cur, n).contains(dest.kind()))
				.filter(n -> !usedEdges.contains(Pair.sorted(cur, n)))
				//don't return to a terminal we've already visited
				.filter(n -> !(n.isTerminal() && path.contains(n)))
				//ensure we pick the other terminal last
				.sorted((n1, n2) -> n1 == dest ? 1 : n2 == dest ? -1 : 0)
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

	private static <T, R extends T> Function<T, R> fixpoint(Function<T, R> f) {
		return (t) -> {
			T current = t;
			while (true) {
				R next = f.apply(current);
				if (current.equals(next)) return next;
				current = next;
			}
		};
	}
}
