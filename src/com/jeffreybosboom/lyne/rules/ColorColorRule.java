package com.jeffreybosboom.lyne.rules;

import com.google.common.collect.ImmutableSet;
import com.jeffreybosboom.lyne.Node;
import com.jeffreybosboom.lyne.Pair;
import com.jeffreybosboom.lyne.Puzzle;
import java.util.Iterator;

/**
 * Colored nodes of different colors have no edge between them; colored nodes of
 * the same color have only their color or NONE.
 *
 * This rule will make all possible deductions on its first execution.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class ColorColorRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Pair<Node, Node>> it = puzzle.edges().iterator(); it.hasNext();) {
			Pair<Node, Node> edge = it.next();
			if (!edge.first.kind().isColored() || !edge.second.kind().isColored()) continue;
			if (edge.first.kind() != edge.second.kind())
				puzzle = puzzle.set(edge.first, edge.second, Node.Kind.NONE);
			else
				puzzle = puzzle.restrict(edge.first, edge.second, ImmutableSet.of(edge.first.kind(), Node.Kind.NONE));
		}
		return puzzle;
	}
}
