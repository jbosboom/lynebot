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

package com.jeffreybosboom.lyne.rules;

import com.google.common.collect.ImmutableSet;
import com.jeffreybosboom.lyne.Node;
import com.jeffreybosboom.lyne.Pair;
import com.jeffreybosboom.lyne.Puzzle;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Edges between octagons and colored nodes may only be of that color or NONE.
 *
 * This rule will make all possible deductions on its first execution.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class ColorOctagonRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Pair<Node, Node>> it =
				Stream.concat(puzzle.edges(), puzzle.edges().map(Pair::opposite)).iterator();
				it.hasNext();) {
			Pair<Node, Node> edge = it.next();
			if (edge.first.kind() == Node.Kind.OCTAGON && edge.second.kind().isColored())
				puzzle = puzzle.restrict(edge.first, edge.second, ImmutableSet.of(edge.second.kind(), Node.Kind.NONE));
		}
		return puzzle;
	}
}
