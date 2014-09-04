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
