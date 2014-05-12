package org.reactome.convert.common;

/**
 * Helper class to save two linked objects
 * @author andreash
 *
 */
public class Pair<E, K> {
	private E member1;
	private K member2;
	
	/**
	 * Create a Pair and initialise it with the two given Members
	 * @param member1 The first member
	 * @param member2 The second member
	 */
	public Pair(E member1, K member2) {
		this.member1 = member1;
		this.member2 = member2;
	}

	/**
	 * Create an empty Pair
	 */
	public Pair() {
		this.member1 = null;
		this.member2 = null;
	}
	
	/**
	 * Returns the first member of the Pair
	 * @return Member 1
	 */
	public E getMember1() {
		return member1;
	}
	
	/**
	 * Returns the second member of the Pair
	 * @return Member 2
	 */
	public K getMember2() {
		return member2;
	}
	
	/**
	 * Sets the first Member of the Pair
	 * @param member1 The first Member
	 */
	public void setMember1(E member1) {
		this.member1 = member1;
	}

	/**
	 * Sets the second Member of the Pair
	 * @param member2 The second Member
	 */
	public void setMember2(K member2) {
		this.member2 = member2;
	}
	
	/**
	 * A pair is equal to another pair if both members match. <code>Pair(null, null)</code> would match
	 * another <code>Pair(null, null)</code> as well as <code>Pair("test", "test")</code> would match a
	 * <code>Pair("test", "test")</code>.
	 * @param obj the reference object with which to compare.
	 * @return <code>true</code> if this object is the same as the obj argument; <code>false</code>
	 *         otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof Pair) {
			Pair otherObj = (Pair)obj;
			if ((this.member1 == null ^ otherObj.getMember1()==null) || (this.member2 == null ^ otherObj.getMember2()==null)) {
				return false;
			}
			if(this.member1 == null && otherObj.getMember1() == null || this.member1.equals(otherObj.getMember1())) {
				if (this.member2 == null && otherObj.getMember2() == null || this.member2.equals(otherObj.getMember2()))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the String representation of the Pair
	 * @return a String representation of the Object
	 */
	public String toString() {
		return "[" + ((member1 == null) ? "null" : member1.toString()) + "<->"
				+ ((member2 == null) ? "null" : member2.toString()) + "]";
	}
}
