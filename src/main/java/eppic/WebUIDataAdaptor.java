package eppic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import eppic.model.HomologDB;
import eppic.model.ChainClusterDB;
import eppic.model.InterfaceDB;
import eppic.model.ResidueDB;
import eppic.model.InterfaceScoreDB;
import eppic.model.PdbInfoDB;
import eppic.model.AssemblyDB;
import eppic.model.UniProtRefWarningDB;
import eppic.model.RunParametersDB;
import eppic.model.InterfaceWarningDB;
import eppic.predictors.CombinedPredictor;
import eppic.predictors.EvolInterfZPredictor;
import eppic.predictors.EvolRimCorePredictor;
import eppic.predictors.GeometryPredictor;
import owl.core.runners.PymolRunner;
import owl.core.sequence.Homolog;
import owl.core.structure.ChainCluster;
import owl.core.structure.ChainInterface;
import owl.core.structure.ChainInterfaceList;
import owl.core.structure.PdbAsymUnit;
import owl.core.structure.PdbBioUnit;
import owl.core.structure.PdbBioUnitList;
import owl.core.structure.PdbChain;
import owl.core.structure.Residue;
import owl.core.structure.SpaceGroup;
import owl.core.util.Goodies;


public class WebUIDataAdaptor {

	private static final int FIRST = 0;
	private static final int SECOND = 1;
	
	private PdbInfoDB pdbScoreItem;
	
	private EppicParams params;
	
	private RunParametersDB runParametersItem;
	
	// a temp map to hold the warnings per interface, used in order to eliminate duplicate warnings
	private HashMap<Integer,HashSet<String>> interfId2Warnings;
	
	public WebUIDataAdaptor() {
		pdbScoreItem = new PdbInfoDB();
		interfId2Warnings = new HashMap<Integer, HashSet<String>>();
	}
	
	public void setParams(EppicParams params) {
		this.params = params;
		pdbScoreItem.setPdbName(params.getJobName());
		runParametersItem = new RunParametersDB();
		runParametersItem.setHomologsCutoff(params.getMinNumSeqs());
		runParametersItem.setHomSoftIdCutoff(params.getHomSoftIdCutoff());
		runParametersItem.setHomHardIdCutoff(params.getHomHardIdCutoff());
		runParametersItem.setQueryCovCutoff(params.getQueryCoverageCutoff());
		runParametersItem.setMaxNumSeqsCutoff(params.getMaxNumSeqs());
		runParametersItem.setReducedAlphabet(params.getReducedAlphabet());
		runParametersItem.setCaCutoffForGeom(params.getCAcutoffForGeom());
		runParametersItem.setCaCutoffForCoreRim(params.getCAcutoffForRimCore());
		runParametersItem.setCaCutoffForCoreSurface(params.getCAcutoffForZscore());
		runParametersItem.setCrCallCutoff(params.getCoreRimScoreCutoff());
		runParametersItem.setCsCallCutoff(params.getCoreSurfScoreCutoff());
		runParametersItem.setGeomCallCutoff(params.getMinCoreSizeForBio());
		runParametersItem.setPdbInfo(pdbScoreItem);
		runParametersItem.setEppicVersion(EppicParams.PROGRAM_VERSION);
		pdbScoreItem.setRunParameters(runParametersItem);
	}
	
	public void setPdbMetadata(PdbAsymUnit pdb) {
		pdbScoreItem.setTitle(pdb.getTitle());
		pdbScoreItem.setReleaseDate(pdb.getReleaseDate());
		SpaceGroup sg = pdb.getSpaceGroup();
		pdbScoreItem.setSpaceGroup(sg==null?null:sg.getShortSymbol());
		pdbScoreItem.setResolution(pdb.getResolution());
		pdbScoreItem.setRfreeValue(pdb.getRfree());
		pdbScoreItem.setExpMethod(pdb.getExpMethod());
		
	}
	
	public void setInterfaces(ChainInterfaceList interfaces, PdbBioUnitList bioUnitList) {
		int iInterface = 0;
		//Get the full details on biounits
		TreeMap<Integer, List<Integer>> matchIds = bioUnitList.getInterfaceMatches(interfaces);
		for (ChainInterface interf:interfaces) {
			iInterface++;
			InterfaceDB ii = new InterfaceDB();
			ii.setInterfaceId(interf.getId());
			ii.setClusterId(interfaces.getCluster(interf.getId()).getId());
			ii.setArea(interf.getInterfaceArea());
			
			ii.setChain1(interf.getFirstMolecule().getPdbChainCode());
			ii.setChain2(interf.getSecondMolecule().getPdbChainCode());
			
			ii.setOperator(SpaceGroup.getAlgebraicFromMatrix(interf.getSecondTransf().getMatTransform()));
			ii.setOperatorType(interf.getSecondTransf().getTransformType().getShortName());
			ii.setIsInfinite(interf.isInfinite());
			

			for(int bioUnitId:matchIds.keySet()){
				PdbBioUnit unit = bioUnitList.get(bioUnitId);
				PdbBioUnitAssignmentItemDB assignDB = new PdbBioUnitAssignmentItemDB();
				
				assignDB.setSize(unit.getSize());
				assignDB.setMethod(unit.getType().getType());
				
				if(matchIds.get(bioUnitId).contains(iInterface)) assignDB.setRegion("bio");
				else assignDB.setRegion("xtal");
				
				assignDB.setInterface(ii);
				
				ii.addBioUnitAssignment(assignDB);
			}
			
			ii.setPdbInfo(pdbScoreItem);
			
			pdbScoreItem.addInterfaceItem(ii);
			
			interfId2Warnings.put(interf.getId(),new HashSet<String>());
		}

	}
	
	public void setPdbBioUnits(PdbBioUnitList pdbBioUnitList) {
		
		for(PdbBioUnit unit:pdbBioUnitList){
			AssemblyDB unitDb = new AssemblyDB();
			unitDb.setMmSize(unit.getSize());
			unitDb.setMethod(unit.getType().getType());
			
			unitDb.setPdbInfo(pdbScoreItem);
			
			pdbScoreItem.addAssembly(unitDb);
		}
		
	}
	
	public void writeJmolScriptFile(ChainInterface interf, double caCutoff, double minAsaForSurface, PymolRunner pr, File dir, String prefix, boolean usePdbResSer) 
			throws FileNotFoundException {
		 
			File file = new File(dir,prefix+"."+interf.getId()+".jmol");
			PrintStream ps = new PrintStream(file);
			ps.print(createJmolScript(interf, caCutoff, minAsaForSurface, pr, usePdbResSer));
			ps.close();

	}
	
	private String createJmolScript(ChainInterface interf, double caCutoff, double minAsaForSurface, PymolRunner pr, boolean usePdbResSer) {
		char chain1 = interf.getFirstMolecule().getPdbChainCode().charAt(0);
		char chain2 = interf.getSecondPdbChainCodeForOutput().charAt(0);
		
		String color1 = pr.getHexColorCode(pr.getChainColor(chain1, 0, interf.isSymRelated()));
		String color2 = pr.getHexColorCode(pr.getChainColor(chain2, 1, interf.isSymRelated()));
		color1 = "[x"+color1.substring(1, color1.length())+"]"; // converting to jmol format
		color2 = "[x"+color2.substring(1, color2.length())+"]";
		String colorInterf1 = pr.getHexColorCode(pr.getInterf1Color());
		String colorInterf2 = pr.getHexColorCode(pr.getInterf2Color());
		colorInterf1 = "[x"+colorInterf1.substring(1, colorInterf1.length())+"]";
		colorInterf2 = "[x"+colorInterf2.substring(1, colorInterf2.length())+"]";
		
		StringBuffer sb = new StringBuffer();
		sb.append("cartoon on; wireframe off; spacefill off; set solvent off;\n");
		sb.append("select :"+chain1+"; color "+color1+";\n");
		sb.append("select :"+chain2+"; color "+color2+";\n");
		interf.calcRimAndCore(caCutoff, minAsaForSurface);
		sb.append(getSelString("core", chain1, interf.getFirstRimCore().getCoreResidues(), usePdbResSer)+";\n");
		sb.append(getSelString("core", chain2, interf.getSecondRimCore().getCoreResidues(), usePdbResSer)+";\n");
		sb.append(getSelString("rim", chain1, interf.getFirstRimCore().getRimResidues(), usePdbResSer)+";\n");
		sb.append(getSelString("rim", chain2, interf.getSecondRimCore().getRimResidues(), usePdbResSer)+";\n");
		sb.append("define interface"+chain1+" core"+chain1+" or rim"+chain1+";\n");
		sb.append("define interface"+chain2+" core"+chain2+" or rim"+chain2+";\n");
		sb.append("define bothinterf interface"+chain1+" or interface"+chain2+";\n");
		// surfaces are cool but in jmol they don't display as good as in pymol, especially the transparency effect is quite bad
		//sb.append("select :"+chain1+"; isosurface surf"+chain1+" solvent;color isosurface gray;color isosurface translucent;\n");
		//sb.append("select :"+chain2+"; isosurface surf"+chain2+" solvent;color isosurface gray;color isosurface translucent;\n");
		sb.append("select interface"+chain1+";wireframe 0.3;\n");
		sb.append("select interface"+chain2+";wireframe 0.3;\n");
		sb.append("select core"+chain1+";"+"color "+colorInterf1+";wireframe 0.3;\n");
		sb.append("select core"+chain2+";"+"color "+colorInterf2+";wireframe 0.3;\n");
		if (interf.hasCofactors()) {
			sb.append("select ligand;wireframe 0.3;\n");
		}
		return sb.toString();
	}
	
	private String getResiSelString(List<Residue> list, char chainName, boolean usePdbResSer) {
		// residue 0 or negatives can exist (e.g. 1epr). In order to have an empty selection we 
		// simply use a very low negative numbe which is unlikely to exist in PDB
		if (list.isEmpty()) return "-10000:"+chainName;
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<list.size();i++) {
			if (usePdbResSer) {
				// jmol uses a special syntax for residue serials with insertion 
				// codes, e.g. 23A from chain A would be "23^A:A" and not "23A:A"
				// A PDB with this problem is 1yg9, it would show a blank jmol screen before this fix
				String pdbSerial = list.get(i).getPdbSerial();
				char lastChar = pdbSerial.charAt(pdbSerial.length()-1);
				if (!Character.isDigit(lastChar)) {
					pdbSerial = pdbSerial.replace(Character.toString(lastChar), "^"+lastChar);
				}
				sb.append(pdbSerial+":"+chainName);
			} else {
				sb.append(list.get(i).getSerial()+":"+chainName);
			}
			if (i!=list.size()-1) sb.append(",");
		}
		return sb.toString();
	}

	private String getSelString(String namePrefix, char chainName, List<Residue> list, boolean usePdbResSer) {
		return "define "+namePrefix+chainName+" "+getResiSelString(list,chainName, usePdbResSer);
	}
	
	public void setGeometryScores(List<GeometryPredictor> gps) {
		for (int i=0;i<gps.size();i++) {
			InterfaceDB ii = pdbScoreItem.getInterfaceItem(i);
			InterfaceScoreDB isi = new InterfaceScoreDB();
			ii.addInterfaceScore(isi);
			isi.setInterface(ii);
			isi.setInterfaceId(gps.get(i).getInterface().getId());
			CallType call = gps.get(i).getCall();
			isi.setCall(call.getName());
			isi.setCallReason(gps.get(i).getCallReason());
			isi.setMethod("Geometry");
			
			if(gps.get(i).getWarnings() != null)
			{
				List<String> warnings = gps.get(i).getWarnings();
				for(String warning: warnings)
				{	
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

		}
	}
	
	public void add(InterfaceEvolContextList iecl) {
		
		List<ChainClusterDB> homInfos = new ArrayList<ChainClusterDB>();
		
		ChainEvolContextList cecl = iecl.getChainEvolContextList();
		for (ChainEvolContext cec:cecl.getAllChainEvolContext()) {
			ChainClusterDB homInfo = new ChainClusterDB();
			ChainCluster cc = cecl.getPdb().getProtChainCluster(cec.getRepresentativeChainCode());
			homInfo.setRepChain(cc.getRepresentative().getPdbChainCode());
			homInfo.setMemberChains(cc.getCommaSepMemberChainCodes());
			homInfo.setHasUniProtRef(cec.hasQueryMatch());
			
			List<UniProtRefWarningDB> queryWarningItemDBs = new ArrayList<UniProtRefWarningDB>();
			for(String queryWarning : cec.getQueryWarnings())
			{
				UniProtRefWarningDB queryWarningItemDB = new UniProtRefWarningDB();
				queryWarningItemDB.setChainCluster(homInfo);
				queryWarningItemDB.setText(queryWarning);
				queryWarningItemDBs.add(queryWarningItemDB);
			}
			
			homInfo.setUniProtRefWarnings(queryWarningItemDBs);
			
			if (cec.hasQueryMatch()) { //all other fields remain null otherwise
				
				homInfo.setNumHomologs(cec.getNumHomologs());
				homInfo.setRefUniProtId(cec.getQuery().getUniId()); 
				 
				homInfo.setRefUniProtStart(cec.getQueryInterval().beg);
				homInfo.setRefUniProtEnd(cec.getQueryInterval().end);
				
				homInfo.setPdbAlignedSeq(cec.getPdb2uniprotAln().getAlignedSequences()[0]);
				homInfo.setAliMarkupLine(String.valueOf(cec.getPdb2uniprotAln().getMarkupLine()));
				homInfo.setRefAlignedSeq(cec.getPdb2uniprotAln().getAlignedSequences()[1]);
				homInfo.setSeqIdCutoff(cec.getIdCutoff());
				homInfo.setClusteringSeqId(cec.getUsedClusteringPercentId());
				
				List<HomologDB> homologItemDBs = new ArrayList<HomologDB>();
				for (Homolog hom:cec.getHomologs().getFilteredSubset()) {
					HomologDB homologItemDB = new HomologDB();
					homologItemDB.setUniProtId(hom.getUniId());
					homologItemDB.setQueryStart(hom.getBlastHsp().getQueryStart());
					homologItemDB.setQueryEnd(hom.getBlastHsp().getQueryEnd());
					if (hom.getUnirefEntry().hasTaxons()) {
						homologItemDB.setFirstTaxon(hom.getUnirefEntry().getFirstTaxon());
						homologItemDB.setLastTaxon(hom.getUnirefEntry().getLastTaxon());
					}
					homologItemDB.setSeqId(hom.getPercentIdentity());
					homologItemDB.setQueryCoverage(hom.getQueryCoverage()*100.0);
					homologItemDB.setChainCluster(homInfo);
					homologItemDBs.add(homologItemDB);
				}
				
				homInfo.setHomologs(homologItemDBs);
			} 

			homInfo.setPdbInfo(pdbScoreItem);
			homInfos.add(homInfo);	
		}
		
		pdbScoreItem.setChainClusters(homInfos);
		

		for (int i=0;i<iecl.size();i++) {
			
			InterfaceEvolContext iec = iecl.get(i);
			InterfaceDB ii = pdbScoreItem.getInterfaceItem(i);
			
			// 1) we add entropy values to the residue details
			addEntropyToResidueDetails(ii.getResidues(), iec);
			
			
			// 2) z-scores
			EvolInterfZPredictor ezp = iecl.getEvolInterfZPredictor(i);
			InterfaceScoreDB isiZ = new InterfaceScoreDB();
			ii.addInterfaceScore(isiZ);
			isiZ.setInterface(ii);
			isiZ.setInterfaceId(iec.getInterface().getId());
			isiZ.setMethod("Z-scores");

			CallType call = ezp.getCall();	
			isiZ.setCall(call.getName());
			isiZ.setCallReason(ezp.getCallReason());
			
			if(ezp.getWarnings() != null) {
				List<String> warnings = ezp.getWarnings();
				for(String warning: warnings) {
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

			// In z-scores, core, rim and ratio scores don't make so much sense
			// So we basically abuse them to carry all desired info to WUI:
			// - core1/core2Score: the surface sampling mean score for each side
			// - rim1/rim2Score: the surface sampling standard deviation for each side
			// - score1/score2: the z-scores for each side
			// - score: the average z-score
			
			// TODO commenting out for now the extra info while we redo the model
			//isiZ.setCore1Score(ezp.getMember1Predictor().getMean());
			//isiZ.setCore2Score(ezp.getMember2Predictor().getMean());
			//isiZ.setRim1Score(ezp.getMember1Predictor().getSd());
			//isiZ.setRim2Score(ezp.getMember2Predictor().getSd());
			
			isiZ.setScore1(ezp.getMember1Predictor().getScore());
			isiZ.setScore2(ezp.getMember2Predictor().getScore());
			isiZ.setScore(ezp.getScore());				

			
			// 3) rim-core entropies
			EvolRimCorePredictor ercp = iecl.getEvolRimCorePredictor(i);

			InterfaceScoreDB isiRC = new InterfaceScoreDB();
			isiRC.setInterface(ii);
			ii.addInterfaceScore(isiRC);
			isiRC.setInterfaceId(iec.getInterface().getId());
			isiRC.setMethod("Entropy");

			call = ercp.getCall();	
			isiRC.setCall(call.getName());
			isiRC.setCallReason(ercp.getCallReason());

			if(ercp.getWarnings() != null) {
				List<String> warnings = ercp.getWarnings();
				for(String warning: warnings) {
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}

			// TODO commenting out for now the extra info while we redo the model
			//isiRC.setCore1Score(ercp.getMember1Predictor().getCoreScore());
			//isiRC.setCore2Score(ercp.getMember2Predictor().getCoreScore());
			//isiRC.setRim1Score(ercp.getMember1Predictor().getRimScore());
			//isiRC.setRim2Score(ercp.getMember2Predictor().getRimScore());
			
			isiRC.setScore1(ercp.getMember1Predictor().getScore());
			isiRC.setScore2(ercp.getMember2Predictor().getScore());
			isiRC.setScore(ercp.getScore());				

		}
		

	}
	
	public void setCombinedPredictors(List<CombinedPredictor> cps) {
		for (int i=0;i<cps.size();i++) {
			InterfaceDB ii = pdbScoreItem.getInterfaceItem(i);
			ii.setFinalCallName(cps.get(i).getCall().getName());		
			ii.setFinalCallReason(cps.get(i).getCallReason());
			if(cps.get(i).getWarnings() != null)
			{
				List<String> warnings = cps.get(i).getWarnings();
				for(String warning: warnings)
				{
					// we first add warning to the temp HashSets in order to eliminate duplicates, 
					// in the end we fill the InterfaceItemDBs by calling addInterfaceWarnings
					interfId2Warnings.get(ii.getInterfaceId()).add(warning);
				}
			}
		}
	}
	
	public void writePdbScoreItemFile(File file) throws EppicException {
		try {
			Goodies.serialize(file,pdbScoreItem);
		} catch (IOException e) {
			throw new EppicException(e, e.getMessage(), true);
		}
	}
	
	public void addResidueDetails(ChainInterfaceList interfaces) {
		for (int i=0;i<interfaces.size();i++) {

			ChainInterface interf = interfaces.get(i+1);
			InterfaceDB ii = pdbScoreItem.getInterfaceItem(i);

			// we add the residue details
			addResidueDetails(ii, interf, params.isDoScoreEntropies());
		}
	}
	
	private void addResidueDetails(InterfaceDB ii, ChainInterface interf, boolean includeEntropy) {
		
		List<ResidueDB> iril = new ArrayList<ResidueDB>();
		ii.setResidues(iril);

		addResidueDetailsOfPartner(iril, interf, 0);
		addResidueDetailsOfPartner(iril, interf, 1);

		for(ResidueDB iri : iril)
		{
			iri.setInterface(ii);
		}
	}
	
	private void addResidueDetailsOfPartner(List<ResidueDB> iril, ChainInterface interf, int molecId) {
		if (interf.isProtein()) {
			PdbChain mol = null;
			if (molecId==FIRST) {
				mol = interf.getFirstMolecule();
			}
			else if (molecId==SECOND) {
				mol = interf.getSecondMolecule();
			}
			
			for (Residue residue:mol) {
				String resType = residue.getLongCode();
				int assignment = -1;
				
				float asa = (float) residue.getAsa();
				float bsa = (float) residue.getBsa();
				
				if (residue.getAsa()>params.getMinAsaForSurface() && residue.getBsa()>0) {
					// NOTE: we use here caCutoffForRimCore as the one and only for both evol methods
					// NOTE2: we are assuming that caCutoffForRimCore<caCutoffForGeom, if that doesn't hold this won't work!
					if (residue.getBsaToAsaRatio()<params.getCAcutoffForRimCore()) {
						assignment = ResidueDB.RIM;
					} else if (residue.getBsaToAsaRatio()<params.getCAcutoffForGeom()){
						assignment = ResidueDB.CORE_EVOLUTIONARY; 
					} else {
						assignment = ResidueDB.CORE_GEOMETRY;
					}
				} else if (residue.getAsa()>params.getMinAsaForSurface()) {
					assignment = ResidueDB.SURFACE;
				}
				
				ResidueDB iri = new ResidueDB(residue.getSerial(),residue.getPdbSerial(),resType,asa,bsa,assignment,null);
				iri.setSide(molecId+1); // structure ids are 1 and 2 while molecId are 0 and 1

				iril.add(iri);
			}
		}
	}

	private void addEntropyToResidueDetails(List<ResidueDB> iril, InterfaceEvolContext iec) {
		ChainInterface interf = iec.getInterface();
		
		
		int[] molecIds = new int[2];
		molecIds[0] = 0;
		molecIds[1] = 1;

		// beware the counter is global for both molecule 1 and 2 (as the List<InterfaceResidueItemDB> contains both, identified by a structure id 1 or 2)
		int i = 0;  

		for (int molecId:molecIds) { 
			ChainEvolContext cec = iec.getChainEvolContext(molecId);
			PdbChain mol = null;
			if (molecId==FIRST) {
				mol = interf.getFirstMolecule();
			}
			else if (molecId==SECOND) {
				mol = interf.getSecondMolecule();
			}

			if (interf.isProtein()) {
				 
				List<Double> entropies = null;
				if (cec.hasQueryMatch()) 
					entropies = cec.getConservationScores(ScoringType.ENTROPY);
				for (Residue residue:mol) {

	 				ResidueDB iri = iril.get(i);
					
					int queryUniprotPos = -1;
					if (!mol.isNonPolyChain() && mol.getSequence().isProtein() && cec.hasQueryMatch()) 
						queryUniprotPos = cec.getQueryUniprotPosForPDBPos(residue.getSerial());

					float entropy = -1;
					// we used to have here: "&& residue instanceof AaResidue" but that was preventing entropy values of mismatch-to-ref-uniprot-residues to be passed
					// for het residues we do have entropy values too as the entropy values are calculated on the reference uniprot sequence (where no het residues are present)
					if (entropies!=null) {	
						if (queryUniprotPos!=-1) entropy = (float) entropies.get(queryUniprotPos).doubleValue();
					}

					iri.setEntropyScore(entropy); 
					i++;
				}
			}
		}
		
		
	}

	public RunParametersDB getRunParametersItem() {
		return runParametersItem;
	}
	
	/**
	 * Add to the pdbScoreItem member the cached warnings interfId2Warnings, compiled in
	 * {@link #setGeometryScores(List)}, {@link #setCombinedPredictors(List)} and {@link #add(InterfaceEvolContextList)} 
	 */
	public void addInterfaceWarnings() {
		
		for (int i=0;i<pdbScoreItem.getInterfaceItems().size();i++) {
			InterfaceDB ii = pdbScoreItem.getInterfaceItem(i);
			for (String warning : interfId2Warnings.get(ii.getInterfaceId())) {
				InterfaceWarningDB warningItem = new InterfaceWarningDB();
				warningItem.setText(warning);
				warningItem.setInterface(ii);
				ii.getInterfaceWarnings().add(warningItem);
			}
		}
	}
	
}
