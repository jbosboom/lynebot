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
