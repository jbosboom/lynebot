package com.jeffreybosboom.region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/16/2014
 */
public final class Region {
	/**
	 * as from BufferedImage.getRGB
	 */
	private final int color;
	private final ImmutableList<Point> points;
	private final IntSummaryStatistics xStats, yStats;
	private Region(int color, List<Point> points) {
		this.color = color;
		this.points = ImmutableList.copyOf(points);
		//TODO: lazy initialize?
		//TODO: compute in one pass?
		this.xStats = this.points.stream().mapToInt(Point::x).summaryStatistics();
		this.yStats = this.points.stream().mapToInt(Point::y).summaryStatistics();
	}

	private static final int[][] NEIGHBORHOOD = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1}, {0, 1},
		{1, -1}, {1, 0}, {1, 1},
	};
	public static ImmutableSet<Region> connectedComponents(BufferedImage image, Set<Integer> interestingColors) {
		final int imageSize = image.getWidth() * image.getHeight();
		BitSet processed = new BitSet(imageSize);
		for (int x = 0; x < image.getWidth(); ++x)
			for (int y = 0; y < image.getHeight(); ++y)
				if (!interestingColors.contains(image.getRGB(x, y)))
					processed.set(y * image.getWidth() + x);

		ImmutableSet.Builder<Region> builder = ImmutableSet.builder();
		int lastClearBit = 0;
		while ((lastClearBit = processed.nextClearBit(lastClearBit)) != imageSize) {
			int fillY = lastClearBit / image.getWidth(), fillX = lastClearBit % image.getWidth();
			int color = image.getRGB(fillX, fillY);
			List<Point> points = new ArrayList<>();

			//flood fill
			Deque<Point> frontier = new ArrayDeque<>();
			frontier.push(new Point(fillX, fillY));
			while (!frontier.isEmpty()) {
				Point p = frontier.pop();
				int bitIndex = p.row() * image.getWidth() + p.col();
				if (processed.get(bitIndex)) continue;
				if (image.getRGB(p.x, p.y) != color) continue;

				points.add(p);
				processed.set(bitIndex);
				for (int[] n : NEIGHBORHOOD) {
					int nx = p.x + n[0], ny = p.y + n[1];
					int nBitIndex = nx + ny * image.getWidth();
					if (0 <= nx && nx < image.getWidth()&& 0 <= ny && ny < image.getHeight()
							&& !processed.get(nBitIndex))
						frontier.push(new Point(nx, ny));
				}
			}
			assert !points.isEmpty();
			builder.add(new Region(color, points));
		}
		return builder.build();
	}

	public int color() {
		return color;
	}

	public ImmutableList<Point> points() {
		return points;
	}

	public Point centroid() {
		return new Point((int)xStats.getAverage(), (int)yStats.getAverage());
	}

	public Rectangle boundingBox() {
		return new Rectangle(xStats.getMin(), yStats.getMin(),
				xStats.getMax() - xStats.getMin(), yStats.getMax() - yStats.getMin());
	}

	public static final class Point {
		public final int x, y;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		//these are mostly for method references (field refs don't exist)
		public int x() {
			return x;
		}
		public int y() {
			return y;
		}
		//row/col is reversed, of course
		public int row() {
			return y;
		}
		public int col() {
			return x;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Point other = (Point)obj;
			if (this.x != other.x)
				return false;
			if (this.y != other.y)
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			int hash = 3;
			hash = 13 * hash + this.x;
			hash = 13 * hash + this.y;
			return hash;
		}
		@Override
		public String toString() {
			return String.format("(%d, %d)", x, y);
		}
	}
}
