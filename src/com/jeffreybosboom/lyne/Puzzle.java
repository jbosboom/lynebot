package com.jeffreybosboom.lyne;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.jeffreybosboom.region.Region;
import com.jeffreybosboom.region.Region.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
	private final Node[][] nodes;
	private final ImmutableTable<Node, Node, ImmutableSet<Node.Kind>> edgeSets;
	private Puzzle(Node[][] nodes) {
		assert Arrays.stream(nodes).mapToInt(x -> x.length).distinct().count() == 1 : "array not rectangular";
		this.nodes = nodes;
		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> builder = ImmutableTable.builder();
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
					builder.put(n, a, initialEdgeSet(n, a));
				}
			}
		this.edgeSets = builder.build();
	}
	private static ImmutableSet<Node.Kind> initialEdgeSet(Node n, Node a) {
		if (n.kind() == Node.Kind.OCTAGON && a.kind() == Node.Kind.OCTAGON)
			return ImmutableSet.copyOf(Node.Kind.values());
		if (n.kind().isColored() && a.kind() == Node.Kind.OCTAGON)
			return ImmutableSet.of(a.kind(), Node.Kind.NONE);
		if (n.kind() == Node.Kind.OCTAGON && a.kind().isColored())
			return ImmutableSet.of(n.kind(), Node.Kind.NONE);
		if (n.kind().isColored() && n.kind().equals(a.kind()))
			return ImmutableSet.of(n.kind(), Node.Kind.NONE);
		return ImmutableSet.of(Node.Kind.NONE);
	}

	private Puzzle(Node[][] nodes, ImmutableTable<Node, Node, ImmutableSet<Node.Kind>> edgeSets) {
		this.nodes = nodes;
		this.edgeSets = edgeSets;
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
		return new Puzzle(puzzle);
	}

	public Stream<Node> nodes() {
		return Arrays.stream(nodes).flatMap(Arrays::stream).filter(x -> x != null);
	}

//	/**
//	 * Returns each pair of adjacent nodes
//	 * @return
//	 */
//	public Stream<Pair<Node, Node>> pairs() {
//
//	}

	public ImmutableSet<Node.Kind> possibilities(Node a, Node b) {
		Pair<Node, Node> p = canonicalOrder(a, b);
		return edgeSets.get(p.first, p.second);
	}

	/**
	 * Returns a Puzzle with the given possibility removed from the edge between
	 * the given nodes.  If the possibility is already not possible, this Puzzle
	 * is returned.  If this removes the last possibility, a
	 * ContradictionException is thrown.
	 * @param a
	 * @param b
	 * @param possibility
	 * @return
	 * @throws ContradictionException
	 */
	public Puzzle remove(Node a, Node b, Node.Kind possibility) {
		Pair<Node, Node> p = canonicalOrder(a, b);
		ImmutableSet<Node.Kind> possibilities = possibilities(a, b);
		if (!possibilities.contains(possibility))
			return this;
		if (possibilities.size() == 1)
			throw new ContradictionException();

		ImmutableSet<Node.Kind> newSet = ImmutableSet.copyOf(
				possibilities.stream().filter(x -> x != possibility).iterator());
		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> tableBuilder = ImmutableTable.builder();
		edgeSets.cellSet().stream()
				.filter(c -> !(c.getColumnKey().equals(p.first) && c.getRowKey().equals(p.second)))
				.forEachOrdered(tableBuilder::put);
		tableBuilder.put(p.first, p.second, newSet);
		return new Puzzle(nodes, tableBuilder.build());
	}

	/**
	 * Returns a Puzzle with the given possibility being the only one in the
	 * edge between the given nodes.  If this possibility is already the only
	 * possible, this Puzzle is returned.  If this possibility is not possible,
	 * a ContradictionException is thrown.
	 * @param a
	 * @param b
	 * @param possibility
	 * @return
	 * @throws ContradictionException
	 */
	public Puzzle set(Node a, Node b, Node.Kind possibility) {
		Pair<Node, Node> p = canonicalOrder(a, b);
		ImmutableSet<Node.Kind> possibilities = possibilities(a, b);
		if (!possibilities.contains(possibility))
			throw new ContradictionException();
		if (possibilities.equals(ImmutableSet.of(possibility)))
			return this;

		ImmutableTable.Builder<Node, Node, ImmutableSet<Node.Kind>> tableBuilder = ImmutableTable.builder();
		edgeSets.cellSet().stream()
				.filter(c -> !(c.getColumnKey().equals(p.first) && c.getRowKey().equals(p.second)))
				.forEachOrdered(tableBuilder::put);
		tableBuilder.put(p.first, p.second, ImmutableSet.of(possibility));
		return new Puzzle(nodes, tableBuilder.build());
	}

	private static Pair<Node, Node> canonicalOrder(Node a, Node b) {
		if (!(a.row() < b.row() || a.row() == b.row() && a.col() < b.col())) {
			 Node t = a;
			 a = b;
			 b = t;
		 }
		return new Pair<>(a, b);
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
		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		BufferedImage image = ImageIO.read(new File("266010_2014-06-29_00001.png"));
		System.out.println(Puzzle.fromImage(image));
	}
}
