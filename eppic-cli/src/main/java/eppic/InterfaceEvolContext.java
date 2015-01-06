package eppic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.math.random.RandomDataImpl;
import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.contact.StructureInterface;
import org.biojava.bio.structure.io.CompoundFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eppic.predictors.EvolCoreRimPredictor;
import eppic.predictors.EvolCoreSurfacePredictor;

public class InterfaceEvolContext implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceEvolContext.class);
	

	// the 2 sides of the interface
	public static final int FIRST  = 0;
	public static final int SECOND = 1;

	private InterfaceEvolContextList parent;
	
	private StructureInterface interf;

	private EvolCoreRimPredictor evolCoreRimPredictor;
	private EvolCoreSurfacePredictor evolCoreSurfacePredictor;


	
	
	public InterfaceEvolContext(StructureInterface interf, InterfaceEvolContextList parent) {
		this.interf = interf;
		this.parent = parent;
	}

	public StructureInterface getInterface() {
		return interf;
	}
	
	public ChainEvolContext getFirstChainEvolContext() {
		return parent.getChainEvolContext(interf.getMoleculeIds().getFirst());
	}
	
	public ChainEvolContext getSecondChainEvolContext() {
		return parent.getChainEvolContext(interf.getMoleculeIds().getSecond());
	}
	
	public ChainEvolContext getChainEvolContext(int molecId) {
		if (molecId==FIRST) return getFirstChainEvolContext();
		if (molecId==SECOND) return getSecondChainEvolContext();
		return null;
	}
	
	public EvolCoreRimPredictor getEvolCoreRimPredictor() {
		return evolCoreRimPredictor;
	}
	
	public void setEvolCoreRimPredictor(EvolCoreRimPredictor evolCoreRimPredictor) {
		this.evolCoreRimPredictor = evolCoreRimPredictor;
	}
	
	public EvolCoreSurfacePredictor getEvolCoreSurfacePredictor() {
		return evolCoreSurfacePredictor;
	}
	
	public void setEvolCoreSurfacePredictor(EvolCoreSurfacePredictor evolCoreSurfacePredictor) {
		this.evolCoreSurfacePredictor = evolCoreSurfacePredictor;
	}
	
	private Chain getMolecule(int molecId) {
		if (molecId==FIRST) {
			return getInterface().getMolecules().getFirst()[0].getGroup().getChain();
		}
		if (molecId==SECOND) {
			return getInterface().getMolecules().getSecond()[0].getGroup().getChain();
		}
		return null;		
	}
	
	private String getChainId (int molecId) {
		if (molecId==FIRST) {
			return getInterface().getMoleculeIds().getFirst();
		}
		if (molecId==SECOND) {
			return getInterface().getMoleculeIds().getSecond();
		}
		return null;	
	}
	
	/**
	 * Finds all unreliable residues that belong to the surface and returns them in a list.
	 * Unreliable are all residues for which the alignment from reference UniProt to PDB doesn't match
	 * @param molecId
	 * @param minAsaForSurface
	 * @return
	 */
	public List<Group> getUnreliableSurfaceRes(int molecId, double minAsaForSurface) {
		List<Group> surfResidues = null;
		if (molecId==FIRST) {
			surfResidues = interf.getSurfaceResidues(minAsaForSurface).getFirst();
		} else if (molecId == SECOND) {
			surfResidues = interf.getSurfaceResidues(minAsaForSurface).getSecond();
		}
		return getReferenceMismatchResidues(surfResidues, molecId);
	}
	
	/**
	 * Given a list of residues returns the subset of those that are unreliable 
	 * because of mismatch of PDB sequence to UniProt reference matching (thus indicating
	 * engineered residues).
	 * @param residues
	 * @param molecId
	 * @return
	 */
	public List<Group> getReferenceMismatchResidues(List<Group> residues, int molecId) {
		List<Group> unreliableResidues = new ArrayList<Group>();
		for (Group res:residues){
			if (isReferenceMismatch(res, molecId)) {
				unreliableResidues.add(res);
			}
		}
		return unreliableResidues;
	}
	
	/**
	 * Given a residue and the molecId to which it belongs returns true if the position is 
	 * a UniProt reference mismatch (thus indicating engineered residue)
	 * @param residue
	 * @param molecId
	 * @return
	 */
	public boolean isReferenceMismatch(Group residue, int molecId) {
		ChainEvolContext chain = getChainEvolContext(molecId);
		int resSer = chain.getSeqresSerial(residue);
		if (resSer!=-1 && !chain.isPdbSeqPositionMatchingUniprot(resSer)) {
			return true;
		}
		return false;
	}
	
	public String getReferenceMismatchWarningMsg(List<Group> unreliableResidues, String typeOfResidues) {
		String msg = null;
		if (!unreliableResidues.isEmpty()) {
			
			msg = unreliableResidues.size()+" "+typeOfResidues+
					" residues of chain "+unreliableResidues.get(0).getChainId()+
					" are unreliable because of mismatch of PDB sequence to UniProt reference: ";
			
			for (int i=0;i<unreliableResidues.size();i++) {
				String serial = unreliableResidues.get(i).getResidueNumber().toString();
				 
				msg+= serial +
					  "("+unreliableResidues.get(i).getPDBName()+")";
				
				if (i!=unreliableResidues.size()-1) msg+=", ";				
			}
			msg+=" ";
		}
		return msg;
	}
	
	/**
	 * Calculates the evolutionary score for the given list of residues by summing up evolutionary
	 * scores per residue and averaging (optionally weighted by BSA)
	 * @param residues
	 * @param molecId
	 * @return
	 */
	public double calcScore(List<Group> residues, int molecId) {
		return getChainEvolContext(molecId).calcScoreForResidueSet(residues);
	}
	
	/**
	 * Returns the distribution of evolutionary scores of random subsets of residues in the surface for
	 * given molecId
	 * @param molecId the molecule id: either {@link #FIRST} or {@link #SECOND}
	 * @param numSamples number of samples of size sampleSize to be taken from the surface
	 * @param sampleSize number of residues in each sample
	 * @param minAsaForSurface the minimum ASA for a residue to be considered surface
	 * @return
	 */
	public double[] getSurfaceScoreDist(int molecId, int numSamples, int sampleSize, double minAsaForSurface) {
		if (sampleSize==0) return new double[0];

		double[] dist = new double[numSamples];
		
		List<Group> surfResidues = null;
		if (molecId==FIRST) surfResidues = interf.getSurfaceResidues(minAsaForSurface).getFirst();
		if (molecId==SECOND) surfResidues = interf.getSurfaceResidues(minAsaForSurface).getSecond();
		
		RandomDataImpl rd = new RandomDataImpl();
		for (int i=0;i<numSamples;i++) {
			Object[] sample = rd.nextSample(surfResidues, sampleSize);
			List<Group> residues = new ArrayList<Group>(sample.length);
			for (int j=0;j<sample.length;j++){
				residues.add((Group)sample[j]);
			}
			ChainEvolContext cec = this.parent.getChainEvolContext(getChainId(molecId));
			dist[i] = cec.calcScoreForResidueSet(residues);
		}		

		return dist;
	}
	
	/**
	 * Returns the number of residues that belong to the surface of the protein
	 * @param molecId the molecule id: either {@link #FIRST} or {@link #SECOND}
	 * @param minAsaForSurface the minimum ASA for a residue to be considered surface
	 * @return
	 */
	public int getNumSurfaceResidues(int molecId, double minAsaForSurface) {
		if (molecId==FIRST) return interf.getSurfaceResidues(minAsaForSurface).getFirst().size();
		if (molecId==SECOND) return interf.getSurfaceResidues(minAsaForSurface).getSecond().size();
		return -1;
	}
	
	/**
	 * Writes out a gzipped PDB file with the 2 chains of this interface with evolutionary scores 
	 * as b-factors. 
	 * In order for the file to be handled properly by molecular viewers whenever the two
	 * chains have the same code we rename the second one to the next letter in alphabet.
	 * PDB chain codes are used for the output, not CIF codes.  
	 * @param file
	 * @throws IOException
	 */
	public void writePdbFile(File file) throws IOException {
		
		
		if (CompoundFinder.isProtein(interf.getMolecules().getFirst()[0].getGroup().getChain()) && 
			CompoundFinder.isProtein(interf.getMolecules().getSecond()[0].getGroup().getChain())) {

			setConservationScoresAsBfactors(FIRST);
			setConservationScoresAsBfactors(SECOND);
			
			PrintStream ps = new PrintStream(new GZIPOutputStream(new FileOutputStream(file)));
			ps.print(interf.toPDB());
			ps.close();
		}
	}
	
	/**
	 * Set the b-factors of the given molecId (FIRST or SECOND) to conservation score values (at the
	 * moment, only entropy supported).
	 * @param molecId
	 * @throws NullPointerException if evolutionary scores are not calculated yet
	 */
	private void setConservationScoresAsBfactors(int molecId) {
		
		// do nothing (i.e. keep original b-factors) if there's no query match for this sequence and thus no evol scores calculated 
		if (!getChainEvolContext(molecId).hasQueryMatch()) return;
		
		List<Double> conservationScores = null;
		Chain pdb = getMolecule(molecId);
		conservationScores = getChainEvolContext(molecId).getConservationScores();
		
		for (Group residue:pdb.getAtomGroups()) {
			
			if (residue.isWater()) continue;

			int resser = getChainEvolContext(molecId).getSeqresSerial(residue);
			
			if (resser == -1) {
				LOGGER.info("Residue {} ({}) of chain {} could not be mapped to a serial, will not set its b-factor to an entropy value",
						residue.getResidueNumber().toString(), residue.getPDBName(), residue.getChainId());
				continue;
			}
				
			int queryPos = getChainEvolContext(molecId).getQueryUniprotPosForPDBPos(resser); 
			 
			if (queryPos!=-1) {
				for (Atom atom:residue.getAtoms()) {
					atom.setTempFactor(conservationScores.get(queryPos-1));
				}
			} else {
				
				// when no entropy info is available for a residue we still want to assign a value for it
				// or otherwise the residue would keep its original real bfactor and then possibly screw up the
				// scaling of colors for the rest
				// The most sensible value we can use is the max entropy so that it looks like a poorly conserved residue
				double maxEntropy = Math.log(this.getChainEvolContext(molecId).getHomologs().getReducedAlphabet())/Math.log(2);
				LOGGER.info("Residue {} ({}) of chain {} has no entropy value associated to it, will set its b-factor to max entropy ({})",
						residue.getResidueNumber().toString(), residue.getPDBName(), residue.getChainId(), maxEntropy);
				for (Atom atom:residue.getAtoms()) {
					atom.setTempFactor(maxEntropy);
				}
			}
		}
	}
	
	public boolean hasEnoughHomologs(int molecId){
		return this.getChainEvolContext(molecId).getNumHomologs()>=parent.getMinNumSeqs(); 
	}

	public int getMinNumSeqs() {
		return this.parent.getMinNumSeqs();
	}
	
	public boolean isProtein(int molecId) {
		if (molecId==FIRST) {
			return CompoundFinder.isProtein(interf.getMolecules().getFirst()[0].getGroup().getChain());
		} else if (molecId==SECOND) {
			return CompoundFinder.isProtein(interf.getMolecules().getSecond()[0].getGroup().getChain());
		} else {
			throw new IllegalArgumentException("Fatal error! Wrong molecId "+molecId);
		}
	}

}
