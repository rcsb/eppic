package eppic.assembly;

import org.biojava.nbio.structure.Chain;


/**
 * A vertex representing a single chain.
 * Uniquely identified by opId and chainId.
 * 
 * 
 * @author spencer
 *
 */
public class ChainVertex implements ChainVertexInterface {
	// Primary Key:
	private int opId; // operator to generate this position within the unit cell
	
	private Chain c;
	 
	/**
	 * Default constructor for factory methods. After construction, at a minimum
	 * call {@link #setChain(Chain)} and {@link #setOpId(int)}.
	 * Use {@link #ChainVertex(Chain, int)} whenever possible.
	 */
	public ChainVertex() {
		this(null,-1);
	}
	
	public ChainVertex(Chain c, int opId) {
		this.c = c;
		this.opId = opId;
	}


	/** Copy constructor */
	public ChainVertex(ChainVertex vert) {
		this.opId = vert.opId;
		this.c = vert.c;
	}

	@Override
	public int getOpId() {
		return opId;
	}
	
	public void setOpId(int i) {
		this.opId = i;
	}
	
	@Override
	public String getChainId() {
		return c.getChainID();
	}
	
	public Chain getChain() {
		return c;
	}
	public void setChain(Chain c) {
		this.c = c;
	}
	
	@Override
	public String toString() {
		return getChainId()+"_"+opId;
	}
	/**
	 * Hash key based on chain and op
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getChainId() == null) ? 0 : getChainId().hashCode());
		result = prime * result + opId;
		return result;
	}
	/**
	 * Equality based on chain and op
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChainVertex other = (ChainVertex) obj;
		if (getChainId() == null) {
			if (other.getChainId() != null)
				return false;
		} else if (!getChainId().equals(other.getChainId()))
			return false;
		if (opId != other.opId)
			return false;
		return true;
	}

	@Override
	public int getEntityId() {
		return c.getCompound().getMolId();
	}

}