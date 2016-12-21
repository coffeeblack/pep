/*
 * $Id: EarleyParserTest.java 2521 2011-02-10 20:15:13Z scott $
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import static edu.osu.ling.pep.ParserOption.IGNORE_TERMINAL_CASE;
import static edu.osu.ling.pep.ParserOption.PREDICT_FOR_PRETERMINALS;
import static edu.osu.ling.pep.Status.ACCEPT;
import static edu.osu.ling.pep.Status.ERROR;
import static edu.osu.ling.pep.Status.REJECT;

import java.util.Arrays;
import java.util.Set;

import junit.framework.Assert;


/**
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 2521 $
 */
public class EarleyParserTest extends PepFixture 
		implements ParserListener {

	EarleyParser earleyParser;
	Parse parse;
	
	Grammar emptyGrammar;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		earleyParser = new EarleyParser(grammar);
		emptyGrammar = new Grammar("empty");
	}

	public final void testGetGrammar() {
		Assert.assertEquals(grammar, earleyParser.getGrammar());
	}
	
	public final void testSetGrammar() {
		earleyParser.setGrammar(emptyGrammar);
	}
	
	public final void testRecognize() throws PepException {
		Assert.assertEquals(ACCEPT, earleyParser.recognize(tokens, seed));
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.EarleyParser#parse(Iterable, Category)}.
	 */
	public final void testParse() throws PepException {
		parse = earleyParser.parse(tokens, S);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// make sure this rejects but does not cause errors
		earleyParser.setGrammar(emptyGrammar);
		parse = earleyParser.parse(tokens, S);
		Assert.assertEquals(REJECT, parse.getStatus());
		earleyParser.setGrammar(grammar);
		
		// see that this makes no difference
		earleyParser.setOption(PREDICT_FOR_PRETERMINALS, true);
		parse = earleyParser.parse(tokens, S);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// change string to "the the left"
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"the", "the", "left"}), S);
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// change string to "girl the left"
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"girl", "the", "left"}), S);
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// change string to "left the girl"
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"left", "the", "girl"}), S);
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// change string to "the the the"
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"the", "the", "the"}), S);
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// change string to "the boy" but seed to NP
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"the", "boy"}), NP);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// change string to "left" but seed to VP
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"left"}), VP);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// change string to "boy" but seed to N
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"boy"}), N);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// change string to "the girl left"
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"the", "girl", "left"}), S);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// change string to "the <null> left"
		earleyParser.listener = this;
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"the", null, "left"}), S);
		Assert.assertEquals(ERROR, parse.getStatus());
		earleyParser.listener = null;
		
		// test option for ignoring preterminal case
		earleyParser.setOption(IGNORE_TERMINAL_CASE, true);
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"THE", "BOY", "LEFT"}), S);
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		earleyParser.setOption(IGNORE_TERMINAL_CASE, false);
		parse = earleyParser.parse(
				Arrays.asList(new String[] {"THE", "BOY", "LEFT"}), S);
		Assert.assertEquals(REJECT, parse.getStatus());
		
		try {
			parse = earleyParser.parse(
					Arrays.asList(new String[] {}), S);
			Assert.fail("parse with no tokens without error");
		}
		catch(PepException expected) {}
		
		// change string to "the '' left"
		try {
			parse = earleyParser.parse(
					Arrays.asList(new String[] {"the", "", "left"}), S);
		}
		catch(PepException pe) {
			Assert.fail("parsing for empty token threw exception: " + pe);
		}
		
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// same thing, only with listener method
		earleyParser.listener = this;
		parse = earleyParser.parse(
			Arrays.asList(new String[] {"the", "", "left"}), S);
		
		Assert.assertEquals(REJECT, parse.getStatus());
		
		// add epsilon production
		grammar.addRule(new Rule(N, new Category("", true)));
		earleyParser.listener = null;
		try {
			parse = earleyParser.parse(
					Arrays.asList(new String[] {"the", "", "left"}), S);
		}
		catch(PepException pe) {
			Assert.fail("parsing for empty token threw exception: " + pe);
		}
		
		Assert.assertEquals(ACCEPT, parse.getStatus());
		
		// test for mixed terminal/nonterminal grammar
		earleyParser.setGrammar(mixed);
		for(String s : new String[]{"a a", "b a", "a b a"}) {
			try {
				parse = earleyParser.parse(s, " ", A);
			}
			catch(PepException pe) {
				Assert.fail("parsing " + s + " threw exception: " + pe);
			}
			
			Assert.assertEquals(ACCEPT, parse.getStatus());
		}
		
		try {
			parse = earleyParser.parse("a b", " ", A);
		}
		catch(PepException pe) {
			Assert.fail("parsing a b threw exception: " + pe);
		}
		
		Assert.assertEquals(REJECT, parse.getStatus());
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.EarleyParser#predict(Chart, int)}.
	 */
	public final void testPredict() {
		Chart chart = new Chart();
		chart.addEdge(0, new Edge(DottedRule.startRule(seed), 0));
		
		earleyParser.predict(chart, 0);
		
		Set<Edge> zeroEdges = chart.getEdges(0); 
		Assert.assertTrue("rule S -> NP VP not predicted",
				zeroEdges.contains(
						new Edge(new DottedRule(new Rule(S, NP, VP)), 0)));
		Assert.assertTrue("rule NP -> Det N not predicted",
				zeroEdges.contains(
					new Edge(new DottedRule(new Rule(NP, Det, N)), 0)));
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.EarleyParser#scan(Chart, int, String)}.
	 */
	public final void testScan() throws PepException {
		Chart chart = new Chart();
		chart.addEdge(0, new Edge(DottedRule.startRule(seed), 0));
		
		earleyParser.predict(chart, 0);
		String zeroToken = tokens.get(0);
		earleyParser.scan(chart, 0, zeroToken);
		Set<Edge> zeroEdges = chart.getEdges(1);
		Edge scanEdge = new Edge(new DottedRule(new Rule(Det, the), 0), 0);
		scanEdge = Edge.scan(scanEdge, zeroToken, true);
		
		Assert.assertTrue("passive edge Det -> the not scanned", 
				zeroEdges.contains(scanEdge));
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.EarleyParser#complete(Chart, int)}.
	 */
	public final void testComplete() throws PepException {
		Chart chart = new Chart();
		chart.addEdge(0, new Edge(DottedRule.startRule(seed), 0));
		
		earleyParser.predict(chart, 0);
		earleyParser.scan(chart, 0, tokens.get(0));
		earleyParser.complete(chart, 1);
		
		Edge expected = new Edge(new DottedRule(new Rule(NP, Det, N), 1), 0);
		for(Edge e : chart.getEdges(1)) {
			if(e.dottedRule.equals(expected.dottedRule)
					&& e.origin == expected.origin) {
				return;
			}
		}
		
		Assert.fail("rule NP -> Det * N not completed");
	}
	
	@SuppressWarnings("unused")
	public void edgeCompleted(EdgeEvent edgeEvent) {}

	@SuppressWarnings("unused")
	public void edgePredicted(EdgeEvent edgeEvent) {}

	public void parseComplete(ParseEvent parseEvent) {
		parse = parseEvent.parse;
	}

	@SuppressWarnings("unused")
	public void parseMessage(ParseEvent parseEvent, String message) {
		System.err.println(message);		
	}
	
	@SuppressWarnings("unused")
	public void parseError(ParseErrorEvent parseErrorEvent)
		throws PepException {}

	@SuppressWarnings("unused")
	public void parserSeeded(EdgeEvent edgeEvent) {}

	@SuppressWarnings("unused")
	public void edgeScanned(EdgeEvent tokenEvent) {}

	@SuppressWarnings("unused")
	public void optionSet(ParserOptionEvent optionEvent) {}
}
