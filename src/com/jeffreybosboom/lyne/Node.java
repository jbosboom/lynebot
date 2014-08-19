package com.jeffreybosboom.lyne;

import com.google.common.base.Preconditions;

/**
 * Nodes are the vertices of the puzzle graph.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/7/2014
 */
public final class Node {
	public enum Kind {TRIANGLE(true), DIAMOND(true), SQUARE(true), OCTAGON(false), NONE(false);
		private final boolean colored;
		private Kind(boolean colored) {
			this.colored = colored;
		}
		public boolean isColored() {
			return colored;
		}
	};
	private final int x, y;
	private final Kind kind;
	/**
	 * 1 for terminals, 2 for non-terminals, pips*2 for octagons.
	 */
	private final int desiredEdges;
	private Node(int x, int y, Kind kind, int desiredEdges) {
		this.x = x;
		this.y = y;
		this.kind = kind;
		this.desiredEdges = desiredEdges;
	}

	public static Node terminal(int x, int y, Kind kind) {
		Preconditions.checkArgument(kind.isColored(), "creating terminal of %s", kind);
		return new Node(x, y, kind, 1);
	}

	public static Node nonterminal(int x, int y, Kind kind) {
		Preconditions.checkArgument(kind.isColored(), "creating nonterminal of %s", kind);
		return new Node(x, y, kind, 2);
	}

	public static Node octagon(int x, int y, int pips) {
		return new Node(x, y, Kind.OCTAGON, pips*2);
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public Kind kind() {
		return kind;
	}

	public int edges() {
		return desiredEdges;
	}

	public boolean isTerminal() {
		return edges() == 1;
	}

	@Override
	public String toString() {
		if (kind() == Kind.OCTAGON)
			return Integer.toString(edges()/2);
		char firstLetter = kind().name().charAt(0);
		return Character.toString(isTerminal() ? firstLetter : Character.toLowerCase(firstLetter));
	}
}
