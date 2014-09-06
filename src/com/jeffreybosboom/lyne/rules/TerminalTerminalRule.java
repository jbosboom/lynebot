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
import java.util.function.Predicate;

/**
 * Two terminals cannot be directly connected if there are other nodes of that
 * color in the puzzle.
 *
 * This rule will make all possible deductions on its first execution.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class TerminalTerminalRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Pair<Node, Node>> it = puzzle.terminals().iterator(); it.hasNext();) {
			Pair<Node, Node> terminals = it.next();
			if (puzzle.neighbors(terminals.first).anyMatch(Predicate.isEqual(terminals.second)) &&
					puzzle.nodes().filter(n -> n.kind() == terminals.first.kind()).count() > 2)
				puzzle = puzzle.set(terminals.first, terminals.second, Node.Kind.NONE);
		}
		return puzzle;
	}
}
