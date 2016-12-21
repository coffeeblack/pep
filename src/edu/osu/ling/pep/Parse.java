/*
 * $Id: Parse.java 2520 2011-02-10 16:05:34Z scott $ 
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import static edu.osu.ling.pep.Category.START;
import static edu.osu.ling.pep.Status.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Represents a parse completed by an {@link EarleyParser Earley parser}.
 * <p>
 * A parse contains the string ({@link List list} of
 * {@link #getTokens() tokens}) parsed, the {@link #getSeed() seed category}
 * and completed {@link Chart chart}. The {@link #getStatus() status} of the
 * completed parse can also be determined.
 * <p>
 * {@link #getParseTreesFor(Category, int, int) Parse trees} can be retrieved
 * for any {@link Category} in the parse at a given string index and origin
 * position. Parses allow the fully derived parse trees for the seed category
 * to be obtained using {@link #getParseTrees()}. For a parse of a string
 * using a grammar that permits structural or lexical ambiguity, the methods
 * for fetching parse trees will return sets that contain more than one element.
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 2520 $
 * @see EarleyParser
 * @see Chart
 */
public class Parse {
	List<String> tokens;
	Category seed;
	Chart chart;
	boolean error;
	
	private Set<ParseTree> parseTrees;
	
	/**
	 * Creates a new parse for the given seed category and 
	 * chart. This constructor is used for parses that complete without errors.
	 * @see Parse#Parse(Category, Chart, boolean)
	 */
	Parse(Category seed, Chart chart) {
		this(seed, chart, false);
	}
	
	/**
	 * Creates a new parse for the given seed category,
	 * chart, and error status.
	 * @param seed The seed category for this parse.
	 * @param chart The completed chart for this parse.
	 * @param error Whether an error occurred while parsing.
	 */
	Parse(Category seed, Chart chart, boolean error) {
		this.seed = seed;
		this.chart = chart;
		this.error = error;
		
		tokens = new ArrayList<String>();
	}
	
	/**
	 * Gets the tokens parsed.
	 */
	public List<String> getTokens() {
		return Collections.unmodifiableList(tokens);
	}
	
	/**
	 * Gets the seed category for this parse.
	 */
	public Category getSeed() {
		return seed;
	}
	
	/**
	 * Gets the completed chart for this parse. 
	 */
	public Chart getChart() {
		return chart;
	}
	
	Set<Edge> getCompletedEdges(Category category, int origin, int index) {
		Set<Edge> edges = chart.edgeSets.get(index);
		if(edges == null || edges.isEmpty()) { // any edges at this index?
			return Collections.emptySet();
		}
		
		Set<Edge> es = new HashSet<Edge>();
		
		for(Edge e : edges) {
			if(e.origin == origin && e.isPassive() && 
					e.dottedRule.left.equals(category)) {
				es.add(e);
			}
		}
		
		return es;
	}
	
	/**
	 * Gets the status of this parse. A parse {@link Status#ACCEPT accepts}
	 * a {@link #getTokens() string} when its {@link #getChart() completed
	 * chart} contains an edge where the following conditions hold:
	 * <ol>
	 * 	<li>its index is the last {@link Chart#indices() index} in the
	 * 		chart,</li>
	 * 	<li>it is {@link Edge#isPassive() passive},</li>
	 * 	<li>its {@link Edge#getOrigin() origin} is the beginning of the string
	 * 		(position <code>0</code>), and</li>
	 *	<li>the {@link Rule#getLeft() left side} of its
	 *		{@link Edge#getDottedRule() dotted rule} is the same as the
	 *		start category that {@link #getSeed() seeded} the parse.</li>
	 * </ol>
	 * @return {@link Status#ACCEPT} for accepted strings,
	 * {@link Status#REJECT} for rejected ones. The status {@link Status#ERROR}
	 * is returned if an error occurred during the parse.
	 */
	public Status getStatus() {
		return error
			? ERROR : getCompletedEdges(START, 0, tokens.size()).isEmpty()
			? REJECT : ACCEPT;
	}
	
	/**
	 * Gets completed parse trees for the seed category spanning the entire
	 * input string. This method returns valid derivations (if any) that the
	 * parse found for the seed category. 
	 * @return A set of parse trees for the {@link #getSeed() seed} category
	 * spanning the entire string, or the {@link Collections#emptySet() empty
	 * set} if none were derivable. In the case that
	 * the grammar used in this parse permits ambiguity for the string parsed,
	 * this set will contain more than one member. If this parse's
	 * {@link #getStatus() status} is {@link Status#ERROR}, the 
	 * {@link Collections#emptySet() empty set} is returned.
	 * @see #getParseTreesFor(Category, int, int)
	 * @since 0.2
	 */
	public Set<ParseTree> getParseTrees() {
		if(parseTrees == null) {
			if(error) {
				parseTrees = Collections.emptySet();
			}
			else {
				parseTrees = getParseTreesFor(START, 0, tokens.size());
			}
		}
		
		return parseTrees;
	}
	
	/**
	 * Gets a parse tree corresponding to the given edge.
	 * @param edge The edge to find a parse tree for.
	 * @return The parse tree corresponding to the specified edge. If the
	 * edge is not contained in this parse's {@link #getChart() chart}, returns
	 * <code>null</code>. The parse tree returned will be the same as
	 * calling
	 * <blockquote><code>ParseTree.newParseTree(edge);</code>
	 * </blockquote>
	 * @throws NullPointerException If <code>edge</code> is <code>null</code>.
	 * @see ParseTree#newParseTree(Edge)
	 * @see Chart#contains(Edge)
	 * @since 0.2
	 */
	public ParseTree getParseTreeFor(Edge edge) {
		if(edge == null) {
			throw new NullPointerException("edge is null");
		}
		if(!chart.contains(edge)) {
			return null;
		}
		
		return ParseTree.newParseTree(edge);
	}
	
	/**
	 * Gets the parse trees derived during this parse with the specified 
	 * category as their {@link ParseTree#getNode() parent}'s left side. This
	 * parse's chart must contain a completed edge at the specified index that
	 * starts at the specified origin with the correct category as its left
	 * side.
	 * <p>
	 * For example, the string <em>Mary saw her duck</em> parsed using the
	 * <code>tiny</code> grammar included in samples will produce a parse that
	 * contains two subtrees for the category <code>VP</code> at origin
	 * position <code>1</code> and index <code>4</code>. This is because the
	 * derived <code>VP</code> categories for <em>saw her duck</em> both start
	 * at <code>1</code> and end at <code>4</code>. To retrieve these subtrees
	 * from the parse, use
	 * <blockquote><code>Parse.getParseTreesFor(VP, 1, 4)<code></blockquote>
	 * where <code>VP</code> represents an instance of {@link Category} whose
	 * {@link Category#getName() name} is &quot;VP&quot;.
	 * </p>
	 * @param category The category of the parse tree's
	 * {@link ParseTree#getNode() parent}. This is the left side of the 
	 * edge's dotted rule.
	 * @param origin The origin position of the edge to find parse trees for.
	 * @param index The string index position of the edge to find parse trees
	 * for. This is the end position of the subtree.
	 * @return A set of parse trees for the given category at the given
	 * origin and string index position, or the {@link Collections#emptySet()
	 * empty set} if no edges match.
	 * @throws NullPointerException If <code>category</code> is
	 * <code>null</code>.
	 * @since 0.2
	 */
	public Set<ParseTree> getParseTreesFor(Category category, int origin,
			int index) {
		if(category == null) {
			throw new NullPointerException("null category");
		}
		
		Set<ParseTree> trees = new HashSet<ParseTree>();
		
		for(Edge e : getCompletedEdges(category, origin, index)) {
			trees.add(ParseTree.newParseTree(e));
		}
		
		return trees;
	}

	/**
	 * Tests whether this parse equals another by comparing their tokens,
	 * seed categories, and completed charts.
	 * @return <code>true</code> iff the given object is an instance of
	 * <code>Parse</code> and its tokens, seed category, and chart are
	 * equal to those of this parse.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Parse) {
			Parse op = (Parse)obj;
			return (error == op.error && tokens.equals(op.tokens)
					&& seed.equals(op.seed) && chart.equals(op.chart));
		}
		
		return false;
	}
	
	/**
	 * Computes a hash code for this parse based on its tokens, seed category,
	 * and chart.
	 * @see List#hashCode()
	 * @see Category#hashCode()
	 * @see Chart#hashCode()
	 */
	@Override
	public int hashCode() {
		return (31 * tokens.hashCode() * seed.hashCode() * chart.hashCode());
	}

	/**
	 * Gets a string representation of this parse.
	 * @return &quot;<code>ACCEPT: S -> [the, boy, left] (1)</code>&quot; for an 
	 * {@link #getStatus() accepted} parse of &quot;the boy left&quot; with
	 * seed category <code>S</code> and one possible {@link #getParseTrees parse
	 * tree}.
	 */
	@Override
	public String toString() {
		Status status = getStatus();
		StringBuilder sb = new StringBuilder(status.toString());
		
		sb.append(": ");
		sb.append(seed);
		sb.append(" -> ");
		sb.append(tokens);
		
		if(status.equals(ACCEPT)) {
			sb.append(" (");
			sb.append(getParseTrees().size());
			sb.append(')');
		}
		
		return sb.toString();
	}
}
