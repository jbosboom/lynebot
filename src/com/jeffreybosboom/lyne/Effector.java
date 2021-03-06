/*
 * Copyright 2014 Jeffrey Bosboom.
 * This file is part of lynebot.
 *
 * lynebot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lynebot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lynebot.  If not, see <http://www.gnu.org/licenses/>.
 */

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
 * UI interaction with Lyne.  Effector finds the Lyne window by looking for its
 * characteristic border, and assumes the window will not move once found.  Note
 * that Effector may misdetect screenshots of Lyne as the actual Lyne window.
 *
 * Effector currently does not automate any of the menus.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/5/2014
 */
public final class Effector {
	private final Robot robot;
	private final Rectangle lyneRect;
	public Effector() throws AWTException, IOException, InterruptedException {
		this.robot = new Robot();
		robot.setAutoDelay(25);

		//compute the bounds of the virtual display
		//based on the GraphicsConfiguration Javadoc example code
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice gd : ge.getScreenDevices())
			for (GraphicsConfiguration gc : gd.getConfigurations())
				virtualBounds = virtualBounds.union(gc.getBounds());

		BufferedImage screenshot = robot.createScreenCapture(virtualBounds);
		Region gameBorder = Region.connectedComponents(screenshot, ImmutableSet.of(Colors.GAME_BORDER)).stream()
				.sorted(Comparator.<Region>comparingDouble(r -> r.boundingBox().getWidth() * r.boundingBox().getHeight()).reversed())
				.findFirst().get();
		this.lyneRect = gameBorder.boundingBox();
	}

	public void playPuzzle() {
		BufferedImage image = robot.createScreenCapture(lyneRect);
		Pair<Puzzle, ImmutableMap<Node, Region.Point>> parseImage = parseImage(image);
		System.out.println(parseImage.first);
		Set<List<Node>> solutionPaths = Solver.solve(parseImage.first);
		ImmutableMap<Node, Region.Point> pointMap = parseImage.second;
		for (List<Node> path : solutionPaths) {
			System.out.println(path.stream()
					.map(n -> String.format("%s (%d, %d)", n, n.row(), n.col()))
					.collect(Collectors.joining(", ")));
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
		for (int i = 0; i < 25; ++i) {
			effector.playPuzzle();
			Thread.sleep(2000);
		}
	}
}
