package com.jeffreybosboom.lyne;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeSet;
import com.jeffreybosboom.region.Region;
import com.jeffreybosboom.region.Region.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/8/2014
 */
public final class Puzzle {
	private static final int[][] BUILD_ADJUST = {{0, 1}, {1, -1}, {1, 0}, {1, 1}};
	private static final int[][] NEIGHBORHOOD = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1}, {0, 1},
		{1, -1}, {1, 0}, {1, 1},
	};
	private final Node[][] nodes;
	private final ImmutableTable<Node, Node, ImmutableSet<Node.Kind>> edgeSets;
	private Puzzle(Node[][] nodes, ImmutableTable<Node, Node, ImmutableSet<Node.Kind>> edgeSets) {
		this.nodes = nodes;
		this.edgeSets = edgeSets;
	}

	/**
	 * Creates a puzzle from the given nodes, performing inference on initial
	 * edge sets based on node kinds.
	 * @param nodes
	 * @return
	 */
	private static Puzzle initialState(Node[][] nodes) {
		assert Arrays.stream(nodes).mapToInt(x -> x.length).distinct().count() == 1 : "array not rectangular";
		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> builder = ImmutableTable.builder();
		//Only include colors if nodes of that color are present.
		ImmutableSet<Node.Kind> allPossibilities = ImmutableSet.<Node.Kind>builder()
				.addAll(Arrays.stream(nodes)
						.flatMap(Arrays::stream)
						.filter(x -> x != null)
						.map(Node::kind)
						.filter(Node.Kind::isColored).iterator())
				.add(Node.Kind.NONE)
				.build();
		for (int x = 0; x < nodes.length; ++x)
			for (int y = 0; y < nodes[0].length; ++y) {
				Node n = nodes[x][y];
				if (n == null) continue;
				assert n.row() == x;
				assert n.col() == y;
				for (int[] adj : BUILD_ADJUST) {
					int xa = x + adj[0], ya = y + adj[1];
					if (!(0 <= xa && xa < nodes.length && 0 <= ya && ya < nodes[0].length)) continue;
					Node a = nodes[xa][ya];
					if (a == null) continue;
					builder.put(n, a, allPossibilities);
				}
			}
		return new Puzzle(nodes, builder.build());
	}

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
		return initialState(nodes);
	}

	//TODO: this actually belongs in a UI controller class, as we need to
	//remember where each node is on screen
	private static final int TOLERANCE = 10;
	public static Puzzle fromImage(BufferedImage image) {
		ImmutableSet<Region> regions = Region.connectedComponents(image);
		List<Region> nodeRegions = regions.stream()
				.filter(r -> Colors.NODE_COLORS.keySet().contains(r.color()))
				.collect(Collectors.toList());
		//terminal markers and pips are inside node regions, so must be smaller
		int maxSize = nodeRegions.stream().mapToInt(r -> r.points().size()).max().getAsInt();
		List<Region> terminalRegions = regions.stream()
				.filter(r -> r.points().size() < maxSize)
				.filter(r -> r.color() == Colors.TERMINAL_CENTER)
				.collect(Collectors.toList());
		List<Region> pipRegions = regions.stream()
				.filter(r -> r.points().size() < maxSize)
				.filter(r -> r.color() == Colors.PIP)
				.collect(Collectors.toList());

		RangeSet<Integer> rowRanges = TreeRangeSet.create();
		nodeRegions.stream()
				.map(Region::centroid)
				.mapToInt(Point::y)
				.mapToObj(i -> Range.closed(i - TOLERANCE, i + TOLERANCE))
				.forEachOrdered(rowRanges::add);
		List<Range<Integer>> rows = rowRanges.asRanges().stream().collect(Collectors.toList());
		RangeSet<Integer> colRanges = TreeRangeSet.create();
		nodeRegions.stream()
				.map(Region::centroid)
				.mapToInt(Point::x)
				.mapToObj(i -> Range.closed(i - TOLERANCE, i + TOLERANCE))
				.forEachOrdered(colRanges::add);
		List<Range<Integer>> cols = colRanges.asRanges().stream().collect(Collectors.toList());

		Node[][] puzzle = new Node[rows.size()][cols.size()];
		for (Region r : nodeRegions) {
			Point c = r.centroid();
			Rectangle b = r.boundingBox();
			int row = rows.indexOf(rowRanges.rangeContaining(c.y()));
			int col = cols.indexOf(colRanges.rangeContaining(c.x()));
			Node.Kind kind = Colors.NODE_COLORS.get(r.color());
			if (kind == Node.Kind.OCTAGON) {
				int pips = (int)pipRegions.stream().filter(p -> b.contains(p.boundingBox())).count();
				puzzle[row][col] = Node.octagon(row, col, pips);
			} else {
				boolean terminal = terminalRegions.stream().anyMatch(t -> b.contains(t.boundingBox()));
				puzzle[row][col] = terminal ? Node.terminal(row, col, kind) : Node.nonterminal(row, col, kind);
			}
		}
		return Puzzle.initialState(puzzle);
	}

	public Node at(int row, int col) {
		return nodes[row][col];
	}

	public Stream<Node> nodes() {
		return Arrays.stream(nodes).flatMap(Arrays::stream).filter(x -> x != null);
	}

	/**
	 * Returns the pair of terminals for each color present in the puzzle.
	 * @return pairs of terminals
	 */
	public Stream<Pair<Node, Node>> terminals() {
		return nodes().filter(Node::isTerminal)
				.collect(Collectors.groupingBy(Node::kind))
				.values().stream()
				.map(l -> {assert l.size() == 2 : l; return Pair.sorted(l.get(0), l.get(1));});
	}

	public Stream<Node> neighbors(Node n) {
		return Arrays.stream(NEIGHBORHOOD)
				.map(p -> new int[]{n.row() + p[0], n.col() + p[1]})
				.filter(p -> 0 <= p[0] && p[0] < nodes.length)
				.filter(p -> 0 <= p[1] && p[1] < nodes[0].length)
				.map(p -> nodes[p[0]][p[1]])
				.filter(x -> x != null);
	}

	/**
	 * Returns each pair of adjacent nodes exactly once.
	 * @return
	 */
	public Stream<Pair<Node, Node>> edges() {
		return nodes().filter(n -> n != null).flatMap(a -> neighbors(a).map(b -> Pair.sorted(a, b))).distinct();
	}

	public ImmutableSet<Node.Kind> possibilities(Node a, Node b) {
		Pair<Node, Node> p = Pair.sorted(a, b);
		return edgeSets.get(p.first, p.second);
	}

	/**
	 * Returns a Puzzle with the given possibility removed from the edge between
	 * the given nodes.  If the possibility is already not possible, this Puzzle
	 * is returned.  If this removes the last possibility for this edge,
	 * a ContradictionException is thrown.  If the edge was modified,
	 * {@link Node#desiredEdges() desired-edges} processing will be performed on
	 * both nodes (possibly leading to further recursive modification), possibly
	 * leading to a ContradictionException.
	 * @param a
	 * @param b
	 * @param possibility
	 * @return
	 * @throws ContradictionException
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
		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> tableBuilder = ImmutableTable.builder();
		edgeSets.cellSet().stream()
				.filter(c -> !(c.getRowKey().equals(p.first) && c.getColumnKey().equals(p.second)))
				.forEachOrdered(tableBuilder::put);
		tableBuilder.put(p.first, p.second, newSet);
		return new Puzzle(nodes, tableBuilder.build());
	}

	public Puzzle restrict(Node a, Node b, Set<Node.Kind> possibilities) {
		ImmutableSet<Node.Kind> currentPossibilities = possibilities(a, b);
		Set<Node.Kind> intersection = Sets.intersection(possibilities, currentPossibilities);
		return set(a, b, intersection);
	}

	/**
	 * Returns a Puzzle with the given possibility being the only one in the
	 * edge between the given nodes.  If the edge is diagonal and being set to a
	 * possibility besides NONE, the crossing edge
	 * (if any) is set to NONE.  If this possibility is already the only
	 * possible, this Puzzle is returned.  If this possibility is not possible,
	 * a ContradictionException is thrown.  Also deferred-edges processing.
	 * @param a
	 * @param b
	 * @param possibility
	 * @return
	 * @throws ContradictionException
	 */
	public Puzzle set(Node a, Node b, Node.Kind possibility) {
		return set(a, b, ImmutableSet.of(possibility));
	}

	public Puzzle set(Node a, Node b, Set<Node.Kind> possibilities) {
		//TODO: does this make sense?  will we always want restrict instead?
		//maybe private?
		Pair<Node, Node> p = Pair.sorted(a, b);
		ImmutableSet<Node.Kind> currentPossibilities = possibilities(a, b);
		if (possibilities.isEmpty() || !currentPossibilities.containsAll(possibilities))
			throw new ContradictionException();
		if (currentPossibilities.equals(possibilities))
			return this;

		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> tableBuilder = ImmutableTable.builder();
		edgeSets.cellSet().stream()
				.filter(c -> !(c.getRowKey().equals(p.first) && c.getColumnKey().equals(p.second)))
				.forEachOrdered(tableBuilder::put);
		tableBuilder.put(p.first, p.second, ImmutableSet.copyOf(possibilities));
		return new Puzzle(nodes, tableBuilder.build());
	}

	@Override
	public String toString() {
		//TODO: concise way to build edge sets?
		StringBuilder sb = new StringBuilder();
		for (Node[] r : nodes) {
			for (Node n : r)
				sb.append(n == null ? " " : n.toString());
			sb.append("\n");
		}
		return sb.toString().trim();
	}

	public static void main(String[] args) throws IOException {
		try (DirectoryStream<Path> directory = Files.newDirectoryStream(Paths.get("."), "*.png")) {
			for (Path imagePath : directory) {
				trySolve(imagePath);
			}
		}
//		trySolve(Paths.get("266010_2014-08-23_00009.png"));
	}

	private static void trySolve(Path imagePath) throws IOException {
		BufferedImage image = ImageIO.read(new File(imagePath.toString()));
		Puzzle puzzle = Puzzle.fromImage(image);
		System.out.println(imagePath);
		System.out.println(puzzle);
		Set<List<Node>> paths = Solver.solve(puzzle);
		if (paths == null)
			System.out.println("FAILED");
		else
			for (List<Node> path : paths)
				System.out.println(path);
	}
}
