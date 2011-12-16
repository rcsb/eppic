package crk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.xml.sax.SAXException;

import crk.predictors.CombinedPredictor;
import crk.predictors.GeometryPredictor;

import owl.core.connections.pisa.PisaConnection;
import owl.core.runners.PymolRunner;
import owl.core.runners.TcoffeeException;
import owl.core.runners.blast.BlastException;
import owl.core.sequence.UniprotVerMisMatchException;
import owl.core.structure.ChainInterface;
import owl.core.structure.ChainInterfaceList;
import owl.core.structure.PdbAsymUnit;
import owl.core.structure.PdbLoadException;
import owl.core.structure.SpaceGroup;
import owl.core.structure.graphs.AICGraph;
import owl.core.util.FileFormatException;
import owl.core.util.Goodies;

public class CRKMain {
	
	
	// THE ROOT LOGGER (log4j)
	private static final Logger ROOTLOGGER = Logger.getRootLogger();
	private static final Log LOGGER = LogFactory.getLog(CRKMain.class);
	
	private static final int STEPS_TOTAL = 4;
	
	// fields
	private CRKParams params;
	
	private PdbAsymUnit pdb;
	private ChainInterfaceList interfaces;
	private ChainEvolContextList cecs;
	private InterfaceEvolContextList iecList;
	private List<GeometryPredictor> gps;
	
	private File stepsLogFile;
	private int stepCount;
	
	private WebUIDataAdaptor wuiAdaptor;
		
	public CRKMain() {
		this.params = new CRKParams();
		this.stepCount = 1;
	}
	

		
	public void setUpLogging() {
		
		// setting up the file logger for log4j
		try {
			FileAppender fullLogAppender = new FileAppender(new PatternLayout("%d{ABSOLUTE} %5p - %m%n"),params.getOutDir()+"/"+params.getBaseName()+".log",false);
			fullLogAppender.setThreshold(Level.INFO);
			ConsoleAppender errorAppender = new ConsoleAppender(new PatternLayout("%5p - %m%n"),ConsoleAppender.SYSTEM_ERR);
			errorAppender.setThreshold(Level.ERROR);
			ConsoleAppender outAppender = new ConsoleAppender(new PatternLayout("%d{ABSOLUTE} %5p - %m%n"),ConsoleAppender.SYSTEM_OUT);
			outAppender.setThreshold(Level.DEBUG);
			ROOTLOGGER.addAppender(fullLogAppender);
			ROOTLOGGER.addAppender(errorAppender);
			if (params.getProgressLogFile()!=null) {
				FileAppender fileErrorAppender = new FileAppender(new PatternLayout("%5p - %m%n"),params.getProgressLogFile().getAbsolutePath(),true);
				fileErrorAppender.setThreshold(Level.ERROR);
				ROOTLOGGER.addAppender(fileErrorAppender);
				stepsLogFile = new File(params.getOutDir(),params.getBaseName()+CRKParams.STEPS_LOG_FILE_SUFFIX);
			}
			if (params.getDebug())
				ROOTLOGGER.addAppender(outAppender);
		} catch (IOException e) {
			System.err.println("Couldn't open log file "+params.getOutDir()+"/"+params.getBaseName()+".log"+" for writing.");
			System.err.println(e.getMessage());
			System.exit(1);
		}
	    ROOTLOGGER.setLevel(Level.INFO);

	    // TODO now using the apache common logging framework which acts as a meta framework for other frameworks such
	    //      as log4j or java logging. We always should instantiate loggers from apache commons (LogFactory.getLog()).
	    //      Left to do is still configure the logging in CRK properly by using the apache commons mechanism. At the moment
	    //      the logging configuration is done using log4j/java logger mechanisms.

	}
	
	public void loadConfigFile() {
		// loading settings from config file
		File userConfigFile = new File(System.getProperty("user.home"),CRKParams.CONFIG_FILE_NAME);  
		try {
			if (userConfigFile.exists()) {
				LOGGER.info("Loading user configuration file " + userConfigFile);
				params.readConfigFile(userConfigFile);
			}
		} catch (IOException e) {
			LOGGER.fatal("Error while reading from config file " + userConfigFile + ": " + e.getMessage());
			System.exit(1);
		}

	}

	public void doLoadPdb() throws CRKException {
		params.getProgressLog().println("Loading PDB data: "+(params.getInFile()==null?params.getPdbCode():params.getInFile().getName()));
		writeStep("Calculating Interfaces");
		pdb = null;
		File cifFile = null; // if given a pdb code in command line we will store the cif file here
		try {
			if (!params.isInputAFile()) {
				cifFile = new File(params.getOutDir(),params.getPdbCode() + ".cif");
				try {
					PdbAsymUnit.grabCifFile(params.getLocalCifDir(), params.getPdbFtpCifUrl(), params.getPdbCode(), cifFile, params.isUseOnlinePdb());
				} catch(IOException e) {
					throw new CRKException(e,"Couldn't get cif file for code "+params.getPdbCode()+" from ftp or couldn't uncompress it. Error: "+e.getMessage(),true);
				}
					
			} else {
				cifFile = params.getInFile();
			}
			// we parse PDB files with no X padding if no SEQRES is found. Otherwise matching to uniprot doesn't work in many cases
			pdb = new PdbAsymUnit(cifFile, PdbAsymUnit.DEFAULT_MODEL, false);
		} catch (FileFormatException e) {
			throw new CRKException(e,"File format error: "+e.getMessage(),true);
		} catch (PdbLoadException e) {
			throw new CRKException(e,"Couldn't load file "+cifFile+". Error: "+e.getMessage(),true);
		} catch (IOException e) {
			throw new CRKException(e,"Problems reading PDB data from "+cifFile+". Error: "+e.getMessage(),true);
		}
		
		if (pdb.getCrystalCell()==null) {
			LOGGER.warn("No crystal information found in source "+params.getPdbCode()+". Only asymmetric unit interfaces will be calculated.");
		}
		
		// we strip the H atoms: surface calculations should not have them (otherwise comparisons of structures with/without H arn't good)
		pdb.removeHatoms();
		
		// for the webui
		wuiAdaptor = new WebUIDataAdaptor();
		wuiAdaptor.setParams(params);
		wuiAdaptor.setPdbMetadata(pdb);
		
	}
	
	public void doLoadInterfacesFromFile() throws CRKException {
		try {
			params.getProgressLog().println("Loading interfaces enumeration from file "+params.getInterfSerFile());
			LOGGER.info("Loading interfaces enumeration from file "+params.getInterfSerFile());
			interfaces = (ChainInterfaceList)Goodies.readFromFile(params.getInterfSerFile());
		} catch (ClassNotFoundException e) {
			throw new CRKException(e,"Couldn't load interface enumeration binary file: "+e.getMessage(),true);
		} catch (IOException e) {
			throw new CRKException(e,"Couldn't load interface enumeration binary file: "+e.getMessage(),true);
		}
		
		if (!pdb.getPdbCode().equals(interfaces.get(1).getFirstMolecule().getPdbCode())) {
			throw new CRKException(null,"PDB codes of given PDB entry/file and given interface enumeration binary file don't match.",true);
		}
		
	}

	public void doFindInterfaces() throws CRKException {

		if (params.isUsePisa()) {
			params.getProgressLog().println("Getting PISA interfaces...");
			LOGGER.info("Interfaces from PISA.");
			PisaConnection pc = new PisaConnection();
			List<String> pdbCodes = new ArrayList<String>();
			pdbCodes.add(pdb.getPdbCode());
			try {
				interfaces = pc.getInterfacesDescription(pdbCodes).get(pdb.getPdbCode()).convertToChainInterfaceList(pdb);
			} catch(SAXException e) {
				throw new CRKException(e,"Error while reading PISA xml file"+e.getMessage(),true);
			} catch(IOException e) {
				throw new CRKException(e,"Error while retrieving PISA xml file: "+e.getMessage(),true);
			}
		} else {
			params.getProgressLog().println("Calculating possible interfaces...");
			try {
				if (params.isUseNaccess()) {
					interfaces = pdb.getAllInterfaces(CRKParams.INTERFACE_DIST_CUTOFF, params.getNaccessExe(), 0, 0, true, false, false);
					LOGGER.info("Interfaces calculated with NACCESS.");
				} else {
					interfaces = pdb.getAllInterfaces(CRKParams.INTERFACE_DIST_CUTOFF, null, params.getnSpherePointsASAcalc(), params.getNumThreads(), true, false, false);
					LOGGER.info("Interfaces calculated with "+params.getnSpherePointsASAcalc()+" sphere points.");
				}
			} catch (IOException e) {
				throw new CRKException(e,"Couldn't run NACCESS for BSA calculation. Error: "+e.getMessage(),true);
			}
		}

		params.getProgressLog().println("Done");


		// checking for clashes
		if (!params.isUsePisa() && interfaces.hasInterfacesWithClashes()) {
			String msg = "Clashes found in some of the interfaces (atoms distance below "+AICGraph.CLASH_DISTANCE+"):";
			List<ChainInterface> clashyInterfs = interfaces.getInterfacesWithClashes();
			for (ChainInterface clashyInterf:clashyInterfs) {
				msg+=("\nInterface: "+clashyInterf.getFirstMolecule().getPdbChainCode()+"+"
						+clashyInterf.getSecondMolecule().getPdbChainCode()+" ("+
						SpaceGroup.getAlgebraicFromMatrix(clashyInterf.getSecondTransf())+
						") Clashes: "+clashyInterf.getNumClashes());
			}
			msg+=("\nThis is most likely an error in the structure. If you think the structure is correct, please report a bug.");
			// we used to throw a fatal exception and exit here, but we decided to simply warn and go ahead 
			LOGGER.warn(msg);
			System.err.println(msg);
			//throw new CRKException(null, msg, true);
		}

		interfaces.calcRimAndCores(params.getCAcutoffForGeom());
		
		try {
			PrintStream interfLogPS = new PrintStream(params.getOutputFile(".interfaces"));
			interfaces.printTabular(interfLogPS, params.getJobName());
			interfLogPS.close();
		} catch(IOException	e) {
			throw new CRKException(e,"Couldn't log interfaces description to file: "+e.getMessage(),false);
		}

		try {
			Goodies.serialize(params.getOutputFile(".interfaces.dat"),interfaces);
		} catch (IOException e) {
			throw new CRKException(e,"Couldn't write serialized ChainInterfaceList object to file: "+e.getMessage(),false);
		}
		
	}
	
	public void doGeomScoring() throws CRKException {
		if (interfaces.getNumInterfaces()==0) {
			// no interfaces found at all, can happen e.g. in NMR structure with 1 chain, e.g. 1nmr
			LOGGER.info("No interfaces found, nothing to analyse.");
			params.getProgressLog().println("No interfaces found, nothing to analyse.");
			// we still continue so that the web interface can pick it up too
			return;
		}
		
		try {
			gps = new ArrayList<GeometryPredictor>();
			PrintStream scoreGeomPS = new PrintStream(params.getOutputFile(CRKParams.GEOMETRY_FILE_SUFFIX+".scores"));
			GeometryPredictor.printScoringHeaders(scoreGeomPS);
			for (ChainInterface interf:interfaces) {
				GeometryPredictor gp = new GeometryPredictor(interf);
				gps.add(gp);
				gp.setBsaToAsaCutoff(params.getCAcutoffForGeom());
				gp.setMinCoreSizeForBio(params.getMinCoreSizeForBio());
				gp.printScores(scoreGeomPS);
				gp.writePdbFile(params.getOutputFile("."+interf.getId()+".rimcore.pdb"));
			}
			scoreGeomPS.close();
						
			// for the webui
			wuiAdaptor.setInterfaces(interfaces);
			wuiAdaptor.setGeometryScores(gps);
		} catch (IOException e) {
			throw new CRKException(e, "Couldn't write interface geometry scores or related pdb files. "+e.getMessage(),true);
		}
	}
	
	public void doWritePymolFiles() throws CRKException {
		if (interfaces.getNumInterfaces()==0) return;
		
		PymolRunner pr = null;
		params.getProgressLog().println("Writing pymol files");
		writeStep("Generating Thumbnails and PyMol Files");
		try {
			pr = new PymolRunner(params.getPymolExe());
			pr.readColorsFromPropertiesFile(CRKParams.COLORS_PROPERTIES_IS);
			pr.readColorMappingsFromResourceFile(CRKParams.PYMOL_COLOR_MAPPINGS_IS);

		} catch (IOException e) {
			LOGGER.error("Couldn't read colors file. Won't generate thumbnails or pse/pml files");
			pr = null;
		}
		
		if (pr!=null) {
			try {
				for (ChainInterface interf:interfaces) {
					pr.generateInterfPngPsePml(interf, 
							params.getOutputFile("."+interf.getId()+".rimcore.pdb"), 
							params.getOutputFile("."+interf.getId()+".pse"),
							params.getOutputFile("."+interf.getId()+".pml"),
							params.getBaseName()+"."+interf.getId());
				}
			} catch (IOException e) {
				throw new CRKException(e, "Couldn't write thumbnails or pymol pse/pml files. "+e.getMessage(),true);
			} catch (InterruptedException e) {
				throw new CRKException(e, "Couldn't generate thumbnails or pse/pml files, pymol thread interrupted: "+e.getMessage(),false);
			}
			wuiAdaptor.setJmolScripts(interfaces, pr);
		}
	}
	
	private void findUniqueChains() {
		String msg = "Unique sequences for "+params.getJobName()+":";
		int i = 1;
		for (String representativeChain:pdb.getAllRepChains()) {
			List<String> entity = pdb.getSeqIdenticalGroup(representativeChain);
			msg+=" "+i+":";
			for (String chain:entity) {
				msg+=" "+chain;
			}
			i++;
		}
		LOGGER.info(msg);
	}
	
	public void doLoadEvolContextFromFile() throws CRKException {
		if (interfaces.getNumInterfaces()==0) return;
		
		findUniqueChains();
		
		try {
			params.getProgressLog().println("Loading chain evolutionary scores from file "+params.getChainEvContextSerFile());
			LOGGER.info("Loading chain evolutionary scores from file "+params.getChainEvContextSerFile());
			cecs = (ChainEvolContextList)Goodies.readFromFile(params.getChainEvContextSerFile());
		} catch (ClassNotFoundException e) {
			throw new CRKException(e,"Couldn't load interface evolutionary context binary file: "+e.getMessage(),true);
		} catch(IOException e) {
			throw new CRKException(e,"Couldn't load interface evolutionary context binary file: "+e.getMessage(),true);
		}

		// TODO check whether this looks compatible with the interfaces that we have
	}
	
	public void doFindEvolContext() throws CRKException {
		if (interfaces.getNumInterfaces()==0) return;
		
		findUniqueChains();
		
		cecs = new ChainEvolContextList(pdb,params.getJobName());
		
		// a) getting the uniprot ids corresponding to the query (the pdb sequence)
		for (ChainEvolContext chainEvCont:cecs.getAllChainEvolContext()) {
			params.getProgressLog().println("Finding query's chain "+chainEvCont.getRepresentativeChainCode()+" uniprot mapping through SIFTS or blasting");
			writeStep("Finding Homologues and Calculating Entropies");
			try {
				chainEvCont.retrieveQueryData(params.getSiftsFile(), params.getBlastBinDir(), params.getBlastDbDir(), params.getBlastDb(), params.getNumThreads(),params.getPdb2uniprotIdThreshold(),params.getPdb2uniprotQcovThreshold());
			} catch (BlastException e) {
				throw new CRKException(e,"Couldn't run blast to retrieve query's uniprot mapping: "+e.getMessage(),true);
			} catch (IOException e) {
				throw new CRKException(e,"Problems while retrieving query data: "+e.getMessage(),true);
			} catch (InterruptedException e) {
				throw new CRKException(e,"Thread interrupted while running blast for retrieving query data: "+e.getMessage(),true);
			} catch (Exception e) { // for any kind of exceptions thrown while connecting through uniprot JAPI
				throw new CRKException(e,"Problems while retrieving query data through Uniprot JAPI. Is Uniprot server down?\n"+e.getMessage(),true);
			}

			if (!chainEvCont.hasQueryMatch()) {
				// no query uniprot match, we do nothing with this sequence
				// TODO should we go ahead and blast with the PDB sequence? that would require quite a few changes in the code
				continue;
			} 
			
			// b) getting the homologs and sequence data 
			params.getProgressLog().println("Blasting for homologues...");
			File blastCacheFile = null;
			if (params.getBlastCacheDir()!=null) {
				blastCacheFile = new File(params.getBlastCacheDir(),chainEvCont.getQuery().getUniId()+".blast.xml"); 
			}
			try {
				chainEvCont.retrieveHomologs(params, blastCacheFile);
				LOGGER.info("Uniprot version used: "+chainEvCont.getUniprotVer());
				
				// for web ui uniprot ver will be set only when at least one sequence has uniprot match
				// it will be redundantly set several times actually, TODO we should do it only once
				// if not a single sequence has match then it will be null
				wuiAdaptor.getRunParametersItem().setUniprotVer(chainEvCont.getUniprotVer());

			} catch (UniprotVerMisMatchException e) {
				throw new CRKException(e, "Mismatch of Uniprot versions! "+e.getMessage(), true);
			} catch (BlastException e) {
				throw new CRKException(e,"Couldn't run blast to retrieve homologs: "+e.getMessage() ,true);
			} catch (IOException e) {
				throw new CRKException(e,"Problem while blasting for sequence homologs: "+e.getMessage(),true);
			} catch (InterruptedException e) {
				throw new CRKException(e,"Thread interrupted while blasting for sequence homologs: "+e.getMessage(),true);
			}

			String msg = "Retrieving UniprotKB data";

			params.getProgressLog().println(msg);
			try {
				chainEvCont.retrieveHomologsData();
			} catch (UniprotVerMisMatchException e) {
				throw new CRKException(e, "Mismatch of Uniprot versions! "+e.getMessage(), true);
			} catch (IOException e) {
				String errmsg = "Problem while retrieving homologs data through Uniprot JAPI";
				throw new CRKException(e, errmsg+": "+e.getMessage(),true);
			} catch (Exception e) { // for any kind of exceptions thrown while connecting through uniprot JAPI
				throw new CRKException(e, "Problems while retrieving homologs data through Uniprot JAPI. Is Uniprot server down?\n"+e.getMessage(),true);
			}

			// filtering optionally for domain of life
			if (params.isFilterByDomain()) chainEvCont.filterToSameDomainOfLife();
			
			// remove redundancy
			chainEvCont.removeRedundancy();

			// skimming so that there's not too many sequences for selecton
			chainEvCont.skimList(params.getMaxNumSeqs());
			
			// c) align
			params.getProgressLog().println("Aligning protein sequences with t_coffee...");
			try {
				chainEvCont.align(params.getTcoffeeBin(), params.isUseTcoffeeVeryFastMode(), params.getNumThreads());
			} catch (TcoffeeException e) {
				throw new CRKException(e, "Couldn't run t_coffee to align protein sequences: "+e.getMessage(), true);
			} catch (IOException e) {
				throw new CRKException(e, "Problems while running t_coffee to align protein sequences: "+e.getMessage(),true);
			} catch (InterruptedException e) {
				throw new CRKException(e, "Thread interrupted while running t_coffee to align protein sequences: "+e.getMessage(),true);
			}


			File outFile = null;
			try {
				// writing homolog sequences to file
				outFile = params.getOutputFile("."+chainEvCont.getRepresentativeChainCode()+".fa");
				chainEvCont.writeHomologSeqsToFile(outFile);

				// printing summary to file
				outFile = params.getOutputFile("."+chainEvCont.getRepresentativeChainCode()+".log");
				PrintStream log = new PrintStream(outFile);
				chainEvCont.printSummary(log);
				log.close();
				// writing the alignment to file
				outFile = params.getOutputFile("."+chainEvCont.getRepresentativeChainCode()+".aln");
				chainEvCont.writeAlignmentToFile(outFile);

			} catch(FileNotFoundException e){
				LOGGER.error("Couldn't write file "+outFile);
				LOGGER.error(e.getMessage());
			}


			// d) computing entropies
			chainEvCont.computeEntropies(params.getReducedAlphabet());


			try {
				// writing the conservation scores (entropies/kaks) log file
				outFile = params.getOutputFile("."+chainEvCont.getRepresentativeChainCode()+CRKParams.ENTROPIES_FILE_SUFFIX);
				PrintStream conservScoLog = new PrintStream(outFile);
				chainEvCont.printConservationScores(conservScoLog, ScoringType.ENTROPY, pdb);
				conservScoLog.close();
			} catch (FileNotFoundException e) {
				LOGGER.error("Could not write the scores log file "+outFile);
				LOGGER.error(e.getMessage());
			}
		}
		
		try {
			Goodies.serialize(params.getOutputFile(".chainevolcontext.dat"),cecs);
		} catch (IOException e) {
			throw new CRKException(e,"Couldn't write serialized ChainEvolContextList object to file: "+e.getMessage(),false);
		}
		
	}
	
	public void doEvolScoring() throws CRKException {
		if (interfaces.getNumInterfaces()==0) return;

		interfaces.calcRimAndCores(params.getCAcutoffForRimCore());
		
		iecList = new InterfaceEvolContextList(params.getJobName(), params.getMinHomologsCutoff(),  
				params.getHomSoftIdCutoff(), params.getQueryCoverageCutoff(), params.getMaxNumSeqs());
		iecList.addAll(interfaces,cecs);

		writeStep("Scoring Interfaces");
		
		if (params.isDoScoreEntropies()) {
			try {
				//interfaces.calcRimAndCores(params.getCAcutoffForRimCore());
				iecList.setRimCorePredBsaToAsaCutoff(params.getCAcutoffForRimCore());
				iecList.setCallCutoff(params.getEntrCallCutoff());
				iecList.setZscoreCutoff(params.getZscoreCutoff());
				PrintStream scoreEntrPS = new PrintStream(params.getOutputFile(CRKParams.ENTROPIES_FILE_SUFFIX+".scores"));
				// entropy nw
				iecList.scoreEntropy(false);
				iecList.printScoresTable(scoreEntrPS);
				wuiAdaptor.add(iecList);
				iecList.resetCalls();
				iecList.writeScoresPDBFiles(params,CRKParams.ENTROPIES_FILE_SUFFIX+".pdb");
				scoreEntrPS.close();
				iecList.resetCalls();
				// z-scores
				//interfaces.calcRimAndCores(params.getCAcutoffForZscore());
				PrintStream scoreZscorePS = new PrintStream(params.getOutputFile(CRKParams.ZSCORES_FILE_SUFFIX+".scores"));
				iecList.setZPredBsaToAsaCutoff(params.getCAcutoffForZscore());
				iecList.scoreZscore();
				iecList.printZscoresTable(scoreZscorePS);
				wuiAdaptor.add(iecList);
				iecList.resetCalls();
				scoreZscorePS.close();

			} catch (IOException e) {
				throw new CRKException(e, "Couldn't write final interface entropy scores or related PDB files. "+e.getMessage(),true);
			} 
		}

		
	}
	
	public void doCombinedScoring() throws CRKException {
		if (interfaces.getNumInterfaces()==0) return;
		
		try {
		
			iecList.setCallCutoff(params.getEntrCallCutoff());
			iecList.setZscoreCutoff(params.getZscoreCutoff());
			//interfaces.calcRimAndCores(params.getCAcutoffForRimCore());
			iecList.setRimCorePredBsaToAsaCutoff(params.getCAcutoffForRimCore());
			iecList.scoreEntropy(false);
			//interfaces.calcRimAndCores(params.getCAcutoffForZscore());
			iecList.setZPredBsaToAsaCutoff(params.getCAcutoffForZscore());
			iecList.scoreZscore();

			List<CombinedPredictor> cps = new ArrayList<CombinedPredictor>();

			PrintStream scoreCombPS = new PrintStream(params.getOutputFile(CRKParams.COMBINED_FILE_SUFFIX+".scores"));
			CombinedPredictor.printScoringHeaders(scoreCombPS);
			for (int i=0;i<iecList.size();i++) {
				CombinedPredictor cp = 
						new CombinedPredictor(iecList.get(i), gps.get(i), iecList.getEvolRimCorePredictor(i), iecList.getEvolInterfZPredictor(i));
				cps.add(cp);
				cp.printScoresLine(scoreCombPS);
			}
			scoreCombPS.close();

			wuiAdaptor.setCombinedPredictors(cps);

		} catch (IOException e) {
			throw new CRKException(e,"Couldn't write final combined scores file. "+e.getMessage(),true);
		}
		params.getProgressLog().println("Done scoring");
	}
	
	private void writeStep(String text) {
		if (stepsLogFile==null) return;
		try {
			PrintStream stepsLog = new PrintStream(stepsLogFile);
			stepsLog.println("step_num="+stepCount);
			stepsLog.println("step="+text);
			stepsLog.println("step_total="+STEPS_TOTAL);
			stepCount++;
		} catch(FileNotFoundException e) {
			LOGGER.error("Couldn't write to steps log file "+stepsLogFile);
		}
	}
	
	/**
	 * The main of CRK 
	 */
	public static void main(String[] args) {
		
		CRKMain crkMain = new CRKMain();

		// we first parse command line and print errors to stderr (logging is not set up yet)
		try {
			crkMain.params.parseCommandLine(args);
		} catch (CRKException e) {
			System.err.println(e.getMessage());
			e.exitIfFatal(1);
		}
		try {
			// turn off jaligner logging (we only use NeedlemanWunschGotoh from that package)
			// (for some reason this doesn't work if condensated into one line, it seems that one needs to instantiate the logger and then call setLevel)
			// (and even weirder, for some reason it doesn't work if you put the code in its own separate method!)
			java.util.logging.Logger jalLogger = java.util.logging.Logger.getLogger("NeedlemanWunschGotoh");
			jalLogger.setLevel(java.util.logging.Level.OFF);

			crkMain.setUpLogging();

			crkMain.loadConfigFile();
			
			try {
				LOGGER.info("Running in host "+InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				LOGGER.warn("Could not determine host where we are running.");
			}
			
			// 0 load pdb
			crkMain.doLoadPdb();

			// 1 finding interfaces
			if (crkMain.params.getInterfSerFile()!=null) {
				crkMain.doLoadInterfacesFromFile();
			} else {
				crkMain.doFindInterfaces();
			}
			crkMain.doGeomScoring(); 
			
			if (crkMain.params.isDoScoreEntropies()) {
				// 2 finding evolutionary context
				if (crkMain.params.getChainEvContextSerFile()!=null) {
					crkMain.doLoadEvolContextFromFile();
				} else {
					crkMain.doFindEvolContext();
				}

				// 3 scoring
				crkMain.doEvolScoring();
				
				// 4 combined scoring
				crkMain.doCombinedScoring();
			}
			
			
			if (crkMain.params.isGenerateThumbnails()) {
				// 5 writing pymol files
				crkMain.doWritePymolFiles();
				
				// 6 writing out the serialized file for web ui
				crkMain.wuiAdaptor.writePdbScoreItemFile(crkMain.params.getOutputFile(".webui.dat"));
			}


		} catch (CRKException e) {
			e.log(LOGGER);
			e.exitIfFatal(1);
		} 
//		catch (Exception e) {
//			e.printStackTrace();
//
//			String stack = "";
//			for (StackTraceElement el:e.getStackTrace()) {
//				stack+="\tat "+el.toString()+"\n";				
//			}
//			LOGGER.fatal("Unexpected error. Exiting.\n"+e+"\n"+stack);
//			System.exit(1);
//		}
	}
	

}
