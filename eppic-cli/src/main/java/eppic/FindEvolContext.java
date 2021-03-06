package eppic;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eppic.commons.sequence.AAAlphabet;
import eppic.commons.sequence.Sequence;
import gnu.getopt.Getopt;


/**
 * An executable class that given a FASTA file with sequences
 * will produce an alignment by using the same procedure as EPPIC
 * 
 * @author duarte_j
 *
 */
public class FindEvolContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(FindEvolContext.class);
	
	private static File inputFile;
	private static ChainEvolContextList cecs;
	private static EppicParams params;
	
	public static void main(String[] args) throws Exception {

		params = new EppicParams();
		loadConfigFile();		
		parseCommandLine(args, FindEvolContext.class.getName());		

		// log4j2 setup for logging to our basename.log file name
		System.setProperty("logFilename", new File(params.getOutDir(),params.getBaseName()+".log").toString());
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();
		// TODO for some reason (bug?) log4j2 2.1 produces a file named with the log4j2.xml $pointer, the only fix I know for now is to remove it manually
		new File("${sys:logFilename}").deleteOnExit();


		
		
		List<Sequence> sequences = Sequence.readSeqs(inputFile, null);
		
		try {
			cecs = new ChainEvolContextList(sequences,params);
		} catch (SQLException e) {
			throw new EppicException(e,"Could not connect to local uniprot database server: "+e.getMessage(),true);
		}

		doFindEvolContext();
		
	}

	
	public static void doFindEvolContext() throws EppicException {
		
		cecs.retrieveQueryData(params);
		
		// b) getting the homologs and sequence data and filtering it
		cecs.retrieveHomologs(params);

		// c) align
		cecs.align(params);
		
		// d) computing entropies
		cecs.computeEntropies(params);
		
		
		//try {
		//	Goodies.serialize(params.getOutputFile(".chainevolcontext.dat"),cecs);
		//} catch (IOException e) {
		//	throw new CRKException(e,"Couldn't write serialized ChainEvolContextList object to file: "+e.getMessage(),false);
		//}
		
		
	}
	
	private static void loadConfigFile() {
		// loading settings from config file
		File userConfigFile = new File(System.getProperty("user.home"),EppicParams.CONFIG_FILE_NAME);  
		try {
			if (userConfigFile.exists()) {
				LOGGER.info("Loading user configuration file " + userConfigFile);
				params.readConfigFile(userConfigFile);
				params.checkConfigFileInput();
			} else {
				LOGGER.error("No config file could be read at "+userConfigFile+
						". Please set one in order to be able to read blast directories and blast/t_coffee/clustalo binaries.");
				System.exit(1);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading from config file: " + e.getMessage());
			System.exit(1);
		} catch (EppicException e) {
			LOGGER.error(e.getMessage());
			System.exit(1);
		}

	}
	
	private static void parseCommandLine(String[] args, String programName) throws EppicException {
		
		String help = "Usage: "+programName+" \n" +
				" -i : input FASTA file\n" +
				" -a : num threads"+
				" -b : base name of output files\n"+
				" -o : out dir \n" +
				" -d : homologs soft identity cutoff\n" +
				" -D : homologs hard identity cutoff\n" +
				" -q : max num homologs\n" +
				" -H : homologs search mode: either \"local\" (only Uniprot region covered\n" +
				"      by PDB structure will be used to search homologs) or \"global\" (full\n" +
				"      Uniprot entry will be used to search homologs)\n" +
				"      Default "+EppicParams.DEF_HOMOLOGS_SEARCH_MODE.getName() + "\n"+
				" -O : restrict homologs search to those within the same domain of life as \n" +
				"      query\n" +
				" -h : print command line parameters help\n\n"
				;

		Getopt g = new Getopt(programName, args, "i:a:b:o:r:d:D:q:H:G:Oh?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'i':
				inputFile = new File(g.getOptarg());
				break;
			case 'a':
				params.setNumThreads(Integer.parseInt(g.getOptarg()));
				break;
			case 'b':
				params.setBaseName(g.getOptarg());
				break;				
			case 'o':
				params.setOutDir(new File(g.getOptarg()));
				break;
			case 'd':
				params.setHomSoftIdCutoff(Double.parseDouble(g.getOptarg()));
				break;
			case 'D':
				params.setHomHardCutoff(Double.parseDouble(g.getOptarg()));
				break;
			case 'q':
				params.setMaxNumSeqs(Integer.parseInt(g.getOptarg()));
				break;
			case 'H':
				params.setHomologsSearchMode(HomologsSearchMode.getByName(g.getOptarg()));
				break;
			case 'O':
				params.setIsFilterByDomain(true);
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
		
		if (inputFile==null) {
			throw new EppicException(null, "A FASTA input file must be provided with -i",true);
		}
		
		if (!inputFile.exists()){
			throw new EppicException(null, "Given file "+inputFile+" does not exist!", true);
		}

		if (!AAAlphabet.isValidAlphabetIdentifier((params.getAlphabet().getNumLetters()))) {
			throw new EppicException(null, "Invalid number of amino acid groups specified ("+params.getAlphabet().getNumLetters()+")", true);
		}

		if (params.getBaseName()==null) {
			params.setBaseName(inputFile.getName());
		}
		
		if (params.getHomSoftIdCutoff()<params.getHomHardIdCutoff()) {
			params.setHomHardCutoff(params.getHomSoftIdCutoff());
		}
		


	}
	

}
