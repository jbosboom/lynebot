package com.jeffreybosboom.lyne.rules;

import com.google.common.collect.ImmutableSet;
import com.jeffreybosboom.lyne.ContradictionException;
import com.jeffreybosboom.lyne.Node;
import com.jeffreybosboom.lyne.Puzzle;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Applies desired-edges inference rules to all nodes:
 * <ul>
 * <li> If the node desires N edges and there are N edges with only colored
 * possibilities, all other edges are set to NONE.
 * <li> If the node desires N edges and there are N-K edges with only
 * colored possibilities and K unknown edges, NONE is removed from all
 * unknown edges.
 * </ul>
 *
 * This rule may require multiple applications to make all inferences it can on
 * a particular puzzle.  Changes by other rules may enable additional inferences.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 9/4/2014
 */
public final class DesiredEdgesRule implements InferenceRule {
	@Override
	public Puzzle apply(Puzzle puzzle) {
		for (Iterator<Node> i = puzzle.nodes().iterator(); i.hasNext();) {
			Node a = i.next();
			List<Node> neighbors = puzzle.neighbors(a).collect(Collectors.toList());
			int knownColored = 0, knownNone = 0;
			for (Node n : neighbors) {
				ImmutableSet<Node.Kind> possibilities = puzzle.possibilities(a, n);
				if (!possibilities.contains(Node.Kind.NONE))
					++knownColored;
				if (possibilities.stream().noneMatch(Node.Kind::isColored))
					++knownNone;
			}
			int unknown = neighbors.size() - knownColored - knownNone;

			if (knownColored > a.desiredEdges())
				throw new ContradictionException();
			if (knownColored + unknown < a.desiredEdges())
				throw new ContradictionException();
			if (unknown == 0) continue;

			//All unknown possibilities are NONE.
			if (knownColored == a.desiredEdges())
				for (Node n : neighbors) {
					ImmutableSet<Node.Kind> possibilities = puzzle.possibilities(a, n);
					if (possibilities.contains(Node.Kind.NONE) && possibilities.stream().anyMatch(Node.Kind::isColored))
						puzzle = puzzle.set(a, n, Node.Kind.NONE);
				}
			//All unknown possibilities are not NONE (but we don't know which color).
			else if (knownColored + unknown == a.desiredEdges())
				for (Node n : neighbors) {
					ImmutableSet<Node.Kind> possibilities = puzzle.possibilities(a, n);
					if (possibilities.contains(Node.Kind.NONE) && possibilities.stream().anyMatch(Node.Kind::isColored))
						puzzle = puzzle.remove(a, n, Node.Kind.NONE);
				}
		}
		return puzzle;
	}
}
