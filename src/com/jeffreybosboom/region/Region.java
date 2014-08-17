package com.jeffreybosboom.region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import javax.imageio.ImageIO;

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
	private final ImmutableList<Point> pixels;
	//TODO: summary stats?
	private Region(int color, List<Point> pixels) {
		this.color = color;
		this.pixels = ImmutableList.copyOf(pixels);
	}

	private static final int[][] NEIGHBORHOOD = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1}, {0, 1},
		{1, -1}, {1, 0}, {1, 1},
	};
	public static ImmutableSet<Region> connectedComponents(BufferedImage image) {
		ImmutableSet.Builder<Region> builder = ImmutableSet.builder();
		final int imageSize = image.getWidth() * image.getHeight();
		BitSet processed = new BitSet(imageSize);
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
				int bitIndex = p.x + p.y * image.getWidth();
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

	public static final class Point {
		public final int x, y;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
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
	}

	public static void main(String[] args) throws IOException {
		BufferedImage image = ImageIO.read(new File("266010_2014-06-29_00001.png"));
		System.out.println(image.getRGB(408, 759));
		ImmutableSet<Region> regions = Region.connectedComponents(image);
		int baseRed = -4032420;
		System.out.println(regions.stream().filter(r -> r.color == baseRed).count());
	}
}
