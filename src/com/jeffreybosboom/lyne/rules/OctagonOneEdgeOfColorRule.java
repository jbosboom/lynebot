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

import com.google.common.collect.EnumMultiset;
import com.jeffreybosboom.lyne.Node;
import com.jeffreybosboom.lyne.Puzzle;
import java.util.Iterator;

/**
 * If an octagon has only one edge for a particular color, that color can be
 * removed from that edge.  (A path of that color could enter the octagon, but
 * not leave.)
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class OctagonOneEdgeOfColorRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Node> it = puzzle.nodes()
				.filter(n -> n.kind() == Node.Kind.OCTAGON)
				.iterator(); it.hasNext();) {
			Node octagon = it.next();
			EnumMultiset<Node.Kind> counter = EnumMultiset.create(Node.Kind.class);
			Puzzle puzzle_ = puzzle;
			puzzle.neighbors(octagon)
					.map(n -> puzzle_.possibilities(octagon, n))
					.forEachOrdered(counter::addAll);
			for (Node.Kind k : Node.Kind.values())
				if (k.isColored() && counter.count(k) == 1)
					//this is inefficient, but should be rare.
					for (Iterator<Node> it2 = puzzle.neighbors(octagon).iterator();
							it2.hasNext();)
						puzzle = puzzle.remove(octagon, it2.next(), k);
		}
		return puzzle;
	}
}
