/*
 * $Id: DottedRule.java 1796 2010-01-28 22:52:27Z scott $
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;


/**
 * Extension of {@link Rule} that maintains a dot position within the
 * rule.
 * <p>
 * Dotted rules are used by {@link EarleyParser Earley parsers} to keep
 * track of how far within a rule processing has succeeded. In a dotted rule,
 * the {@link DottedRule#getActiveCategory() active category} is the first
 * category after the {@link DottedRule#getPosition() dot position}, and is
 * <code>null</code> when processing has fully covered the underlying rule.
 * {@link Edge Edges} test the active category of dotted rules to determine
 * when an edge is active or passive.
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 1796 $
 * @see Rule
 * @see Edge
 */
public class DottedRule extends Rule {
	int position;
	Category activeCategory;
	
	/**
	 * Creates a new dotted rule for the given rule, with a dot position at
	 * the beginning of the rule's right side (position <code>0</code>).
	 * @see DottedRule#DottedRule(Rule, int)
	 */
	public DottedRule(Rule rule) {
		this(rule, 0);
	}
	
	/**
	 * Creates a dotted rule maintaining the dot position within the right side
	 * category sequence of the underlying rule. 
	 * @param rule The underlying rule.
	 * @param position The zero-based position within <code>rule</code> right
	 * side categories where this dotted rule's dot is maintained.
	 * @throws IndexOutOfBoundsException If <code>position &lt; 0</code> or
	 * <code>position</code> is greater than the length of the
	 * {@link Rule#getRight() right side sequence} in <code>rule</code>. 
	 */
	public DottedRule(Rule rule, int position) {
		super(rule.left, rule.right);
		
		if(position < 0 || position > right.length) {
			throw new IndexOutOfBoundsException(
					"illegal position: " + position);
		}
		
		this.position = position;
		
		// determine active category
		activeCategory = (position < right.length) ? right[position] : null;
	}
	
	/**
	 * Creates and returns a new dotted rule exactly like the one provided 
	 * except that its {@link #getPosition() dot position} is advanced by
	 * <code>1</code>.
	 * @param dottedRule The dotted rule whose dot position should be advanced.
	 * @throws IndexOutOfBoundsException If thw dotted rule's dot position
	 * is already at the end of its right side.
	 * @return A new dotted rule wrapping this rule with its position
	 * incremented.
	 * @see #DottedRule(Rule, int)
	 * @since 0.2
	 */
	public static DottedRule advanceDot(DottedRule dottedRule) {
		return new DottedRule(dottedRule, dottedRule.position + 1);
	}
	
	/**
	 * Creates a new start rule for a given seed category.
	 * @param seed The seed category to use.
	 * @throws NullPointerException If the seed category is <code>null</code>.
	 * @throws IllegalArgumentException If the seed category is a
	 * {@link Category#isTerminal() terminal}.
	 * @return A dotted rule that has the {@link Category#START special start
	 * rule} on the {@link #getLeft() left} and the specified <code>seed</code>
	 * on the {@link #getRight() right}. This method is used by Earley parsers
	 * for seeding.
	 * @see Category#START
	 * @see EarleyParser#parse(Iterable, Category)
	 */
	public static DottedRule startRule(Category seed) {
		if(seed == null) {
			throw new NullPointerException("null seed");
		}
		if(seed.terminal) {
			throw new IllegalArgumentException("seed is a terminal: " + seed);
		}
		
		return new DottedRule(new Rule(Category.START, seed), 0);
	}
	
	/**
	 * Gets the dot position within the underlying rule's
	 * {@link Rule#getRight() right side category sequence}.
	 * @return The dot position that was specified for this dotted rule when
	 * it was created.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Gets the active category in the underlying rule, if any.
	 * @return The category at this dotted rule's
	 * {@link #getPosition() dot position} in the underlying rule's
	 * {@link Rule#getRight() right side category sequence}. If this rule's
	 * dot position is already at the end of the right side category sequence,
	 * returns <code>null</code>.
	 */
	public Category getActiveCategory() {
		return activeCategory;
	}
	
	/**
	 * Tests whether this dotted rule is equal to another dotted rule by
	 * comparing their underlying rules and dot positions.
	 * @return <code>true</code> iff the specified object is an instance of
	 * <code>DottedRule</code> and its underlying rule and position are equal
	 * to this dotted rule's rule and position.
	 * @see Rule#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof DottedRule && super.equals(obj)
				&& position == ((DottedRule)obj).position);
	}

	/**
	 * Computes a hash code for this dotted rule based on its underlying rule
	 * and dot position.
	 * @see Rule#hashCode()
	 */
	@Override
	public int hashCode() {
		return (super.hashCode() * (31 + position));
	}

	/**
	 * Gets a string representation of this dotted rule.
	 * @return &quot;<code>S -> NP * VP</code>&quot; for a dotted rule with
	 * an underlying rule <code>S -> NP VP</code> and a dot position
	 * <code>1</code>.
	 * @see Rule#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(left.toString());
		sb.append(" ->");
		
		for(int i = 0; i <= right.length; i++) {
			if(i == position) {
				sb.append(" *"); // insert dot at position
			}
			
			if(i < right.length) {
				sb.append(' '); // space between categories
				sb.append(right[i].toString());
			}
		}
		
		return sb.toString();
	}
}
