package ch.systemsx.sybit.crkwebui.server.files.downloader.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;

import org.junit.Test;

import ch.systemsx.sybit.crkwebui.shared.model.Assembly;
import ch.systemsx.sybit.crkwebui.shared.model.AssemblyContent;
import ch.systemsx.sybit.crkwebui.shared.model.AssemblyScore;
import ch.systemsx.sybit.crkwebui.shared.model.ChainCluster;
import ch.systemsx.sybit.crkwebui.shared.model.Homolog;
import ch.systemsx.sybit.crkwebui.shared.model.Interface;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceCluster;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceClusterScore;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceScore;
import ch.systemsx.sybit.crkwebui.shared.model.PdbInfo;
import ch.systemsx.sybit.crkwebui.shared.model.Residue;
import ch.systemsx.sybit.crkwebui.shared.model.RunParameters;


public class DataDownloadServletTest {

	@Test
	public void testSerializePdbInfoList() throws PropertyException, JAXBException {
		List<PdbInfo> pdbList = getPdbInfo();
		StringWriter stringWriter = new StringWriter();
		PrintWriter p = new PrintWriter(stringWriter);

		DataDownloadServlet d = new DataDownloadServlet();

		d.serializePdbInfoList(pdbList, p);

		String resultString = stringWriter.getBuffer().toString();
		System.out.println(resultString);
		String expectedFirstLine = "<eppicAnalysisList><eppicAnalysis uid=\"9999\" jobId=\"1smt\" inputType=\"0\" inputName=\"1smt\">";
		assertEquals("Wrong content", expectedFirstLine, resultString.split("\n")[0]);
		assertTrue("Result should contain pdb title", resultString.contains("SMTB REPRESSOR FROM SYNECHOCOCCUS PCC7942"));
		assertTrue("Result should contain the interface", resultString.contains("<operatorType>OP</operatorType>"));
		assertTrue("Result should contain the interface cluster", resultString.contains("<interfaceCluster>"));
	}

	private List<PdbInfo> getPdbInfo() {
		PdbInfo pdbInfo = new PdbInfo();
		pdbInfo.setInputType(0);
		pdbInfo.setInputName("1smt");
		pdbInfo.setJobId("1smt");
		pdbInfo.setPdbCode("1smt");
		pdbInfo.setResolution(2.2);
		pdbInfo.setRfreeValue(2.25);
		pdbInfo.setTitle("SMTB REPRESSOR FROM SYNECHOCOCCUS PCC7942");
		pdbInfo.setExpMethod("X-RAY DIFFRACTION");
		RunParameters runParameters = getRunParameters();
		pdbInfo.setRunParameters(runParameters);
		pdbInfo.setUid(9999);
		pdbInfo.setReleaseDate(new Date());
		pdbInfo.setSpaceGroup("XXX");
		List<InterfaceCluster> clusters = getInterfaceClusters();
		pdbInfo.setInterfaceClusters(clusters);
		List<ChainCluster> chainClusters = getChainCluster();
		pdbInfo.setChainClusters(chainClusters);
		pdbInfo.setCellA(1);
		pdbInfo.setCellA(1);
		pdbInfo.setCellA(1);
		pdbInfo.setCellAlpha(90);
		pdbInfo.setCellBeta(90);
		pdbInfo.setCellGamma(90);
		pdbInfo.setCrystalFormId(1);
		pdbInfo.setAssemblies(getAssemblies());
		pdbInfo.setMaxNumClashesAnyInterface(10); 
		return Arrays.asList(pdbInfo);
	}

	private List<ChainCluster> getChainCluster() {
		List<ChainCluster> chainClusters = new ArrayList<ChainCluster>();
		ChainCluster chainCluster = new ChainCluster();
		chainCluster.setClusteringSeqId(232423.2323);
		List<Homolog> homologs = new ArrayList<Homolog>();
		Homolog homolog = new Homolog();
		homolog.setAlignedSeq("ABCDEFGHIJKL");
		homolog.setFirstTaxon("tax1");
		homolog.setLastTaxon("last tax");
		homolog.setQueryCoverage(.3);
		homologs.add(homolog);
		chainCluster.setHomologs(homologs);
		chainClusters.add(chainCluster);
		return chainClusters;
	}

	private List<InterfaceCluster> getInterfaceClusters() {
		List<InterfaceCluster> clusters = new ArrayList<InterfaceCluster>();
		InterfaceCluster iC = new InterfaceCluster();
		iC.setAvgArea(100);
		iC.setClusterId(1);
		List<InterfaceClusterScore> interfaceClusterScores = new ArrayList<InterfaceClusterScore>();
		InterfaceClusterScore iCS1 = new InterfaceClusterScore();
		iCS1.setCallName("name1");
		iCS1.setConfidence(.99);
		iCS1.setMethod("method1");
		iCS1.setScore(333.333);
		iCS1.setScore1(444.444);
		iCS1.setScore2(555.555);
		iCS1.setUid(1234);
		interfaceClusterScores.add(iCS1);
		InterfaceClusterScore iCS2 = new InterfaceClusterScore();
		iCS2.setCallName("name2");
		iCS2.setConfidence(2.99);
		iCS2.setMethod("method2");
		iCS2.setScore(333.333);
		iCS2.setScore1(444.444);
		iCS2.setScore2(555.555);
		iCS2.setUid(1234);
		interfaceClusterScores.add(iCS2);
		iC.setInterfaceClusterScores(interfaceClusterScores);
		List<Interface> interfaces = new ArrayList<Interface>();
		Interface int1 = new Interface();
		InterfaceScore interfaceScore = new InterfaceScore();
		interfaceScore.setCallName("ic name");
		interfaceScore.setCallReason("just");
		interfaceScore.setConfidence(.98);
		interfaceScore.setInterfaceId(3);
		interfaceScore.setMethod("i method");
		interfaceScore.setScore(123);
		interfaceScore.setScore1(234);
		interfaceScore.setScore2(345);
		int1.addInterfaceScore(interfaceScore);
		int1.setArea(100);
		int1.setChain1("A");
		int1.setChain2("B");
		int1.setClusterId(1);
		int1.setInterfaceId(1);
		int1.setOperator("XXX");
		int1.setOperatorType("OP");
		int1.setOperatorId(1);
		int1.setXtalTrans_x(1);
		int1.setXtalTrans_y(-1);
		int1.setXtalTrans_z(0);
		List<Residue> residues = new ArrayList<Residue>();
		Residue residue = new Residue();
		residue.setUid(1);	
		residue.setAsa(3.33);
		residue.setBsa(4.44);
		residue.setEntropyScore(5.55);
		residue.setRegion((short)3);
		residue.setResidueNumber(1);
		residue.setResidueType("ALA");
		residue.setSide(false);
		residues.add(residue);
		Residue residue2 = new Residue();
		residue2.setUid(2);	
		residue2.setAsa(3.33);
		residue2.setBsa(4.44);
		residue2.setEntropyScore(5.55);
		residue2.setRegion((short)3);
		residue2.setResidueNumber(17);
		residue2.setResidueType("SER");
		residue2.setSide(true);
		residues.add(residue2);
		int1.setResidues(residues);
		interfaces.add(int1);
		iC.setInterfaces(interfaces);
		iC.setUid(1111);
		clusters.add(iC);
		return clusters;
	}
	
	private List<Assembly> getAssemblies() {
		List<Assembly> assemblies = new ArrayList<Assembly>();
		
		Assembly a1 = new Assembly();
		a1.setAssemblyScores(getAssemblyScores());
		a1.setAssemblyContents(getAssemblyContents());
		a1.setId(1);
		a1.setInterfaceClusterIdsString("1,2,3,4");
		a1.setTopologicallyValid(true);
		Assembly a2 = new Assembly();
		a2.setId(2);
		a2.setInterfaceClusterIdsString("1,2,3");
		a2.setTopologicallyValid(false);
		
		
		assemblies.add(a1);
		assemblies.add(a2);
		
		return assemblies;
	}
	
	private List<AssemblyScore> getAssemblyScores() {
		List<AssemblyScore> assemblyScores = new ArrayList<>();
		AssemblyScore as1 = new AssemblyScore();
		as1.setCallName("BIO");
		as1.setMethod("EPPIC");
		AssemblyScore as2 = new AssemblyScore();
		as2.setCallName("XTAL");
		as2.setMethod("EPPIC");

		assemblyScores.add(as1);
		assemblyScores.add(as2);
		
		return assemblyScores;
	}
	
	private List<AssemblyContent> getAssemblyContents() {
		List<AssemblyContent> assemblyContents = new ArrayList<>();
		AssemblyContent ac1 = new AssemblyContent();
		ac1.setMmSize(3);
		ac1.setStoichiometry("A3");
		ac1.setSymmetry("C3");
		AssemblyContent ac2 = new AssemblyContent();
		ac2.setMmSize(1);
		ac2.setStoichiometry("A1");
		ac2.setSymmetry("C1"); 		
		
		assemblyContents.add(ac1);
		assemblyContents.add(ac2);
		
		return assemblyContents;
	}

	private RunParameters getRunParameters() {
		RunParameters runParameters = new RunParameters();
		runParameters.setCaCutoffForCoreRim(.1);
		runParameters.setCaCutoffForCoreSurface(.2);
		runParameters.setCaCutoffForGeom(.3);
		runParameters.setCrCallCutoff(.4);
		runParameters.setCsCallCutoff(.5);
		runParameters.setEppicVersion("X");
		runParameters.setGeomCallCutOff(6);
		return runParameters;
	}
}
