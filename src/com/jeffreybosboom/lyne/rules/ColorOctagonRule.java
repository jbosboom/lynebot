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
