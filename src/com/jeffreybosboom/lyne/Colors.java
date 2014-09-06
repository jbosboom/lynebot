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

/**
 * Constants for Lyne colors, based on the default palette.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/17/2014
 */
public final class Colors {
	private Colors() {}

	public static final int GAME_BORDER = rgb(233, 241, 223);
	public static final int TRIANGLE = rgb(168, 219, 168);
	public static final int DIAMOND = rgb(59, 134, 134);
	public static final int SQUARE = rgb(194, 120, 92);
	public static final int OCTAGON = rgb(167, 219, 216);
	public static final int PIP = rgb(121, 189, 154);
	public static final int TERMINAL_CENTER = rgb(233, 241, 223);

	public static final ImmutableMap<Integer, Node.Kind> NODE_COLORS = ImmutableMap.of(TRIANGLE, Node.Kind.TRIANGLE, DIAMOND, Node.Kind.DIAMOND, SQUARE, Node.Kind.SQUARE, OCTAGON, Node.Kind.OCTAGON);
	public static final ImmutableSet<Integer> LYNE_COLORS = ImmutableSet.of(
			GAME_BORDER, TRIANGLE, DIAMOND, SQUARE, OCTAGON, PIP, TERMINAL_CENTER
	);

	private static int rgb(int r, int g, int b) {
		return 0xFF << 24 | r << 16 | g << 8 | b;
	}
}
