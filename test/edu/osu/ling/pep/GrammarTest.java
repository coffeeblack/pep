/*
 * $Id: GrammarTest.java 1805 2010-02-03 22:37:31Z scott $
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

/**
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 1805 $
 */
public class GrammarTest extends PepFixture {

	Grammar g;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		g = new Grammar("test");
		g.addRule(rule1);
		g.addRule(rule2);
		g.addRule(rule3);
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.Grammar#addRule(edu.osu.ling.pep.Rule)}.
	 */
	public final void testAddRule() {
		Rule r = new Rule(Z, X, Y);
		g.addRule(r);
		Assert.assertTrue(g.getRules(Z).contains(r));
		
		try {
			g.addRule(null);
			Assert.fail("able to add null rule");
		}
		catch(NullPointerException expected) {}
	}
	
	public final void testContainsRules() {
		Assert.assertTrue(g.containsRules(rule1.left));
		Assert.assertTrue(g.getRules(rule2.left).contains(rule2));
		Assert.assertFalse(g.getRules(rule3.left).contains(rule2));
	}
	/* TODO fix
	public final void testGetPreterminal() {
		Assert.assertEquals(rule2,
				g.getPreterminals(rule2, rule2.right[0].name, true));
		Assert.assertEquals(null,
				g.getPreterminals(rule2, rule2.right[0].name.toUpperCase(),
						false));
	}*/

	/**
	 * Test method for {@link edu.osu.ling.pep.Grammar#getRules(edu.osu.ling.pep.Category)}.
	 */
	public final void testGetRules() {
		Set<Rule> setOfrules = new HashSet<Rule>();
		setOfrules.add(rule1);
		setOfrules.add(rule2);
		Assert.assertEquals(setOfrules, g.getRules(rule1.left));		
		Assert.assertEquals(setOfrules, g.getRules(rule2.left));
		
		setOfrules.clear();
		setOfrules.add(rule3);
		Assert.assertEquals(setOfrules, g.getRules(rule3.left));
		
		Rule r4 = new Rule(rule3.left, A, X, Y, Z);
		g.addRule(r4);
		setOfrules.add(r4);
		Assert.assertEquals(setOfrules, g.getRules(rule3.left));
	}

}
