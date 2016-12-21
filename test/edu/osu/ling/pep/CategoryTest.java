/*
 * $Id: CategoryTest.java 562 2007-08-16 15:16:13Z scott $
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import junit.framework.Assert;


/**
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 562 $
 */
public class CategoryTest extends PepFixture {

	/**
	 * Test method for {@link edu.osu.ling.pep.Category#hashCode()}.
	 */
	public final void testHashCode() {
		Assert.assertEquals(
				31 * A.name.hashCode() * Boolean.valueOf(A.terminal).hashCode(),
				A.hashCode());
	}
	
	/**
	 * Test method for {@link edu.osu.ling.pep.Category#Category(java.lang.String)}.
	 */
	public final void testCategoryString() {
		try {
			new Category("", false);
			Assert.fail("able to create non-terminal category with empty name");
		}
		catch(IllegalArgumentException expected) {}
		
		try {
			new Category("", true);
		}
		catch(IllegalArgumentException iae) {
			Assert.fail("creating terminal category with empty name threw: "
					+ iae);
		}
		
		try {
			new Category(null);
			Assert.fail("able to create category with null name");
		}
		catch(IllegalArgumentException expected) {}
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.Category#getName()}.
	 */
	public final void testGetName() {
		Assert.assertEquals("A", A.getName());
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.Category#isTerminal()}.
	 */
	public final void testIsTerminal() {
		Assert.assertEquals(false, A.isTerminal());
		Assert.assertEquals(true, a.isTerminal());
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.Category#equals(java.lang.Object)}.
	 */
	public final void testEqualsObject() {
		Assert.assertEquals(A, new Category("A", false));
		Assert.assertFalse(A.equals(new Category("B")));
	}

	/**
	 * Test method for {@link edu.osu.ling.pep.Category#toString()}.
	 */
	public final void testToString() {
		Assert.assertEquals("A", A.toString());
	}

}
