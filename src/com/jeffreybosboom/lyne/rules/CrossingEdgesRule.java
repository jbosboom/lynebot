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

import com.jeffreybosboom.lyne.Node;
import com.jeffreybosboom.lyne.Pair;
import com.jeffreybosboom.lyne.Puzzle;
import java.util.Iterator;

/**
 * Diagonal edges that are not NONE imply the edge they cross must be NONE.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class CrossingEdgesRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Pair<Node, Node>> i = puzzle.edges().iterator(); i.hasNext();) {
			Pair<Node, Node> p = i.next();
			if (p.first.row() == p.second.row() || p.first.col() == p.second.col())
				continue; //no crossing edge
			if (puzzle.possibilities(p.first, p.second).contains(Node.Kind.NONE)) continue;
			//Canonical order and not-same-row/col enforces it's a down-right edge.
			Node ac = puzzle.at(p.first.row()+1, p.first.col()), bc = puzzle.at(p.second.row()-1, p.second.col());
			if (ac != null && bc != null) //does the crossing edge exist?
				puzzle = puzzle.set(ac, bc, Node.Kind.NONE);
		}
		return puzzle;
	}
}
