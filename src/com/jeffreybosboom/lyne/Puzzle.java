package com.jeffreybosboom.lyne;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/8/2014
 */
public final class Puzzle {
	private static final int[][] NEIGHBORHOOD = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1}, {0, 1},
		{1, -1}, {1, 0}, {1, 1},
	};
	private final Node[][] nodes;
	private final ImmutableMap<Node, ImmutableSet<Node>> neighbors;
	//TODO: consider replacing with edgeSets.keySet() -- not sure of performance
	//impact, as edges() is hammered by CrossingEdgesRule
	private final ImmutableSet<Pair<Node, Node>> edges;
	private final ImmutableMap<Pair<Node, Node>, ImmutableSet<Node.Kind>> edgeSets;
	public Puzzle(Node[][] nodes) {
		assert Arrays.stream(nodes).mapToInt(x -> x.length).distinct().count() == 1 : "array not rectangular";
		this.nodes = nodes;

		ImmutableMap.Builder<Node, ImmutableSet<Node>> neighborsBuilder = ImmutableMap.builder();
		nodes().forEachOrdered(n -> {
			neighborsBuilder.put(n, ImmutableSet.copyOf(
					Arrays.stream(NEIGHBORHOOD)
							.map(p -> new int[]{n.row() + p[0], n.col() + p[1]})
							.filter(p -> 0 <= p[0] && p[0] < nodes.length)
							.filter(p -> 0 <= p[1] && p[1] < nodes[0].length)
							.map(p -> nodes[p[0]][p[1]])
							.filter(x -> x != null)
							.iterator()
			));
		});
		this.neighbors = neighborsBuilder.build();

		this.edges = ImmutableSet.copyOf(nodes().
				filter(n -> n != null)
				.flatMap(a -> neighbors(a).map(b -> Pair.sorted(a, b)))
				.iterator());

		//Only include colors if nodes of that color are present.
		ImmutableSet<Node.Kind> maximalEdgeSet = ImmutableSet.<Node.Kind>builder()
				.addAll(Arrays.stream(nodes)
						.flatMap(Arrays::stream)
						.filter(x -> x != null)
						.map(Node::kind)
						.filter(Node.Kind::isColored).iterator())
				.add(Node.Kind.NONE)
				.build();
		ImmutableMap.Builder<Pair<Node, Node>, ImmutableSet<Node.Kind>> edgeSetsBuilder = ImmutableMap.builder();
		edges().forEachOrdered(e -> edgeSetsBuilder.put(e, maximalEdgeSet));
		this.edgeSets = edgeSetsBuilder.build();
	}

	private Puzzle(Puzzle puzzle, ImmutableMap<Pair<Node, Node>, ImmutableSet<Node.Kind>> edgeSets) {
		this.nodes = puzzle.nodes;
		this.edges = puzzle.edges;
		this.neighbors = puzzle.neighbors;
		this.edgeSets = edgeSets;
	}

	private Puzzle withEdgeSet(Pair<Node, Node> edge, ImmutableSet<Node.Kind> newEdgeSet) {
		assert neighbors(edge.first).anyMatch(Predicate.isEqual(edge.second));
		assert edge.first.compareTo(edge.second) < 0;
		assert edgeSets.containsKey(edge) : "not an edge: "+edge;
		assert !newEdgeSet.contains(Node.Kind.OCTAGON);
		assert !newEdgeSet.isEmpty();
		//TODO: no-change and ContradictionException checks could be moved here
		//from set and remove.
		ImmutableMap.Builder<Pair<Node, Node>, ImmutableSet<Node.Kind>> edgeSetBuilder = ImmutableMap.builder();
		edgeSets.entrySet().stream()
				.filter(e -> !e.getKey().equals(edge))
				.forEachOrdered(edgeSetBuilder::put);
		edgeSetBuilder.put(edge, newEdgeSet);
		return new Puzzle(this, edgeSetBuilder.build());
	}

	/**
	 * Creates a Puzzle by parsing a String.  This method is the inverse of
	 * toString().
	 */
	public static Puzzle fromString(String str) {
		String[] rows = str.split("\n");
		checkArgument(Arrays.stream(rows).mapToInt(s -> s.length()).distinct().count() == 1, "not rectangular");
		Node[][] nodes = new Node[rows.length][rows[0].length()];
		for (int row = 0; row < rows.length; ++row)
			for (int col = 0; col < rows[0].length(); ++col) {
				char c = rows[row].charAt(col);
				if (Character.isDigit(c))
					nodes[row][col] = Node.octagon(row, col, Character.digit(c, 10));
				else {
					Node.Kind kind = Arrays.stream(Node.Kind.values())
							.filter(k -> k.toString().startsWith(""+Character.toUpperCase(c)))
							.findFirst().get();
					nodes[row][col] = Character.isUpperCase(c) ?
							Node.terminal(row, col, kind) :
							Node.nonterminal(row, col, kind);
				}
			}
		return new Puzzle(nodes);
	}

	public Node at(int row, int col) {
		return nodes[row][col];
	}

	public Stream<Node> nodes() {
		return Arrays.stream(nodes).flatMap(Arrays::stream).filter(x -> x != null);
	}

	/**
	 * Returns the pair of terminals for each color present in the puzzle.
	 * These are usually not edges!
	 * @return pairs of terminals
	 */
	public Stream<Pair<Node, Node>> terminals() {
		return nodes().filter(Node::isTerminal)
				.collect(Collectors.groupingBy(Node::kind))
				.values().stream()
				.map(l -> {assert l.size() == 2 : l; return Pair.sorted(l.get(0), l.get(1));});
	}

	public Stream<Node> neighbors(Node n) {
		return neighbors.get(n).stream();
	}

	/**
	 * Returns each edge (pair of adjacent nodes) exactly once.  The returned
	 * pairs are in canonical order.
	 * @return a stream of edges in this puzzle
	 */
	public Stream<Pair<Node, Node>> edges() {
		return edges.stream();
	}

	public ImmutableSet<Node.Kind> possibilities(Node a, Node b) {
		Pair<Node, Node> p = Pair.sorted(a, b);
		return edgeSets.get(p);
	}

	/**
	 * Returns a Puzzle with the given possibility removed from the edge between
	 * the given nodes.  If the possibility is already not possible, this Puzzle
	 * is returned.  If this removes the last possibility for this edge,
	 * a ContradictionException is thrown.
	 */
	public Puzzle remove(Node a, Node b, Node.Kind possibility) {
		Pair<Node, Node> p = Pair.sorted(a, b);
		ImmutableSet<Node.Kind> possibilities = possibilities(a, b);
		if (!possibilities.contains(possibility))
			return this;
		if (possibilities.size() == 1)
			throw new ContradictionException();

		ImmutableSet<Node.Kind> newSet = ImmutableSet.copyOf(
				possibilities.stream().filter(x -> x != possibility).iterator());
		return withEdgeSet(p, newSet);
	}

	public Puzzle restrict(Node a, Node b, Set<Node.Kind> possibilities) {
		return set(a, b, Sets.intersection(possibilities, possibilities(a, b)));
	}

	/**
	 * Returns a Puzzle with the given possibility being the only one in the
	 * edge between the given nodes.  If this possibility is already the only
	 * possible, this Puzzle is returned.  If this possibility is not possible,
	 * a ContradictionException is thrown.
	 */
	public Puzzle set(Node a, Node b, Node.Kind possibility) {
		return set(a, b, ImmutableSet.of(possibility));
	}

	private Puzzle set(Node a, Node b, Set<Node.Kind> possibilities) {
		//private because rules actually want to call restrict instead
		Pair<Node, Node> p = Pair.sorted(a, b);
		ImmutableSet<Node.Kind> currentPossibilities = possibilities(a, b);
		if (possibilities.isEmpty() || !currentPossibilities.containsAll(possibilities))
			throw new ContradictionException();
		if (currentPossibilities.equals(possibilities))
			return this;

		return withEdgeSet(p, ImmutableSet.copyOf(possibilities));
	}

	@Override
	public String toString() {
		//TODO: concise way to print edge sets?
		StringBuilder sb = new StringBuilder();
		for (Node[] r : nodes) {
			for (Node n : r)
				sb.append(n == null ? " " : n.toString());
			sb.append("\n");
		}
		return sb.toString().trim();
	}
}
