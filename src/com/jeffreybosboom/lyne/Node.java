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

package com.jeffreybosboom.lyne;

import com.google.common.base.Preconditions;
import java.util.Comparator;

/**
 * Nodes are the vertices of the puzzle graph.
 * @author Jeffrey Bosboom <jbosboom@csail.mit.edu>
 * @since 8/7/2014
 */
public final class Node implements Comparable<Node> {
	public enum Kind {TRIANGLE(true), DIAMOND(true), SQUARE(true), OCTAGON(false), NONE(false);
		private final boolean colored;
		private Kind(boolean colored) {
			this.colored = colored;
		}
		public boolean isColored() {
			return colored;
		}
	};
	private final int row, col;
	private final Kind kind;
	/**
	 * 1 for terminals, 2 for non-terminals, pips*2 for octagons.
	 */
	private final int desiredEdges;
	private Node(int row, int col, Kind kind, int desiredEdges) {
		this.row = row;
		this.col = col;
		this.kind = kind;
		this.desiredEdges = desiredEdges;
	}

	public static Node terminal(int row, int col, Kind kind) {
		Preconditions.checkArgument(kind.isColored(), "creating terminal of %s", kind);
		return new Node(row, col, kind, 1);
	}

	public static Node nonterminal(int row, int col, Kind kind) {
		Preconditions.checkArgument(kind.isColored(), "creating nonterminal of %s", kind);
		return new Node(row, col, kind, 2);
	}

	public static Node octagon(int row, int col, int pips) {
		return new Node(row, col, Kind.OCTAGON, pips*2);
	}

	public int row() {
		return row;
	}

	public int col() {
		return col;
	}

	public Kind kind() {
		return kind;
	}

	public int desiredEdges() {
		return desiredEdges;
	}

	public boolean isTerminal() {
		return desiredEdges() == 1;
	}

	private static final Comparator<Node> COMPARATOR =
			Comparator.comparingInt(Node::row)
					.thenComparingInt(Node::col);
	@Override
	public int compareTo(Node o) {
		return COMPARATOR.compare(this, o);
	}

	@Override
	public String toString() {
		if (kind() == Kind.OCTAGON)
			return Integer.toString(desiredEdges()/2);
		char firstLetter = kind().name().charAt(0);
		return Character.toString(isTerminal() ? firstLetter : Character.toLowerCase(firstLetter));
	}
}
