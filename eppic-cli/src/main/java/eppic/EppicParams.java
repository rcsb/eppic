package eppic;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eppic.commons.sequence.AminoAcid;

public class EppicParams {
	
	// CONSTANTS
	public static final String     PROGRAM_NAME    = "eppic";
	public static final String	   PROGRAM_VERSION = "3.0-alpha";
	public static final String 	   CONTACT_EMAIL   = "eppic@systemsx.ch";
	
	private static final Pattern   PDBCODE_PATTERN = Pattern.compile("^\\d\\w\\w\\w$");
	public static final String     CONFIG_FILE_NAME = ".eppic.conf";
	
	public static final String	   FINISHED_FILE_NAME = "finished";
	
	protected static final String  ENTROPIES_FILE_SUFFIX = ".entropies";
	protected static final String  INTERFACES_FILE_SUFFIX = ".interfaces";
	protected static final String  CONTACTS_FILE_SUFFIX = ".contacts";
	protected static final String  INTERFACESDAT_FILE_SUFFIX = ".interfaces.dat";
	protected static final String  CHAINEVCONTEXTDAT_FILE_SUFFIX = ".chainevolcontext.dat";
	protected static final String  SERIALIZED_MODEL_FILE_SUFFIX = ".webui.dat";
	protected static final String  SCORES_FILE_SUFFIX = ".scores";
	protected static final String  STEPS_LOG_FILE_SUFFIX = ".steps.log";
	protected static final String  BLAST_CACHE_FILE_SUFFIX = ".blast.xml.gz";
	protected static final String  PDB_BIOUNIT_ASSIGN_FILE_SUFFIX = ".pdb-biounit.comparison";
	
	// the size of png files generated by generateChainPse (icons for WUI)
	protected static final int	   COLOR_ENTROPIES_ICON_WIDTH = 40;
	protected static final int	   COLOR_ENTROPIES_ICON_HEIGHT = 40;
	
	// the default used to be 5.9 to match PISA, now reduced to 5.5 in order to reduce
	// the number of useless too small interfaces and thus make PDB-wide calculations a bit
	// more optimal
	public static final double     INTERFACE_DIST_CUTOFF = 5.5;
	// min interface area to keep interfaces: interfaces below this value are discarded
	// value is "guesstimated" from a reasonable experimental error and by looking at the histogram of 
	// area distributions for the whole PDB (in Oct 2013): it peaks at ~15, then has a small shoulder 
	// until 35 and decays monotonically after that
	public static final double     MIN_INTERFACE_AREA_TO_KEEP = 35;
	// if any interface has this number of clashes we'll abort with an error
	public static final int		   NUM_CLASHES_FOR_ERROR = 30;
	// shorter chains will be considered peptides
	public static final int	       PEPTIDE_LENGTH_CUTOFF = 20; 
	// 10% maximum allowed unreliable residues for calling nopred
	public static final double     MAX_ALLOWED_UNREL_RES = 0.1; 
	// minimum number of core residues per interface member to calculate evol score (if fewer 
	// we don't calculate anything because it would be too unreliable statistically)
	public static final int        MIN_NUMBER_CORE_RESIDUES_EVOL_SCORE = 4;
	// value to use when core/rim ratio is infinity (rim score=0), some arbitrary large value, unlikely to happen in realistic cases
	public static final double     SCORERATIO_INFINITY_VALUE = 1000;
	// for sequences (strictly) below this length value no blast will be performed
	public static final int		   MIN_SEQ_LENGTH_FOR_BLASTING = 10;
	// the maximum length of an engineered insertion when a single chain maps to multiple segments with a single UniProt reference
	public static final int		   NUM_GAP_RES_FOR_CHIMERIC_FUSION = 10;
	// the hard limits aka "duarte" limits
	// max limit based on 1pre (bio with 2290 and 0+2 cores) and 2vg5 interface 2 (xtal with 2070 and 0+0 cores) 
	public static final double	   MAX_AREA_XTALCALL = 2200; 
	public static final double 	   MIN_AREA_BIOCALL  = 400;   
	// interface clustering constants
	public static final double 	   CLUSTERING_RMSD_CUTOFF = 2.0;
	public static final int 	   CLUSTERING_MINATOMS = 10;
	public static final String     CLUSTERING_ATOM_TYPE = "CA";
	// the strategies for core-surface scoring
	public static final int 	   CORE_SURFACE_SCORE_STRATEGY_CLASSIC = 0;
	public static final int 	   CORE_SURFACE_SCORE_STRATEGY_ZSCORE = 1;
	
	// the distance for two atoms between chains to be considered a clashing pair
	public static final double 	   CLASH_DISTANCE = 1.5;
	// a generic low distance for a close interaction (electrostatic, salt bridge, semi-covalent, covalent) for 2 atoms between chains
	// see review Harding MM, Acta Crystallographica 2006 - 
	// actually distances can be up to 2.4 (or even more) in some cases, taking a conservative approach here 
	public static final double 	   CLOSE_INTERACTION_DIST = 2.1;
	// disulfide bridges
	public static final double 	   DISULFIDE_BRIDGE_DIST = 2.05;
	public static final double     DISULFIDE_BRIDGE_DIST_SIGMA = 0.1;
	
	// the PDB biounit that we take as the PDB biounit annotation: since introduction of Biojava 
	// we have decided to use biounit 1 (whatever its type) and ignore the rest
	public static final int		   PDB_BIOUNIT_TO_USE = 1;
	
	// PROPERTY FILES
	protected static final InputStream COLORS_PROPERTIES_IS = EppicParams.class.getResourceAsStream("/eppic/chain_colors.dat");
	protected static final InputStream PYMOL_COLOR_MAPPINGS_IS = EppicParams.class.getResourceAsStream("/eppic/pymol.colors");
	
	// DEFAULTS FOR COMMAND LINE PARAMETERS
	public static final double    DEF_HOM_SOFT_ID_CUTOFF = 0.6;
	public static final double    DEF_HOM_HARD_ID_CUTOFF = 0.5;

	private static final int      DEF_NUMTHREADS = Runtime.getRuntime().availableProcessors();
	
	private static final String   DEF_OUT_DIR = ".";
	
	// default entropy calculation 
	public static final int       DEF_ENTROPY_ALPHABET = 10;

	// default cutoffs for the final bio/xtal call
	public static final int       DEF_MIN_CORE_SIZE_FOR_BIO = 6;
	public static final double    DEF_CORERIM_SCORE_CUTOFF = 0.75;
	public static final double    DEF_CORESURF_SCORE_CUTOFF = -1.00;
	
	// default core assignment thresholds
	public static final double    DEF_CA_CUTOFF_FOR_GEOM = 0.95;
	public static final double    DEF_CA_CUTOFF_FOR_RIMCORE = 0.70;
	public static final double    DEF_CA_CUTOFF_FOR_ZSCORE = 0.70;

	private static final int      DEF_MAX_NUM_SEQUENCES = 100;
	
	public static final int 	  DEF_NSPHEREPOINTS_ASA_CALC = 3000;
	
	protected static final HomologsSearchMode DEF_HOMOLOGS_SEARCH_MODE = HomologsSearchMode.LOCAL;
	
	// DEFAULTS FOR CONFIG FILE ASSIGNABLE CONSTANTS
	// defaults for pdb data location
	private static final String   DEF_LOCAL_CIF_DIR = "/pdbdata/pdb/data/structures/all/mmCIF";
	private static final String   DEF_PDB_FTP_CIF_URL = "ftp://ftp.wwpdb.org/pub/pdb/data/structures/all/mmCIF/";
	private static final boolean  DEF_USE_ONLINE_PDB = false;

	// default sifts file location
	private static final String   DEF_SIFTS_FILE = "ftp://ftp.ebi.ac.uk/pub/databases/msd/sifts/text/pdb_chain_uniprot.lst";	
	private static final boolean  DEF_USE_SIFTS = true;
	
	// default blast settings
	private static final File     DEF_BLASTP_BIN = new File("/usr/bin/blastp"); // from blast+ package
	private static final File     DEF_BLASTCLUST_BIN = new File("/usr/bin/blastclust"); // from legacy blast package
	private static final String   DEF_BLAST_DATA_DIR = "/usr/share/blast";
	
	// default aligner programs execs: blank files, so that we can control that one and only one is set (see checkConfigFileInput)
	private static final File	  DEF_TCOFFEE_BIN = new File("");
	private static final File	  DEF_CLUSTALO_BIN = new File("");
	
	// default pymol exec
	private static final File	  DEF_PYMOL_EXE = new File("/usr/bin/pymol");
	
	// default hbplus exec
	private static final File     DEF_HBPLUS_EXE = new File("/usr/bin/hbplus");
	
	// default minimum ASA for calling a residue surface
	public static final double    DEF_MIN_ASA_FOR_SURFACE = 5;
	// default minimum number of atoms for a cofactor to be considered for ASA calculations purposes, if -1 all ignored
	public static final int		  DEF_MIN_SIZE_COFACTOR_FOR_ASA = 40;
	
	// default cutoffs
	private static final double   DEF_QUERY_COVERAGE_CUTOFF = 0.85;
	public static final int       DEF_MIN_NUM_SEQUENCES = 10;
	private static final double	  DEF_HOM_ID_STEP = 0.05;
	private static final double   DEF_MIN_QUERY_COV_FOR_IDENTICALS_REMOVAL = 0.85;
	
	// default pdb2uniprot mapping blast thresholds
	private static final double   DEF_PDB2UNIPROT_ID_THRESHOLD = 0.90;
	private static final double   DEF_PDB2UNIPROT_QCOV_THRESHOLD = 0.85;
	
	private static final boolean  DEF_ALLOW_CHIMERAS = false;
	
	private static final boolean  DEF_USE_PDB_CODE_FROM_FILE = false;

	// default cache dirs
	private static final String   DEF_BLAST_CACHE_DIR = null;
	private static final String   DEF_ALN_CACHE_DIR = null;
	
	// default use uniparc
	private static final boolean  DEF_USE_UNIPARC = true;
	
	// default use pdb res serials for output
	public static final boolean   DEF_USE_PDB_RES_SER = true;
	
	// default core-surface score strategy: 0 for classic, 1 for straight z-scores
	private static final int 	  DEF_CORE_SURFACE_SCORE_STRATEGY = CORE_SURFACE_SCORE_STRATEGY_ZSCORE;
	
	// FIELDS
	
	// the parameters
	private String inputStr;
	private String pdbCode;
	private boolean doEvolScoring;
	private double homSoftIdCutoff;
	private double homHardIdCutoff;
	private double homIdStep;
	private String baseName;
	private File outDir;
	private int numThreads;
	private int reducedAlphabet;
	
	private double caCutoffForGeom;
	private double caCutoffForRimCore;
	private double caCutoffForZscore;
	
	private int    minCoreSizeForBio;
	
	private int maxNumSeqs;
	
	private int nSpherePointsASAcalc;

	private double coreRimScoreCutoff;
	
	private double coreSurfScoreCutoff;
	
	private File interfSerFile;
	private File chainEvContextSerFile;
	
	private boolean generateInterfacesPdbFiles;
	private boolean generateThumbnails;
	
	private boolean generateModelSerializedFile;
	
	private File progressLogFile;
	private PrintStream progressLog;
	
	private File configFile;
	
	private boolean debug;
	
	private HomologsSearchMode homologsSearchMode;
	
	private boolean filterByDomain;
	
	// some other fields
	private File inFile;
	
	// fields assignable from config file
	private String   localCifDir;
	private String   pdbFtpCifUrl;
	private boolean  useOnlinePdb;
	
	private String   siftsFile;
	
	private boolean  useSifts;
	
	private File     blastclustBin;
	private File     blastpBin;
	
	private String   blastDataDir; // dir with blosum matrices only needed for blastclust
	
	private File     tcoffeeBin;
	private File	 clustaloBin;
	
	private File     selectonBin;
	private double   selectonEpsilon;

	private File	 pymolExe;
	
	private double minAsaForSurface;
	private int minSizeCofactorForAsa;	
	
	private double   queryCoverageCutoff;
	private int      minNumSeqs;
	
	private double   minQueryCovForIdenticalsRemoval;
	
	private double   pdb2uniprotIdThreshold;
	private double   pdb2uniprotQcovThreshold;
	
	private boolean  allowChimeras;
	
	private boolean  usePdbCodeFromFile;
			
	private String   blastCacheDir;
	private String   alnCacheDir;
	
	private boolean  useUniparc;
	
	private boolean  usePdbResSer;
	
	private int coreSurfaceScoreStrategy;
	
	// and finally the ones with no defaults
	private String   blastDbDir; // no default
	private String   blastDb;    // no default
	
	private String   localUniprotDbName; // no default
	private File hbplusExe;
	
	/**
	 * 
	 */
	public EppicParams() {
		setDefaults();
	}
	
	private void setDefaults() {
		
		this.inputStr = null;
		this.pdbCode = null;
		this.doEvolScoring = false;
		this.homSoftIdCutoff = DEF_HOM_SOFT_ID_CUTOFF;
		this.homHardIdCutoff = DEF_HOM_HARD_ID_CUTOFF;
		this.homIdStep = DEF_HOM_ID_STEP;
		this.baseName = null;
		this.outDir = new File(DEF_OUT_DIR);
		this.numThreads = DEF_NUMTHREADS;
		this.reducedAlphabet = DEF_ENTROPY_ALPHABET;
		this.caCutoffForGeom = DEF_CA_CUTOFF_FOR_GEOM;
		this.caCutoffForRimCore = DEF_CA_CUTOFF_FOR_RIMCORE;
		this.caCutoffForZscore = DEF_CA_CUTOFF_FOR_ZSCORE;
		this.minCoreSizeForBio = DEF_MIN_CORE_SIZE_FOR_BIO;
		this.maxNumSeqs = DEF_MAX_NUM_SEQUENCES;
		this.nSpherePointsASAcalc = DEF_NSPHEREPOINTS_ASA_CALC;
		this.coreRimScoreCutoff = DEF_CORERIM_SCORE_CUTOFF;
		this.coreSurfScoreCutoff = DEF_CORESURF_SCORE_CUTOFF;
		this.interfSerFile = null;
		this.chainEvContextSerFile = null;
		this.generateInterfacesPdbFiles = false;
		this.generateThumbnails = false;
		this.generateModelSerializedFile = false;
		this.progressLog = System.out;
		this.debug = false;
		this.homologsSearchMode = DEF_HOMOLOGS_SEARCH_MODE;
		this.filterByDomain = false;

	}
	
	public void parseCommandLine(String[] args, String programName, String help) {
	

		Getopt g = new Getopt(programName, args, "i:sa:b:o:r:e:c:z:m:x:y:d:D:q:H:G:OA:I:C:plwL:g:uh?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'i':
				inputStr = g.getOptarg();
				setInput();
				break;
			case 's':
				doEvolScoring = true;
				break;
			case 'a':
				numThreads = Integer.parseInt(g.getOptarg());
				break;
			case 'b':
				baseName = g.getOptarg();
				break;				
			case 'o':
				outDir = new File(g.getOptarg());
				break;
			case 'r':
				reducedAlphabet = Integer.parseInt(g.getOptarg()); 
				break;
			case 'e':
				caCutoffForGeom = Double.parseDouble(g.getOptarg());
				break;
			case 'c':
				caCutoffForRimCore = Double.parseDouble(g.getOptarg());
				break;				
			case 'z':
				caCutoffForZscore = Double.parseDouble(g.getOptarg());
				break;				
			case 'm':
				minCoreSizeForBio = Integer.parseInt(g.getOptarg());
				break;
			case 'x':
				coreRimScoreCutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'y':
				coreSurfScoreCutoff = Double.parseDouble(g.getOptarg());
				break;				
			case 'd':
				homSoftIdCutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'D':
				homHardIdCutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'q':
				maxNumSeqs = Integer.parseInt(g.getOptarg());
				break;
			case 'H':
				homologsSearchMode = HomologsSearchMode.getByName(g.getOptarg());
				break;
			case 'O':
				filterByDomain = true;
				break;
			case 'A':
				nSpherePointsASAcalc = Integer.parseInt(g.getOptarg());
				break;
			case 'I':
				interfSerFile = new File(g.getOptarg());
				break;
			case 'C':
				chainEvContextSerFile = new File(g.getOptarg());
				break;
			case 'p':
				generateInterfacesPdbFiles = true;
				break;
			case 'l':
				generateInterfacesPdbFiles = true;
				generateThumbnails = true;
				break;
			case 'w':
				generateModelSerializedFile = true;
				break;
			case 'L':
				progressLogFile = new File(g.getOptarg());
				break;
			case 'g':
				configFile = new File(g.getOptarg());
				break;
			case 'u':
				debug = true;
				break;
			case 'h':
				System.out.println(help);
				System.exit(0);
				break;
			case '?':
				System.err.println(help);
				System.exit(1);
				break; // getopt() already printed an error
			}
		}
	}
	
	public void parseCommandLine(String[] args) throws EppicException {
		
		String help = 
		PROGRAM_NAME+" ver. "+PROGRAM_VERSION+"\n" +
		"Usage: \n" +
		"   -i <string> :  input PDB code or PDB/mmCIF file\n" +
		"  [-s]         :  calculate evolutionary entropy-based scores (core-rim and \n" +
		"                  core-surface).\n" +
		"                  If not specified, only geometric scoring is done.\n"+
		"  [-a <int>]   :  number of threads for blast, alignment and ASA calculation. \n" +
		"                  Default: "+DEF_NUMTHREADS+"\n"+
		"  [-b <string>]:  basename for output files. Default: as input PDB code or file \n" +
		"                  name\n"+
		"  [-o <dir>]   :  output dir, where output files will be written. Default: current\n" +
		"                  dir \n" +
		"  [-r <int>]   :  specify the number of groups of aminoacids (reduced alphabet) to\n" +
		"                  be used for entropy calculations.\n" +
		"                  Valid values are 2, 4, 6, 8, 10, 15 and 20. Default: "+DEF_ENTROPY_ALPHABET+"\n" +
		"  [-e <float>] :  the BSA/ASA cutoff for core assignment in geometry predictor.\n" +
		"                  Default: "+String.format("%4.2f",DEF_CA_CUTOFF_FOR_GEOM)+"\n" +
		"  [-c <float>] :  the BSA/ASA cutoff for core assignment in core-rim evolutionary \n" +
		"                  predictor. Default: "+String.format("%4.2f",DEF_CA_CUTOFF_FOR_RIMCORE)+"\n" +
		"  [-z <float>] :  the BSA/ASA cutoff for core assignment in core-surface \n" +
		"                  evolutionary predictor. Default: "+String.format("%4.2f",DEF_CA_CUTOFF_FOR_ZSCORE)+"\n" +
		"  [-m <int>]   :  geometry scoring cutoff for number of interface core residues, if \n" +
		"                  below this value the geometry call will be XTAL, if equals or \n" +
		"                  higher the geometry call is BIO. Default "+DEF_MIN_CORE_SIZE_FOR_BIO+"\n" +
		"  [-x <float>] :  core-rim score cutoff for calling BIO/XTAL. If below this score, \n" +
		"                  interface is BIO, if above XTAL. Default: " + String.format("%4.2f",DEF_CORERIM_SCORE_CUTOFF)+"\n"+
		"  [-y <float>] :  core-surface score cutoff to call BIO/XTAL. If below this score, \n" +
		"                  interface is BIO, if above XTAL. Default: " + String.format("%4.2f",DEF_CORESURF_SCORE_CUTOFF)+"\n"+
		"  [-d <float>] :  sequence identity soft cutoff, if enough homologs ("+DEF_MIN_NUM_SEQUENCES+") above this \n" +
		"                  threshold the search for homologs stops, default: "+String.format("%3.1f",DEF_HOM_SOFT_ID_CUTOFF)+"\n"+
		"  [-D <float>] :  sequence identity hard cutoff, if after applying the soft\n" +
		"                  cutoff (see -d), not enough homologs ("+DEF_MIN_NUM_SEQUENCES+") are found\n" +
		"                  then the threshold is lowered in "+String.format("%4.2f",DEF_HOM_ID_STEP)+" steps until this hard\n" +
		"                  cutoff is reached. \n" +
		"                  Default: "+String.format("%3.1f",DEF_HOM_HARD_ID_CUTOFF)+"\n"+
		"  [-q <int>]   :  maximum number of sequences to keep for calculation of conservation \n" +
		"                  scores. Default: "+DEF_MAX_NUM_SEQUENCES+"\n"+
		"  [-H <string>]:  homologs search mode: either \"local\" (only UniProt region covered\n" +
		"                  by PDB structure will be used to search homologs) or \"global\" \n" +
		"                  (full UniProt entry will be used to search homologs).\n" +
		"                  Default "+DEF_HOMOLOGS_SEARCH_MODE.getName() + "\n"+
		"  [-O]         :  restrict homologs search to those within the same domain of \n" +
		"                  life as query\n"+
		"  [-A <int>]   :  number of sphere points for ASA calculation, this parameter \n" +
		"                  controls the accuracy of the ASA calculations, the bigger the\n" +
		"                  more accurate (and slower). Default: "+DEF_NSPHEREPOINTS_ASA_CALC+"\n" +
		"  [-I <file>]  :  binary file containing the interface enumeration output of a \n" +
		"                  previous run of "+PROGRAM_NAME+"\n" +
		"  [-C <file>]  :  binary file containing the evolutionary scores for a particular \n" +
		"                  sequence output of a previous run of "+PROGRAM_NAME+"\n" +
		"  [-p]         :  if specified PDB files for each interface will be generated\n"+
		"  [-l]         :  if specified PyMOL files (thumbnail pngs, pse files) will be generated \n"+ 
		"                  for each interface and for each chain cluster (requires PyMOL).\n"+
		"                  This option will force the -p option\n" +
		"  [-w]         :  if specified a serialized webui.dat file will be produced\n" +
		"  [-L <file>]  :  a file where progress log will be written to. Default: progress\n" +
		"                  log written to std output\n" +
		"  [-g <file>]  :  an "+PROGRAM_NAME+" config file. This will override the existing \n" +
		"                  config file in the user's home directory\n" +
		"  [-u]         :  debug, if specified debug output will be also shown on standard\n" +
		"                  output\n\n";
		
		parseCommandLine(args, PROGRAM_NAME, help);
		checkCommandLineInput();

	}

	public void checkCommandLineInput() throws EppicException {
		
		if (inputStr==null) {
			throw new EppicException(null, "Missing argument -i", true);
		}
		
		if (inFile!=null && !inFile.exists()){
			throw new EppicException(null, "Given file "+inFile+" does not exist!", true);
		}
		
		if (baseName==null) {
			baseName=pdbCode;
			if (inFile!=null) {
				if (inFile.getName().contains(".")) {
					baseName = inFile.getName().substring(0, inFile.getName().lastIndexOf('.'));
				} else {
					baseName = inFile.getName();
				}
			}
		}
		
		if (progressLogFile!=null) {
			try {
				progressLog = new PrintStream(progressLogFile);
			} catch (FileNotFoundException e) {
				throw new EppicException(e, "Specified log file can not be written to: "+e.getMessage(), true);
			}
		} 
		
		if (configFile!=null && !configFile.exists()) {
			throw new EppicException(null, "Specified config file "+configFile+" doesn't exist",true);
		}
		
		if (!AminoAcid.isValidNumGroupsReducedAlphabet(reducedAlphabet)) {
			throw new EppicException(null, "Invalid number of amino acid groups specified ("+reducedAlphabet+")", true);
		}
		
		if (homologsSearchMode==null) {
			// invalid string passed as homologs search mode
			throw new EppicException(null, "Invalid string specified as homologs search mode (-H).", true);
		}
		
		if (homSoftIdCutoff<homHardIdCutoff) {
			homHardIdCutoff = homSoftIdCutoff;
		}


	}
	
	public void checkConfigFileInput() throws EppicException {
		
		if (!isInputAFile()) {
			if (!isUseOnlinePdb()) {
				if (localCifDir==null || ! new File(localCifDir).isDirectory()) {
					throw new EppicException(null,
						"To be able to use PDB codes as input with -i option, a valid LOCAL_CIF_DIR must be set in config file. " +
						"It must contain the PDB mmCIF compressed file repository as in "+DEF_PDB_FTP_CIF_URL+
						". Alternatively you can set USE_ONLINE_PDB to true and the default PDB ftp server will be used (or the one set in PDB_FTP_CIF_URL)",true);
				}
			} 		
		}
		
		if (isDoEvolScoring()) {
			if (blastDbDir==null || ! new File(blastDbDir).isDirectory()) {
				throw new EppicException(null,"BLAST_DB_DIR has not been set to a valid value in config file",true);
			}
			if (blastDb==null) {
				throw new EppicException(null,"BLAST_DB has not been set to a valid value in config file",true);
			} else {
				// .00.xxx or .xxx with xxx one of phr, pin, psd, psi, psq
				File dbFile1 = new File(blastDbDir,blastDb+".00.phr"); 
				File dbFile2 = new File(blastDbDir,blastDb+".phr");
				if (!dbFile1.exists() && !dbFile2.exists()){
					throw new EppicException(null,dbFile1+" or "+dbFile2+" blast index files not present. Please set correct values of BLAST_DB_DIR and BLAST_DB in config file",true);
				}
				
			}
			if (!blastpBin.exists()) {
				throw new EppicException(null,"The BLASTP_BIN path given in config file does not exist: "+blastpBin,true);
			}
			if (!blastclustBin.exists()) {
				throw new EppicException(null,"The BLASTCLUST_BIN path given in config file does not exist: "+blastclustBin,true);
			}
			if (! new File(blastDataDir).isDirectory()) {
				throw new EppicException(null,"BLAST_DATA_DIR must be set to a valid value in config file. Directory "+blastDataDir+ " doesn't exist.",true);
			} else if (! new File(blastDataDir,"BLOSUM62").exists()) {
				throw new EppicException(null, "BLAST_DATA_DIR parameter in config file must be set to a dir containing a blast BLOSUM62 file. No BLOSUM62 file in "+blastDataDir, true);
			}
			
			// alignment programs: we allow one and only one to be set
			if (!tcoffeeBin.exists() && !clustaloBin.exists()) {
				throw new EppicException(null,"Either TCOFFEE_BIN or CLUSTALO_BIN must be set to a valid value in config file.",true);
			} else if (tcoffeeBin.exists() && clustaloBin.exists()){
				throw new EppicException(null,"Both TCOFFEE_BIN and CLUSTALO_BIN are set in config file. Only one of the 2 programs can be set at the same time.",true);
			}
			if (tcoffeeBin.exists()) {
				clustaloBin = null;
			} else {
				tcoffeeBin = null;
			}
		}
		
		if (isGenerateThumbnails()) {
			//TODO check for Pymol
		}
	}
	
	public boolean isInputAFile() {
		return inFile!=null;
	}
	
	public File getInFile() {
		return inFile;
	}
	
	public File getOutputFile(String suffix) {
		return new File(outDir,baseName+suffix);
	}
	
	public String getPdbCode() {
		return pdbCode;
	}
	
	/**
	 * Sets the values of inFile and pdbCode from input string given in -i
	 * - if inputStr matches a PDB code (i.e. regex \d\w\w\w) then inFile is null and input considered to be PDB code
	 * - if inputStr does not match a PDB code then inFile gets initialised to inputStr and pdbCode remains null
	 */
	public void setInput() {

		Matcher m = PDBCODE_PATTERN.matcher(inputStr);
		if (m.matches()) {
			this.inFile = null;
			this.pdbCode = inputStr.toLowerCase();
		} else {
			this.inFile = new File(inputStr);
			this.pdbCode = null;
		}
		
	}
	
	public boolean isDoEvolScoring() {
		return doEvolScoring;
	}
	
	public double getHomSoftIdCutoff() {
		return homSoftIdCutoff;
	}
	
	public void setHomSoftIdCutoff(double homSoftIdCutoff) {
		this.homSoftIdCutoff = homSoftIdCutoff;
	}
	
	public double getHomHardIdCutoff() {
		return homHardIdCutoff;
	}
	
	public void setHomHardCutoff(double homHardIdCutoff) {
		this.homHardIdCutoff = homHardIdCutoff;
	}
	
	public double getHomIdStep() {
		return homIdStep;
	}

	public String getBaseName() {
		return baseName;
	}
	
	public void setBaseName(String baseName) {
		this.baseName = baseName;
	}
	
	public File getOutDir() {
		return outDir;
	}
	
	public void setOutDir(File outDir) {
		this.outDir = outDir;
	}
	
	public int getNumThreads() {
		return numThreads;
	}
	
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}
	
	public int getReducedAlphabet() {
		return reducedAlphabet;
	}
	
	public void setReducedAlphabet(int reducedAlphabet) {
		this.reducedAlphabet = reducedAlphabet;
	}
	
	/**
	 * Returns the maximum theoretical value that entropy can have for an 
	 * alignment column (for entropies defined with log base 2). 
	 * The value is log(number of aminoacid classes)
	 * @return
	 */
	public double getMaxEntropy() {
		return Math.log(reducedAlphabet)/Math.log(2);
	}
	
	public double getCAcutoffForGeom() {
		return caCutoffForGeom;
	}
	
	public double getCAcutoffForRimCore(){
		return caCutoffForRimCore;
	}
	
	public double getCAcutoffForZscore() {
		return caCutoffForZscore;
	}
	
	public double getMinAsaForSurface() {
		return minAsaForSurface;
	}
	
	public int getMinSizeCofactorForAsa() {
		return minSizeCofactorForAsa;
	}
	
	public int getMinCoreSizeForBio() {
		return minCoreSizeForBio;
	}
	
	public double getSelectonEpsilon() {
		return selectonEpsilon;
	}
	
	public void setSelectonEpsilon(double selectonEpsilon) {
		this.selectonEpsilon = selectonEpsilon;
	}
	
	public int getMaxNumSeqs() {
		return maxNumSeqs;
	}
	
	public void setMaxNumSeqs(int maxNumSeqs) {
		this.maxNumSeqs = maxNumSeqs;
	}
	
	public int getnSpherePointsASAcalc() {
		return nSpherePointsASAcalc;
	}

	public void setnSpherePointsASAcalc(int nSpherePointsASAcalc) {
		this.nSpherePointsASAcalc = nSpherePointsASAcalc;
	}
	
	public double getCoreRimScoreCutoff() {
		return coreRimScoreCutoff;
	}

	public void setCoreRimScoreCutoff(double entrCallCutoff) {
		this.coreRimScoreCutoff = entrCallCutoff;
	}
	
	public double getCoreSurfScoreCutoff(){
		return coreSurfScoreCutoff;
	}
	
	public void setCoreSurfScoreCutoff(double coreSurfScoreCutoff) {
		this.coreSurfScoreCutoff = coreSurfScoreCutoff;
	}

	public File getInterfSerFile() {
		return interfSerFile;
	}


	public void setInterfSerFile(File interfSerFile) {
		this.interfSerFile = interfSerFile;
	}


	public File getChainEvContextSerFile() {
		return chainEvContextSerFile;
	}


	public void setChainEvContextSerFile(File chainEvContextSerFile) {
		this.chainEvContextSerFile = chainEvContextSerFile;
	}

	public void setInFile(File inFile) {
		this.inFile = inFile;
	}
	
	public boolean isGenerateInterfacesPdbFiles(){
		return generateInterfacesPdbFiles;
	}
	
	public boolean isGenerateThumbnails() {
		return generateThumbnails;
	}
	
	public boolean isGenerateModelSerializedFile() {
		return generateModelSerializedFile;
	}
	
	public PrintStream getProgressLog() {
		return progressLog;
	}
	
	public File getProgressLogFile() {
		return progressLogFile;
	}
	
	public File getConfigFile() {
		return configFile;
	}
	
	public boolean getDebug() {
		return debug;
	}
	
	public HomologsSearchMode getHomologsSearchMode() {
		return homologsSearchMode;
	}
	
	public void setHomologsSearchMode(HomologsSearchMode homologsSearchMode) {
		this.homologsSearchMode = homologsSearchMode;
	}
	
	public boolean isFilterByDomain() {
		return filterByDomain;
	}
	
	public void setIsFilterByDomain(boolean filterByDomain) {
		this.filterByDomain = filterByDomain;
	}

	public void readConfigFile(File file) throws FileNotFoundException, IOException { 
		Properties p = new Properties();
		p.load(new FileInputStream(file));

		try {
			// variables without defaults
			blastDbDir    	= p.getProperty("BLAST_DB_DIR");
			blastDb        	= p.getProperty("BLAST_DB");
			
			localUniprotDbName = p.getProperty("LOCAL_UNIPROT_DB_NAME");

			localCifDir   	= p.getProperty("LOCAL_CIF_DIR", DEF_LOCAL_CIF_DIR);
			pdbFtpCifUrl 	= p.getProperty("PDB_FTP_URL", DEF_PDB_FTP_CIF_URL);
			useOnlinePdb  	= Boolean.parseBoolean(p.getProperty("USE_ONLINE_PDB", new Boolean(DEF_USE_ONLINE_PDB).toString()));
			
			siftsFile       = p.getProperty("SIFTS_FILE", DEF_SIFTS_FILE);
			useSifts        = Boolean.parseBoolean(p.getProperty("USE_SIFTS", new Boolean(DEF_USE_SIFTS).toString()));
			
			blastclustBin   = new File(p.getProperty("BLASTCLUST_BIN", DEF_BLASTCLUST_BIN.toString()));
			
			blastpBin	    = new File(p.getProperty("BLASTP_BIN", DEF_BLASTP_BIN.toString()));
			
			blastDataDir    = p.getProperty("BLAST_DATA_DIR", DEF_BLAST_DATA_DIR);
			
			// for alignment programs we either read them or set them to null
			tcoffeeBin 		= new File(p.getProperty("TCOFFEE_BIN", DEF_TCOFFEE_BIN.toString()));
			
			clustaloBin		= new File(p.getProperty("CLUSTALO_BIN", DEF_CLUSTALO_BIN.toString()));
			
			pymolExe		= new File(p.getProperty("PYMOL_EXE", DEF_PYMOL_EXE.toString()));
			
			hbplusExe       = new File(p.getProperty("HBPLUS_EXE", DEF_HBPLUS_EXE.toString()));
			
			minAsaForSurface = Double.parseDouble(p.getProperty("MIN_ASA_FOR_SURFACE", new Double(DEF_MIN_ASA_FOR_SURFACE).toString()));
			
			minSizeCofactorForAsa = Integer.parseInt(p.getProperty("MIN_SIZE_COFACTOR_FOR_ASA", new Integer(DEF_MIN_SIZE_COFACTOR_FOR_ASA).toString()));

			queryCoverageCutoff = Double.parseDouble(p.getProperty("QUERY_COVERAGE_CUTOFF", new Double(DEF_QUERY_COVERAGE_CUTOFF).toString()));
			minNumSeqs = Integer.parseInt(p.getProperty("MIN_NUM_SEQUENCES", new Integer(DEF_MIN_NUM_SEQUENCES).toString()));
			
			minQueryCovForIdenticalsRemoval = Double.parseDouble(p.getProperty("MIN_QUERY_COV_FOR_IDENTICALS_REMOVAL", new Double(DEF_MIN_QUERY_COV_FOR_IDENTICALS_REMOVAL).toString()));
			
			homIdStep = Double.parseDouble(p.getProperty("HOM_ID_STEP",new Double(DEF_HOM_ID_STEP).toString()));
			
			pdb2uniprotIdThreshold = Double.parseDouble(p.getProperty("PDB2UNIPROT_ID_THRESHOLD", new Double(DEF_PDB2UNIPROT_ID_THRESHOLD).toString()));
			pdb2uniprotQcovThreshold = Double.parseDouble(p.getProperty("PDB2UNIPROT_QCOV_THRESHOLD", new Double(DEF_PDB2UNIPROT_QCOV_THRESHOLD).toString()));
			
			allowChimeras = Boolean.parseBoolean(p.getProperty("ALLOW_CHIMERAS", new Boolean(DEF_ALLOW_CHIMERAS).toString()));
			
			usePdbCodeFromFile = Boolean.parseBoolean(p.getProperty("USE_PDB_CODE_FROM_FILE", new Boolean(DEF_USE_PDB_CODE_FROM_FILE).toString()));
			
			blastCacheDir    = p.getProperty("BLAST_CACHE_DIR", DEF_BLAST_CACHE_DIR);
			alnCacheDir		 = p.getProperty("ALN_CACHE_DIR", DEF_ALN_CACHE_DIR);
			
			useUniparc       = Boolean.parseBoolean(p.getProperty("USE_UNIPARC",new Boolean(DEF_USE_UNIPARC).toString()));
			
			usePdbResSer	 = Boolean.parseBoolean(p.getProperty("USE_PDB_RES_SER",new Boolean(DEF_USE_PDB_RES_SER).toString()));
			
			coreSurfaceScoreStrategy = Integer.parseInt(p.getProperty("CORE_SURFACE_SCORE_STRATEGY", new Integer(DEF_CORE_SURFACE_SCORE_STRATEGY).toString()));
			
		} catch (NumberFormatException e) {
			System.err.println("A numerical value in the config file was incorrectly specified: "+e.getMessage()+".\n" +
					"Please check the config file.");
			System.exit(1);
		}
	}

	public String getLocalCifDir() {
		return localCifDir;
	}

	public String getPdbFtpCifUrl() {
		return pdbFtpCifUrl;
	}

	public boolean isUseOnlinePdb() {
		return useOnlinePdb;
	}

	public String getSiftsFile() {
		return siftsFile;
	}
	
	public boolean isUseSifts() {
		return useSifts;
	}

	public File getBlastpBin() {
		return blastpBin;
	}
	
	public File getBlastclustBin() {
		return blastclustBin;
	}

	public String getBlastDataDir() {
		return blastDataDir;
	}
	
	public File getTcoffeeBin() {
		return tcoffeeBin;
	}
	
	public File getClustaloBin() {
		return clustaloBin;
	}

	public File getSelectonBin() {
		return selectonBin;
	}

	public File getPymolExe() {
		return pymolExe;
	}
	
	public File getHbplusExe() {
		return hbplusExe;
	}

	public double getQueryCoverageCutoff() {
		return queryCoverageCutoff;
	}

	public int getMinNumSeqs() {
		return minNumSeqs;
	}

	public double getMinQueryCovForIdenticalsRemoval() {
		return minQueryCovForIdenticalsRemoval;
	}
	
	public double getPdb2uniprotIdThreshold() {
		return pdb2uniprotIdThreshold;
	}

	public double getPdb2uniprotQcovThreshold() {
		return pdb2uniprotQcovThreshold;
	}
	
	public boolean isAllowChimeras() {
		return allowChimeras;
	}
	
	public boolean isUsePdbCodeFromFile() {
		return usePdbCodeFromFile;
	}

	public String getBlastCacheDir() {
		return blastCacheDir;
	}
	
	public String getAlnCacheDir() {
		return alnCacheDir;
	}

	public String getBlastDbDir() {
		return blastDbDir;
	}

	public String getBlastDb() {
		return blastDb;
	}
	
	public String getLocalUniprotDbName() {
		return localUniprotDbName;
	}
	
	public boolean isUseUniparc() {
		return useUniparc;
	}
	
	public boolean isUsePdbResSer() {
		return usePdbResSer;
	}
	
	public int getCoreSurfaceScoreStrategy() {
		return coreSurfaceScoreStrategy;
	}

}
