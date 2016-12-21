/*
 * $Id: ParseTreeTest.java 1805 2010-02-03 22:37:31Z scott $
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;



/**
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 1805 $
 */
public class ParseTreeTest extends PepFixture {

	Parse parse;
	Set<ParseTree> parseTrees;
	
	Category VI = new Category("VI"), VT = new Category("VT"),
		VS = new Category("VS"), saw = new Category("saw", true),
			duck = new Category("duck", true),
				her = new Category("her", true),
					he = new Category("he", true);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		grammar = new Grammar("test");
		
		grammar.addRule(new Rule(S, NP, VP));
		grammar.addRule(new Rule(NP, he));
		grammar.addRule(new Rule(NP, her));
		grammar.addRule(new Rule(NP, Det, N));
		grammar.addRule(new Rule(VT, saw));
		grammar.addRule(new Rule(VS, saw));
		grammar.addRule(new Rule(VI, duck));
		grammar.addRule(new Rule(N, duck));
		grammar.addRule(new Rule(Det, her));
		grammar.addRule(new Rule(VP, VT, NP));
		grammar.addRule(new Rule(VP, VS, S));
		grammar.addRule(new Rule(VP, VI));
		
		tokens.clear();
		tokens.add(he.name);
		tokens.add(saw.name);
		tokens.add(her.name);
		tokens.add(duck.name);
		
		EarleyParser parser = new EarleyParser(grammar);
		parse = parser.parse(tokens, S);
		parseTrees = parse.getParseTrees();
	}

	public final void testParseTrees() {
		// structural ambiguity reflected?
		Assert.assertEquals("problem with parse trees: " + parseTrees,
				2, parseTrees.size());
		
		// sub trees
		Set<ParseTree> vpSubTrees = parse.getParseTreesFor(VP, 1, 4);
		Assert.assertEquals("problem with VP sub trees: " + vpSubTrees,
				2, vpSubTrees.size());
		
		// should contain all parse trees at index 4
		for(Edge edge : parse.chart.getEdges(4)) {
			if(edge.origin == 1 && edge.isPassive()
					&& edge.dottedRule.left.equals(VP)) {
				ParseTree pt = parse.getParseTreeFor(edge);
				Assert.assertTrue("VP sub trees does not contain: " + pt,
					vpSubTrees.contains(pt));
			}
		}
		
		// VI "duck" at end
		Set<ParseTree> viSubTrees = parse.getParseTreesFor(VI, 3, 4);
		Assert.assertEquals("problem with VI sub trees: " + viSubTrees,
				1, viSubTrees.size());
		
		Set<ParseTree> npSubTrees = parse.getParseTreesFor(NP, 0, 1);
		Assert.assertEquals("problem with NP sub trees: " + npSubTrees,
				1, npSubTrees.size());
		
		// NP "her duck"
		Set<ParseTree> npSubTrees2 = parse.getParseTreesFor(NP, 2, 4);
		Assert.assertEquals("problem with NP sub trees: " + npSubTrees2,
				1, npSubTrees2.size());
		
		// sentential complement "her duck"
		Set<ParseTree> sSubTrees = parse.getParseTreesFor(S, 2, 4);
		Assert.assertEquals("problem with S sub trees: " + sSubTrees,
				1, sSubTrees.size());
		
		ParseTree sSubTree = sSubTrees.iterator().next();
		ParseTree[] sChildren = sSubTree.getChildren();
		//Iterator<ParseTree> sci = sChildren.iterator();
		Assert.assertEquals(NP, sChildren[0].getNode());
		ParseTree sVPSubTree = sChildren[1];
		Assert.assertEquals(VP, sVPSubTree.getNode());
		ParseTree viSubTree = sVPSubTree.getChildren()[0];
		Assert.assertEquals(VI, viSubTree.getNode());
		ParseTree duckSubTree = viSubTree.getChildren()[0];
		Assert.assertEquals(duck, duckSubTree.getNode());
		
		// back up
		Assert.assertEquals(viSubTree, duckSubTree.getParent());
		Assert.assertEquals(sVPSubTree, viSubTree.getParent());
		Assert.assertEquals(sSubTree, sVPSubTree.getParent());
		Assert.assertTrue(sSubTree.getParent() == null);
		
		// wrong stuff in seed
		Assert.assertEquals(Collections.emptySet(),
				parse.getParseTreesFor(NP, 0, tokens.size()));
	}
	
	public final void testNewParseTree() {
		Edge startEdge = new Edge(DottedRule.startRule(S), 0);
		startEdge = Edge.complete(startEdge, 
				new Edge(new DottedRule(new Rule(S, NP, VP), 2), 0));
		
		ParseTree startTree = ParseTree.newParseTree(startEdge);
		Assert.assertTrue(startTree.getChildren() == null);
		
		Edge sEdge = new Edge(new DottedRule(new Rule(S, NP, VP)), 0);
		sEdge = Edge.complete(sEdge,
				new Edge(new DottedRule(new Rule(NP, Det, N), 2), 0));
		sEdge = Edge.complete(sEdge,
				new Edge(new DottedRule(new Rule(VP, VT, NP), 2), 3));
		
		ParseTree sTree = ParseTree.newParseTree(sEdge, null);
		ParseTree[] sChildren = sTree.getChildren();
		
		Assert.assertEquals(NP, sChildren[0].getNode());
		Assert.assertEquals(VP, sChildren[1].getNode());
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.ParseTree#getNode()}.
	 */
	public final void testGetParent() {
		for(ParseTree pt : parseTrees) {
			Assert.assertEquals(S, pt.getNode());
		}
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.ParseTree#getChildren()}.
	 */
	public final void testGetChildren() throws PepException {
		for(ParseTree pt : parseTrees) {
			ParseTree[] i = pt.getChildren();
			Assert.assertEquals(NP, i[0].getNode());
			Assert.assertEquals(VP, i[1].getNode());
		}
		
		// test for Jim Slattery's bug
		Grammar g = new Grammar("g");
		g.addRule(new Rule(S, NP, NP));
		g.addRule(new Rule(NP, he));
		
		List<String> t = new ArrayList<String>(2);
		t.add("he");
		t.add("he");
		
		EarleyParser p = new EarleyParser(g);
		Parse prse = p.parse(t, S);
		ParseTree tree = prse.getParseTrees().iterator().next();
		
		int npCount = 0;
		for(ParseTree c : tree.getChildren()) {
			for(ParseTree x : c.getChildren()) {
				if(x.getNode().equals(he)) {
					npCount++;
				}
			}
		}
		
		Assert.assertEquals(2, npCount);
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.ParseTree#equals(java.lang.Object)}.
	 */
	public final void testEqualsObject() {
		ParseTree test = new ParseTree(edge1.dottedRule.left, null);
		for(ParseTree pt : parseTrees) {
			Assert.assertFalse(test.equals(pt));
		}
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.ParseTree#toString()}.
	 */
	public final void testToString() {
		String s1 = "[S[NP[he]][VP[VT[saw]][NP[Det[her]][N[duck]]]]]",
			s2 = "[S[NP[he]][VP[VS[saw]][S[NP[her]][VP[VI[duck]]]]]]";
		
		for(ParseTree pt : parseTrees) {
			Assert.assertTrue("problem with toString(): " + pt.toString(),
					pt.toString().equals(s1) || pt.toString().equals(s2));
		}
	}

}
