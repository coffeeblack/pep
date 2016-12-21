/*
 * $Id: Rule.java 2519 2011-02-10 16:01:23Z scott $ 
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import java.util.Arrays;


/**
 * Represents a production rule in a {@link Grammar context-free grammar}.
 * <p>
 * Rules contain a single {@link Category category} on the
 * {@link Rule#getLeft() left side} that produces the series of categories on
 * the {@link Rule#getRight() right side}. 
 * <p>
 * Rules are immutable and cannot be changed once instantiated.
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 2519 $
 * @see Category
 * @see Grammar
 */
public class Rule {
	Category left;
	Category[] right;
	
	/**
	 * Creates a new rule with the specified left side category and series of
	 * categories on the right side.
	 * @param left The left side (trigger) for this production rule.
	 * @param right The right side (productions) licensed for this rule's
	 * left side.
	 * @throws IllegalArgumentException If
	 * <ol>
	 * 	<li>the specified left or right categories are <code>null</code>,</li>
	 * 	<li>the right series is zero-length,</li>
	 * 	<li>the right side contains a <code>null</code> category.</li>
	 * </ol> 
	 */
	public Rule(Category left, Category... right) {
		if(left == null) {
			throw new IllegalArgumentException("empty left category");
		}
		if(left.terminal) {
			throw new IllegalArgumentException("left category is terminal");
		}
		if(right == null || right.length == 0) {
			throw new IllegalArgumentException("no right categories");
		}
		
		// check for nulls on right
		for(Category r : right) {
			if(r == null) {
				throw new IllegalArgumentException(
					"right contains null category: " + Arrays.toString(right));
			}
		}
		
		this.left = left;
		this.right = right;
	}
	
	/**
	 * Gets the left side category of this rule.
	 */
	public Category getLeft() {
		return left;
	}
	
	/**
	 * Gets the series of categories on the right side of this rule.
	 */
	public Category[] getRight() {
		return right;
	}
	
	/**
	 * Tests whether this rule is a pre-terminal production rule. A rule is a
	 * preterminal rule if its right side contains a
	 * {@link Category#isTerminal() terminal category}. 
	 * @return <code>true</code> iff this rule's right side contains a 
	 * terminal category.
	 */
	public boolean isPreterminal() {
		for(Category r : right) {
			if(r.terminal) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Tests whether this rule is a pre-terminal with a right side of length
	 * <code>1</code>.
	 * @see #isPreterminal()
	 * @see #getRight()
	 * @since 0.4
	 */
	public boolean isSingletonPreterminal() {
		return (isPreterminal() && right.length == 1);
	}

	/**
	 * Tests whether this rule is equal to another, with the same left and
	 * right sides.
	 * @return <code>true</code> iff the specified object is an instance of
	 * <code>Rule</code> and its left and right sides are equal to this rule's
	 * left and right sides.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Rule) {
			Rule or = (Rule)obj;			
			return (left.equals(or.left) && Arrays.equals(right, or.right));
		}
		
		return false;
	}

	/**
	 * Compues a hash code for this rule based on its left and right side
	 * categories.
	 */
	@Override
	public int hashCode() {
		return (31 * left.hashCode() * Arrays.hashCode(right));
	}

	/**
	 * Gets a string representation of this rule.
	 * @return &quot;<code>S -> NP VP</code>&quot; for a rule with a left side
	 * category of <code>S</code> and a right side sequence
	 * <code>[NP, VP]</code>.
	 * @see Category#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(left.toString());
		sb.append(" ->");
		
		for(int i = 0; i < right.length; i++) {
			sb.append(' '); // space between categories
			sb.append(right[i].toString());
		}
		
		return sb.toString();
	}
}
