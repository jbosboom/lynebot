package com.jeffreybosboom.lyne;

import com.google.common.collect.ImmutableMap;

/**
 *
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/17/2014
 */
public final class Colors {
	private Colors() {}

	public static final int GAME_BOARDER = rgb(233, 241, 223);
	public static final int TRIANGLE = rgb(168, 219, 168);
	public static final int DIAMOND = rgb(59, 134, 134);
	public static final int SQUARE = rgb(194, 120, 92);
	public static final int OCTAGON = rgb(167, 219, 216);
	public static final int PIP = rgb(121, 189, 154);
	public static final int TERMINAL_CENTER = rgb(233, 241, 223);

	public static final ImmutableMap<Integer, Node.Kind> NODE_COLORS = ImmutableMap.of(TRIANGLE, Node.Kind.TRIANGLE, DIAMOND, Node.Kind.DIAMOND, SQUARE, Node.Kind.SQUARE, OCTAGON, Node.Kind.OCTAGON);

	private static int rgb(int r, int g, int b) {
		return 0xFF << 24 | r << 16 | g << 8 | b;
	}
}
