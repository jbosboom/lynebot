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
		//TODO: actually need to check crossing edge is assigned etc
		for (Iterator<Pair<Node, Node>> i = puzzle.pairs().iterator(); i.hasNext();) {
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
