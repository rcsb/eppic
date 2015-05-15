package eppic.assembly;


/**
 * A bean to contain the description of an assembly: 
 * macromolecular size, symmetry, composition and stoichiometry
 * @author duarte_j
 *
 */
public class AssemblyDescription {
	
	private int size;
	private String symmetry;
	private String composition;
	private String stoichiometry;
	
	public AssemblyDescription(int size, String symmetry, String composition, String stoichiometry) {
		this.size = size;
		this.symmetry = symmetry;
		this.composition = composition;
		this.stoichiometry = stoichiometry;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getSymmetry() {
		return symmetry;
	}

	public void setSymmetry(String symmetry) {
		this.symmetry = symmetry;
	}

	public String getComposition() {
		return composition;
	}

	public void setComposition(String composition) {
		this.composition = composition;
	}

	public String getStoichiometry() {
		return stoichiometry;
	}

	public void setStoichiometry(String stoichiometry) {
		this.stoichiometry = stoichiometry;
	}

	public String toString() {
		return stoichiometry+"["+symmetry+"]";
	}
	
}