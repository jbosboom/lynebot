package com.jeffreybosboom.lyne;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.jeffreybosboom.region.Region;
import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI interaction with Lyne.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/5/2014
 */
public final class Effector {
	private final Robot robot;
	private final Rectangle lyneRect;
	public Effector() throws AWTException, IOException, InterruptedException {
		this.robot = new Robot();
		robot.setAutoDelay(100);

		//compute the bounds of the virtual display
		//based on the GraphicsConfiguration Javadoc example code
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice gd : ge.getScreenDevices())
			for (GraphicsConfiguration gc : gd.getConfigurations())
				virtualBounds = virtualBounds.union(gc.getBounds());

		BufferedImage screenshot = robot.createScreenCapture(virtualBounds);
		Region gameBorder = Region.connectedComponents(screenshot, ImmutableSet.of(Colors.GAME_BOARDER)).stream()
				.sorted(Comparator.<Region>comparingDouble(r -> r.boundingBox().getWidth() * r.boundingBox().getHeight()).reversed())
				.findFirst().get();
		this.lyneRect = gameBorder.boundingBox();
	}

	public void playPuzzle() {
		BufferedImage image = robot.createScreenCapture(lyneRect);
//		ImageIO.write(image, "PNG", new File("screenshot.png"));
		Pair<Puzzle, ImmutableMap<Node, Region.Point>> parseImage = parseImage(image);
		System.out.println(parseImage.first);
		Set<List<Node>> solutionPaths = Solver.solve(parseImage.first);
		ImmutableMap<Node, Region.Point> pointMap = parseImage.second;
		for (List<Node> path : solutionPaths) {
			mouseMove(pointMap.get(path.get(0)));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			for (int i = 1; i < path.size(); ++i)
				mouseMove(pointMap.get(path.get(i)));
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		}
	}

	private void mouseMove(Region.Point p) {
		robot.mouseMove(p.x + lyneRect.getLocation().x, p.y + lyneRect.getLocation().y);
	}

	private static final int TOLERANCE = 10;
	public static Pair<Puzzle, ImmutableMap<Node, Region.Point>> parseImage(BufferedImage image) {
		ImmutableSet<Region> regions = Region.connectedComponents(image, Colors.LYNE_COLORS);
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
				.mapToInt(Region.Point::y)
				.mapToObj(i -> Range.closed(i - TOLERANCE, i + TOLERANCE))
				.forEachOrdered(rowRanges::add);
		List<Range<Integer>> rows = rowRanges.asRanges().stream().collect(Collectors.toList());
		RangeSet<Integer> colRanges = TreeRangeSet.create();
		nodeRegions.stream()
				.map(Region::centroid)
				.mapToInt(Region.Point::x)
				.mapToObj(i -> Range.closed(i - TOLERANCE, i + TOLERANCE))
				.forEachOrdered(colRanges::add);
		List<Range<Integer>> cols = colRanges.asRanges().stream().collect(Collectors.toList());

		Node[][] puzzle = new Node[rows.size()][cols.size()];
		ImmutableMap.Builder<Node, Region.Point> mapBuilder = ImmutableMap.builder();
		for (Region r : nodeRegions) {
			Region.Point c = r.centroid();
			Rectangle b = r.boundingBox();
			int row = rows.indexOf(rowRanges.rangeContaining(c.y()));
			int col = cols.indexOf(colRanges.rangeContaining(c.x()));
			Node.Kind kind = Colors.NODE_COLORS.get(r.color());
			Node node;
			if (kind == Node.Kind.OCTAGON) {
				int pips = (int)pipRegions.stream().filter(p -> b.contains(p.boundingBox())).count();
				node = Node.octagon(row, col, pips);
			} else {
				boolean terminal = terminalRegions.stream().anyMatch(t -> b.contains(t.boundingBox()));
				node = terminal ? Node.terminal(row, col, kind) : Node.nonterminal(row, col, kind);
			}
			puzzle[row][col] = node;
			mapBuilder.put(node, c);
		}
		return new Pair<>(new Puzzle(puzzle), mapBuilder.build());
	}

	public static void main(String[] args) throws Throwable {
		Effector effector = new Effector();
		effector.playPuzzle();
	}
}
