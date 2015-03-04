package eppic.db.tools;

import eppic.model.ChainClusterDB;
import eppic.model.HomologDB;
import eppic.model.InterfaceClusterDB;
import eppic.model.InterfaceDB;
import eppic.model.PdbInfoDB;
import eppic.model.ResidueDB;
import gnu.getopt.Getopt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import owl.core.sequence.alignment.MultipleSequenceAlignment;
import owl.core.structure.AAAlphabet;

public class AlphabetOptimisation {
	
	//public static final String SAVE_FILE_PATH = "/home/somody_j/Thesis/savefile";
	public static final String SAVE_FILE_PATH = "/gpfs/home/somody_j/veryrandom/savefile";
	public static final double EVOL_CUTOFF = 0.80;
	public static final double GEOM_CUTOFF = 0.90;
	public static final double CALL_THRESHOLD = -0.90;
	public static final double MIN_ASA_FOR_SURFACE = 5;
	public static final String[] AMINO_ACIDS = {"A", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "Y"};
	public static final int CARRYING_CAPACITY = 20; // >=4
	public static final int INITIAL_POPULATION_SIZE = 10; // >=4
	public static final int ROUNDS_OF_EVOLUTION = 10000;
	public static final double SCORE_INFINITY_VALUE = 1000.00;
	public static final double NUM_SURF_RES_TOLERANCE = 2.00;
	public static final int NUMBER_OF_SURFACE_SAMPLES = 10000;
	public static final int MINIMUM_NUM_HOMOLOGUES = 30;
	public static final double MINIMUM_HOMOLOGY = 0.60;
	public static final int NUM_TRIALS = 1000; // for optimise2
	
	public static ArrayList<Boolean> truths; // LIST OF BOOLEANS (TRUE IFF BIO), ONE PER DATAPOINT
	public static ArrayList<ArrayList<MultipleSequenceAlignment>> MSAs; // LIST OF PAIRS OF MSAS, ONE PAIR PER DATAPOINT, PAIR = (LEFT, RIGHT)
	public static ArrayList<ArrayList<ArrayList<Integer>>> regions; // LIST OF PAIRS OF LISTS OF REGIO_INTS, ONE PER DATAPOINT, PAIR = (LEFT LIST OF REGIONS, RIGHT)
	public static ArrayList<ArrayList<HashMap<Integer, Integer>>> mappings; // LIST OF PAIRS OF MAPPINGS FROM RES_ID TO MSA_COL, ONE PER DATAPOINT, PAIR = (LEFT MAPPING, RIGHT)
	public static ArrayList<ArrayList<ArrayList<Integer>>> resids; // LIST OF PAIRS OF LISTS OF RESIDUE NUMBERS, ONE PER DATAPOINT, PAIR = (LEFT CHAIN RESID#S, RIGHT)
	public static ArrayList<ArrayList<Boolean>> stringencies; // LIST OF PAIRS OF BOOLEANS, ONE PER DATAPOINT, PAIR = (LEFT SUFF_STRING?, RIGHT SUFF_STRING?)
	
	public static String dbName;
	public static File listFile;
	public static File specifiedAlphabets;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		File file = new File(SAVE_FILE_PATH);
		if (file.exists() && file.isFile()) {
			parseCommandLine(args);
			System.out.println("Data has been saved, loading from file...");
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			ArrayList<Object> fileData = (ArrayList<Object>) ois.readObject();
			truths = (ArrayList<Boolean>) fileData.get(0);
			MSAs = (ArrayList<ArrayList<MultipleSequenceAlignment>>) fileData.get(1);
			regions = (ArrayList<ArrayList<ArrayList<Integer>>>) fileData.get(2);
			mappings = (ArrayList<ArrayList<HashMap<Integer, Integer>>>) fileData.get(3);
			resids = (ArrayList<ArrayList<ArrayList<Integer>>>) fileData.get(4);
			stringencies = (ArrayList<ArrayList<Boolean>>) fileData.get(5);
			ois.close();
			System.out.println("Done loading from file...");
		} else {
			System.out.println("No saved data found, computing from scratch...");
			parseCommandLine(args);
			collectData();
		}
		
		testSpecifiedAlphabets();
		
		// RUN ALPHABET OPTIMISATION BY EVOLUTIONARY ALGORITHM
//		optimiseAlphabet();
		
		// RUN ALPHABET OPTIMISATION BY BRUTE FORCE
//		optimiseAlphabet2();
	}
	
	private static void parseCommandLine(String[] args) throws Exception {
		String help = 
				"Usage: AlphabetOptimisation\n" +
						" -D : the database name to use\n" +
						" -i : a list file of PDB codes\n" +
						"The database access must be set in file " + DBHandler.CONFIG_FILE_NAME + " in home dir\n";

		String localDbName = null;
		String listFileName = null;
		String alphabetsFileName = null;
		Getopt g = new Getopt("AlphabetOptimisation", args, "D:i:A:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'D':
				localDbName = g.getOptarg();
				break;
			case 'i':
				listFileName = g.getOptarg();
				break;
			case 'A':
				alphabetsFileName = g.getOptarg();
				break;
			case 'h':
				System.out.println(help);
				System.exit(0);
				break;
			case '?':
				System.err.println(help);
				System.exit(1);
				break;
			}
		}
		
		dbName = localDbName;
		if (listFileName == null) {
			System.err.println("A file with a list of PDB codes have to be provided with -i");
			System.exit(1);
		}
		
		listFile = new File(listFileName);
		if (!listFile.exists()) {
			System.err.println("Input string " + listFileName + " does not seem to be a file");
			System.exit(1);
		}
		
		if (alphabetsFileName != null) {
			specifiedAlphabets = new File(alphabetsFileName);
		}
	}
	
	private static void collectData() throws Exception {
		// START DATABASE HANDLER
		DBHandler dbh = new DBHandler(dbName);
		
		// EXTRACT INFORMATION FROM DATASET
		List<ArrayList<String>> listList = readList(listFile);
		List<String> pdbCodes = listList.get(0);
		List<String> firstChains = listList.get(1);
		List<String> secondChains = listList.get(2);
		List<String> interfaceNumbers = listList.get(3);
		List<String> trueCalls = listList.get(4);
		List<PdbInfoDB> pdbInfos = dbh.deserializePdbList(pdbCodes);
		assert pdbCodes.size() == pdbInfos.size();
		
		// INITIALISE STATIC CLASS VARIABLES
		truths = new ArrayList<Boolean>();
		MSAs = new ArrayList<ArrayList<MultipleSequenceAlignment>>();
		regions = new ArrayList<ArrayList<ArrayList<Integer>>>();
		mappings = new ArrayList<ArrayList<HashMap<Integer, Integer>>>();
		resids = new ArrayList<ArrayList<ArrayList<Integer>>>();
		stringencies = new ArrayList<ArrayList<Boolean>>();
		
		for (int i = 0; i < pdbInfos.size(); i++) {
//		for (int i = 2850; i < 2860; i++) {
			System.out.println(i);
			PdbInfoDB pdbInfo = pdbInfos.get(i);
			String firstChain = firstChains.get(i);
			String secondChain = secondChains.get(i);
			int interfaceNumber = Integer.parseInt(interfaceNumbers.get(i));
//			System.out.println(pdbCodes.get(i) + " " + pdbInfo.getPdbCode() + " " + firstChain + " " + secondChain + " " + interfaceNumber);
			
			// ADD TRUE CALL TO LIST OF TRUE CALLS
			truths.add(trueCalls.get(i).equals("bio"));
			
			// GET LEFT AND RIGHT CHAINCLUSTERS FOR EACH PDBID
			List<ChainClusterDB> chainClusters = pdbInfo.getChainClusters();
			ChainClusterDB firstChainCluster = getChainClusterFromChainClustersByChain(chainClusters, firstChain);
			ChainClusterDB secondChainCluster = getChainClusterFromChainClustersByChain(chainClusters, secondChain);
			
			if (!firstChainCluster.isHasUniProtRef() || !secondChainCluster.isHasUniProtRef()) {
				System.out.println(i + "\t" + pdbInfo.getPdbCode() + "\t" + "HAS NO HOMOLOGUES");
				continue;
			}
			
			// CHECK AND LOG REQUIREMENTS FOR HOMOLOGUE STRINGENCY
			ArrayList<Boolean> stringencyPair = new ArrayList<Boolean>();
			if (firstChainCluster.getSeqIdCutoff() < MINIMUM_HOMOLOGY || firstChainCluster.getNumHomologs() < MINIMUM_NUM_HOMOLOGUES) {
				stringencyPair.add(false);
			} else {
				stringencyPair.add(true);
			}
			if (secondChainCluster.getSeqIdCutoff() < MINIMUM_HOMOLOGY || secondChainCluster.getNumHomologs() < MINIMUM_NUM_HOMOLOGUES) {
				stringencyPair.add(false);
			} else {
				stringencyPair.add(true);
			}
			stringencies.add(stringencyPair);
			
			// GET LEFT AND RIGHT HOMOLOGUES
			List<HomologDB> firstHomologs = firstChainCluster.getHomologs();
			List<HomologDB> secondHomologs = secondChainCluster.getHomologs();
			
			// CREATE ARRAYS FOR CREATING LEFT AND RIGHT MULTIPLESEQUENCEALIGNMENTS
			String[] firstHomologTags = new String[firstHomologs.size() + 1];
			String[] firstHomologSeqs = new String[firstHomologs.size() + 1];
			String[] secondHomologTags = new String[secondHomologs.size() + 1];
			String[] secondHomologSeqs = new String[secondHomologs.size() + 1];
			
			// CREATE TAGS AND SEQUENCES FOR LEFT AND RIGHT QUERIES AND ADD TO ARRAY
			firstHomologTags[0] = firstChainCluster.getRefUniProtId() + "_" + firstChainCluster.getRefUniProtStart() + "-" + firstChainCluster.getRefUniProtEnd();
			firstHomologSeqs[0] = firstChainCluster.getMsaAlignedSeq();
			secondHomologTags[0] = secondChainCluster.getRefUniProtId() + "_" + secondChainCluster.getRefUniProtStart() + "-" + secondChainCluster.getRefUniProtEnd();
			secondHomologSeqs[0] = secondChainCluster.getMsaAlignedSeq();
			
			// ADD LEFT AND RIGHT HOMOLOGUE TAGS AND SEQUENCES TO ARRAY
			for (int j = 0; j < firstHomologs.size(); j++) {
				HomologDB homolog = firstHomologs.get(j);
				firstHomologTags[j + 1] = homolog.getUniProtId() + "_" + homolog.getSubjectStart() + "-" + homolog.getSubjectEnd();
				firstHomologSeqs[j + 1] = homolog.getAlignedSeq();
			}
			for (int j = 0; j < secondHomologs.size(); j++) {
				HomologDB homolog = secondHomologs.get(j);
				secondHomologTags[j + 1] = homolog.getUniProtId() + "_" + homolog.getSubjectStart() + "-" + homolog.getSubjectEnd();
				secondHomologSeqs[j + 1] = homolog.getAlignedSeq();
			}
			
			// CREATE PAIR (LEFT, RIGHT) OF MULTIPLESEQUENCEALIGNMENTS
			ArrayList<MultipleSequenceAlignment> MSAPair = new ArrayList<MultipleSequenceAlignment>();
			MSAPair.add(new MultipleSequenceAlignment(firstHomologTags, firstHomologSeqs));
			MSAPair.add(new MultipleSequenceAlignment(secondHomologTags, secondHomologSeqs));
			
			// ADD MULTIPLESEQUENCEALIGNMENT PAIR TO LIST OF MULTIPLESEQUENCEALIGNMENTS
			MSAs.add(MSAPair);
			
			// GET LEFT AND RIGHT LISTS OF REGIONS OF RESIDUES
			List<InterfaceClusterDB> interfaceClusters = pdbInfo.getInterfaceClusters();
			InterfaceDB theInterface = getInterfaceFromInterfaceClustersByInterfaceId(interfaceClusters, interfaceNumber);
			List<ResidueDB> residues = theInterface.getResidues();
			ArrayList<Integer> firstRegions = new ArrayList<Integer>();
			ArrayList<Integer> secondRegions = new ArrayList<Integer>();
			
			// CREATE LEFT AND RIGHT MAPPINGS FROM INTERFACE RESIDUE NUMBERS TO CHAINCLUSTER MSA COLUMN NUMBERS
			ArrayList<Integer> firstResids = new ArrayList<Integer>();
			ArrayList<Integer> secondResids = new ArrayList<Integer>();
			HashMap<Integer, Integer> firstMapping = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> secondMapping = new HashMap<Integer, Integer>();
			String firstPDB = firstChainCluster.getPdbAlignedSeq();
			String firstREF = firstChainCluster.getRefAlignedSeq();
			String secondPDB = secondChainCluster.getPdbAlignedSeq();
			String secondREF = secondChainCluster.getRefAlignedSeq();
			MultipleSequenceAlignment firstPairwise = new MultipleSequenceAlignment(new String[] {"firstPDB", "firstREF"}, new String[] {firstPDB, firstREF});
			MultipleSequenceAlignment secondPairwise = new MultipleSequenceAlignment(new String[] {"secondPDB", "secondREF"}, new String[] {secondPDB, secondREF});
			for (ResidueDB aResidue : residues) {
				if (aResidue.getSide() == 1) {
					int mapFrom = aResidue.getResidueNumber();
					if (mapFrom >= firstChainCluster.getPdbStart() && mapFrom <= firstChainCluster.getPdbEnd()) {
						int correspondingRefIndex = firstPairwise.al2seq("firstREF", firstPairwise.seq2al("firstPDB", mapFrom)) - 1;
						if (correspondingRefIndex > 0) {
							int withOffsets = correspondingRefIndex - firstChainCluster.getRefUniProtStart() + 1;
							int mapTo = MSAPair.get(0).seq2al(firstHomologTags[0], withOffsets + 1);
							firstRegions.add(getRegion(aResidue.getAsa(), aResidue.getBsa()));
							firstResids.add(aResidue.getResidueNumber());
							firstMapping.put(mapFrom, mapTo);
						}
					}
				} else if (aResidue.getSide() == 2) {
					int mapFrom = aResidue.getResidueNumber();
					if (mapFrom >= secondChainCluster.getPdbStart() && mapFrom <= secondChainCluster.getPdbEnd()) {
						int correspondingRefIndex = secondPairwise.al2seq("secondREF", secondPairwise.seq2al("secondPDB", mapFrom)) - 1;
						if (correspondingRefIndex > 0) {
							int withOffsets = correspondingRefIndex - secondChainCluster.getRefUniProtStart() + 1;
							int mapTo = MSAPair.get(1).seq2al(secondHomologTags[0], withOffsets + 1);
							secondRegions.add(getRegion(aResidue.getAsa(), aResidue.getBsa()));
							secondResids.add(aResidue.getResidueNumber());
							secondMapping.put(mapFrom, mapTo);
						}
					}
				}
			}
			
			// CREATE PAIR (LEFT, RIGHT) OF MAPPINGS
			ArrayList<ArrayList<Integer>> regionPair = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> residPair = new ArrayList<ArrayList<Integer>>();
			ArrayList<HashMap<Integer, Integer>> mappingPair = new ArrayList<HashMap<Integer, Integer>>();
			regionPair.add(firstRegions);
			residPair.add(firstResids);
			mappingPair.add(firstMapping);
			regionPair.add(secondRegions);
			residPair.add(secondResids);
			mappingPair.add(secondMapping);
			
			// ADD PAIR OF LISTS OF REGIONS TO LIST OF PAIRS OF LISTS OF REGIONS
			regions.add(regionPair);
			resids.add(residPair);
			mappings.add(mappingPair);
		}
		System.out.println("Done computing from scratch...saving data to file");
		File file = new File(SAVE_FILE_PATH);
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		ArrayList<Object> toWrite = new ArrayList<Object>();
		toWrite.add(truths);
		toWrite.add(MSAs);
		toWrite.add(regions);
		toWrite.add(mappings);
		toWrite.add(resids);
		toWrite.add(stringencies);
		oos.writeObject(toWrite);
		oos.close();
		System.out.println("Done saving to file...");
	}
	
	private static ArrayList<ArrayList<String>> readList(File file) throws IOException {
		ArrayList<String> pdbs = new ArrayList<String>();
		ArrayList<String> ones = new ArrayList<String>();
		ArrayList<String> twos = new ArrayList<String>();
		ArrayList<String> ints = new ArrayList<String>();
		ArrayList<String> trueCalls = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) continue;
			if (line.trim().isEmpty()) continue;
			String[] lineArray = line.trim().split("\\s+");
			if (lineArray[1].length() == 4) {
				pdbs.add(lineArray[1]);
				ones.add(lineArray[2]);
				twos.add(lineArray[3]);
				ints.add(lineArray[4]);
				trueCalls.add(lineArray[5]);
			}
		}
		br.close();

		ArrayList<ArrayList<String>> toReturn = new ArrayList<ArrayList<String>>();
		toReturn.add(pdbs);
		toReturn.add(ones);
		toReturn.add(twos);
		toReturn.add(ints);
		toReturn.add(trueCalls);
		assert pdbs.size() == ones.size();
		assert ones.size() == twos.size();
		assert twos.size() == ints.size();
		assert ints.size() == trueCalls.size();
		return toReturn;
	}
	
	private static ChainClusterDB getChainClusterFromChainClustersByChain(List<ChainClusterDB> clusters, String chain) {
		ChainClusterDB toReturn = null;
		for (ChainClusterDB cluster : clusters) {
			for (String member : cluster.getMemberChains().split(",")) {
				if (member.equals(chain)) {
					toReturn = cluster;
				}
			}
		}
		return toReturn;
	}
	
	private static InterfaceDB getInterfaceFromInterfaceClustersByInterfaceId(List<InterfaceClusterDB> clusters, int interfaceId) {
		InterfaceDB toReturn = null;
		for (InterfaceClusterDB cluster : clusters) {
			for (InterfaceDB anInterface : cluster.getInterfaces()) {
				if (anInterface.getInterfaceId() == interfaceId) {
					toReturn = anInterface;
				}
			}
		}
		return toReturn;
	}
	
	private static int getRegion(double asa, double bsa) {
		int assignment = ResidueDB.OTHER;
		double ratio = bsa / asa;
		if (asa > MIN_ASA_FOR_SURFACE && bsa > 0) {
			if (ratio < EVOL_CUTOFF) {
				assignment = ResidueDB.RIM_EVOLUTIONARY;
			} else if (ratio < GEOM_CUTOFF) {
				assignment = ResidueDB.CORE_EVOLUTIONARY; 
			} else {
				assignment = ResidueDB.CORE_GEOMETRY;
			}
		} else if (asa > MIN_ASA_FOR_SURFACE) {
			assignment = ResidueDB.SURFACE;
		}		
		return assignment;
	}
	
	private static ArrayList<ArrayList<Double>> getEntropies(int whichDatapoint, int whichSide, AAAlphabet whichAlphabet) {
		ArrayList<Double> coreEntropies = new ArrayList<Double>(); // empty list for core entropies
		ArrayList<Double> surfaceEntropies = new ArrayList<Double>(); // empty list for surface entropies
		ArrayList<Integer> thisRegions = regions.get(whichDatapoint).get(whichSide); // get appropriate list of region ints
		ArrayList<Integer> thisResidues = resids.get(whichDatapoint).get(whichSide); // get appropriate list of residue nums
		HashMap<Integer, Integer> thisMapping = mappings.get(whichDatapoint).get(whichSide); // get appropriate mapping from residnum to msacol
		MultipleSequenceAlignment thisMSA = MSAs.get(whichDatapoint).get(whichSide); // get appropriate msa
		for (int i = 0; i < thisResidues.size(); i++) { // loop through indices of regions/residues
			int thisRegion = thisRegions.get(i); // get the region at this index
			int mapFrom = thisResidues.get(i); // get the residue number at this index
			int mapped = thisMapping.get(mapFrom); // use mapping to get column number from residue number
			if (thisRegion == ResidueDB.CORE_EVOLUTIONARY || thisRegion == ResidueDB.CORE_GEOMETRY) {
				coreEntropies.add(thisMSA.getColumnEntropy(mapped, whichAlphabet)); // add column entropy to core list iff core
			}
			if (thisRegion != ResidueDB.OTHER) { // not other -> one of surface/rimevol/coreevol/coregeom = the real surface
				surfaceEntropies.add(thisMSA.getColumnEntropy(mapped, whichAlphabet)); // add column entropy to surface list iff surface
			}
		}
		ArrayList<ArrayList<Double>> entropiesPair = new ArrayList<ArrayList<Double>>();
		entropiesPair.add(coreEntropies);
		entropiesPair.add(surfaceEntropies);
		return entropiesPair;
	}
	
	private static void optimiseAlphabet() {
		Set<AAAlphabet> alphabetPool = initialisePool();
//		for (AAAlphabet alphabet : alphabetPool) {
//			System.out.println(alphabet); // + "\t" + testFitness(alphabet));
//		}
		for (int i = 1; i <= ROUNDS_OF_EVOLUTION; i++) {
			alphabetPool = evolve(alphabetPool);
			System.out.println("Just finished round " + i + " of evolution...\n");
//			System.out.println("\nAfter " + i + " rounds of evolution...");
//			for (AAAlphabet alphabet : alphabetPool) {
//				System.out.println(alphabet); // + "\t" + testFitness(alphabet));
//			}
		}
	}
	
	private static void optimiseAlphabet2() {
		HashSet<String> tried = new HashSet<String>();
		Random rand = new Random();
		System.out.println("Testing preset alphabets...");
		System.out.println("Wang2" + "\t" + AAAlphabet.WANG_2 + "\t" + testFitness(new AAAlphabet(AAAlphabet.WANG_2)));
		System.out.println("Murphy2" + "\t" + AAAlphabet.MURPHY_2 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MURPHY_2)));
		System.out.println("Murphy4" + "\t" + AAAlphabet.MURPHY_4 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MURPHY_4)));
		System.out.println("Mirny6" + "\t" + AAAlphabet.MIRNY_6 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MIRNY_6)));
		System.out.println("Murphy8" + "\t" + AAAlphabet.MURPHY_8 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MURPHY_8)));
		System.out.println("Murphy10" + "\t" + AAAlphabet.MURPHY_10 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MURPHY_10)));
		System.out.println("Murphy15" + "\t" + AAAlphabet.MURPHY_15 + "\t" + testFitness(new AAAlphabet(AAAlphabet.MURPHY_15)));
		System.out.println("Standard20" + "\t" + AAAlphabet.STANDARD_20 + "\t" + testFitness(new AAAlphabet(AAAlphabet.STANDARD_20)));
		
		System.out.println("Testing " + NUM_TRIALS + " random alphabets...");
		for (int trial = 0; trial < NUM_TRIALS; trial++) {
			String alphabet = "";
			ArrayList<String> left = new ArrayList<String>(Arrays.asList(AMINO_ACIDS));
			Collections.shuffle(left);
			int groups = rand.nextInt(19) + 2;
			int[] dist = new int[groups];
			int currentSum = 0;
			for (int i = 0; i < dist.length; i++) {
				dist[i] = rand.nextInt(20) + 1;
				currentSum += dist[i];
			}
			while (currentSum != 20) {
				int somegroup = rand.nextInt(dist.length);
				if (currentSum > 20) {
					dist[somegroup]--;
					currentSum--;
				} else if (currentSum < 20) {
					dist[somegroup]++;
					currentSum++;
				}
				for (int i = 0; i < dist.length; i++) {
					if (dist[i] < 1) {
						dist[i] += 2;
						currentSum += 2;
					} else if (dist[i] > 20) {
						dist[i] -= 2;
						currentSum -= 2;
					}
				}
			}
			for (int i = 0; i < dist.length; i++) {
				for (int j = 0; j < dist[i]; j++) {
					alphabet += left.get(0);
					left.remove(0);
				}
				alphabet += ":";
			}
//			System.out.println(alphabet);
			String finalAlphabet = simplifyAlphabetString(alphabet.substring(0, alphabet.length() - 1));
			if (!tried.contains(finalAlphabet)) {
				System.out.println(finalAlphabet + "\t\t" + testFitness(new AAAlphabet(finalAlphabet)));
			} else {
				System.out.println(finalAlphabet + "\t\t" + "ALREADY TESTED!");
			}
		}
	}
	
	private static void optimiseAlphabet3() { // old version
		HashSet<String> tried = new HashSet<String>();
		Random rand = new Random();
		for (int trial = 0; trial < 5; trial++) {
			String alphabet = "";
			ArrayList<String> left = new ArrayList<String>(Arrays.asList(AMINO_ACIDS));
			while (left.size() > 0) {
				if (left.size() == 1) {
					alphabet += left.get(0);
					alphabet += ":";
					break;
				}
				int number = rand.nextInt(left.size() - 1) + 1;
				String thisGroup = "";
				for (int i = 0; i < number; i++) {
					int anIndex = rand.nextInt(left.size());
					thisGroup += left.get(anIndex);
					left.remove(anIndex);
				}
				alphabet += thisGroup;
				alphabet += ":";
			}
			String finalAlphabet = simplifyAlphabetString(alphabet.substring(0, alphabet.length() - 1));
			if (!tried.contains(finalAlphabet)) {
				System.out.println(finalAlphabet + "\t\t" + testFitness(new AAAlphabet(finalAlphabet)));
			} else {
				System.out.println(finalAlphabet + "\t\t" + "ALREADY TESTED!");
			}
		}
	}
	
	private static void testSpecifiedAlphabets() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(specifiedAlphabets));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) continue;
			if (line.trim().isEmpty()) continue;
			AAAlphabet specifiedAlphabet = new AAAlphabet(line.trim());
			System.out.println("RESULT:\t" + line.trim() + "\t" + testFitness(specifiedAlphabet));
		}
		br.close();
	}
	
	
	private static String simplifyAlphabetString(String input) {
		String[] inputArray = input.split(":");
		int sum = 0;
		for (int i = 0; i < inputArray.length; i++) {
			sum += inputArray[i].length();
		}
		assert sum == 20;
		for (int i = 0; i < inputArray.length; i++) {
			char[] chars = inputArray[i].toCharArray();
			Arrays.sort(chars);
			String newString = "";
			for (int j = 0; j < chars.length; j++) {
				newString += chars[j];
			}
			inputArray[i] = newString;
		}
		Arrays.sort(inputArray);
		String output = "";
		for (int i = 0; i < inputArray.length; i++) {
			if (inputArray[i].length() > 0) {
				output += inputArray[i];
				output += ":";
			}
		}
		return output.substring(0, output.length() - 1);
	}
	
	private static Set<AAAlphabet> initialisePool() {
		Set<AAAlphabet> pool = new HashSet<AAAlphabet>();
		for (int i = 0; i < INITIAL_POPULATION_SIZE; i++) {
			pool.add(new AAAlphabet(AAAlphabet.STANDARD_20));
		}
		return pool;
	}
	
	
	private static double testFitness(AAAlphabet whichAlphabet) { // balanced accuracy
		double truePositives = 0.0;
		double falsePositives = 0.0;
		double trueNegatives = 0.0;
		double falseNegatives = 0.0;
		for (int i = 0; i < truths.size(); i++) { // loop through all datapoints
			if (stringencies.get(i).get(0) && stringencies.get(i).get(1)) {
				ArrayList<ArrayList<Double>> leftEntropiesPair = getEntropies(i, 0, whichAlphabet); // get left entropies
				Double leftScore = doEntropySamplingAndCalculation(leftEntropiesPair.get(0), leftEntropiesPair.get(1)); // and calculate the score
				ArrayList<ArrayList<Double>> rightEntropiesPair = getEntropies(i, 1, whichAlphabet); // get right entropies
				Double rightScore = doEntropySamplingAndCalculation(rightEntropiesPair.get(0), rightEntropiesPair.get(1)); // and calculate the score
				double finalScore = Double.NaN; // default value for final score
				if (!Double.isNaN(leftScore) && !Double.isNaN(rightScore)) { // if neither side is NaN, average for final score
					finalScore = (leftScore + rightScore) / 2.0;
				} else if (Double.isNaN(leftScore)) { // if left is NaN, use right for final
					finalScore = rightScore;
				} else if (Double.isNaN(rightScore)) { // if right is NaN, use left for final
					finalScore = leftScore;
				} // otherwise, both are NaN, final stays NaN
				boolean finalCall = Boolean.FALSE;
				if (!Double.isNaN(finalScore)) {
					finalCall = finalScore < CALL_THRESHOLD;
					if (finalCall) {
//						System.out.println(i + " prediction: bio, since " + finalScore + " < " + CALL_THRESHOLD);
					} else {
//						System.out.println(i + " prediction: xtal, since " + finalScore + " >= " + CALL_THRESHOLD);
					}
				} else {
//					System.out.println(i + " prediction: xtal, since " + finalScore + " is NaN");
				}
				boolean truth = truths.get(i);
				if (finalCall && truth) {
					truePositives++;
				} else if (!finalCall && !truth) {
					trueNegatives++;
				} else if (finalCall && !truth) {
					falsePositives++;
				} else if (!finalCall && truth) {
					falseNegatives++;
				}
			}
		}
		if (truePositives + falseNegatives < 1) {
			return 0.0;
		}
		if (trueNegatives + falsePositives < 1) {
			return 0.0;
		}
		double sensitivity = truePositives / (truePositives + falseNegatives);
		double specificity = trueNegatives / (falsePositives + trueNegatives);
		return (sensitivity + specificity) / 2.0;
	}
	
	private static double doEntropySamplingAndCalculation(ArrayList<Double> coreEntropies, ArrayList<Double> surfaceEntropies) {
		if (coreEntropies.size() == 0) {
			return Double.NaN;
		}
		if (surfaceEntropies.size() < (coreEntropies.size() * NUM_SURF_RES_TOLERANCE)) {
			return Double.NaN;
		}
		double coreTotal = 0.0;
		double coreCount = 0.0;
		for (double thisEntropy : coreEntropies) {
			coreTotal += thisEntropy;
			coreCount += 1.0;
		}
		double coreScore = coreTotal / coreCount;
		final int SAMPLE_SIZE = coreEntropies.size();
		double surfaceScoreMean = Double.NaN;
		double surfaceScoreSD = Double.NaN;
		double[] surfaceScoreDistribution = new double[NUMBER_OF_SURFACE_SAMPLES];
		RandomDataImpl rd = new RandomDataImpl();
		for (int i = 0; i < NUMBER_OF_SURFACE_SAMPLES; i++) {
			Object[] sample = rd.nextSample(surfaceEntropies, SAMPLE_SIZE);
			double total = 0.0;
			double count = 0.0;
			for (int j = 0; j < sample.length; j++) {
				total += (double)sample[j];
				count += 1.0;
			}
			surfaceScoreDistribution[i] = total / count;
		}
		UnivariateStatistic stat = new Mean();
		surfaceScoreMean = stat.evaluate(surfaceScoreDistribution);
		stat = new StandardDeviation();
		surfaceScoreSD = stat.evaluate(surfaceScoreDistribution);
		double zScore = Double.NaN;
		if (surfaceScoreSD != 0) {
			zScore = (coreScore - surfaceScoreMean) / surfaceScoreSD;
		}
		else {
			if ((coreScore - surfaceScoreMean) > 0) {
				zScore = SCORE_INFINITY_VALUE;
			} else if ((coreScore - surfaceScoreMean) < 0) {
				zScore = -SCORE_INFINITY_VALUE;
			} else {
				zScore = Double.NaN;
			}
		}
		return zScore;
	}
	
	private static Set<AAAlphabet> evolve(Set<AAAlphabet> oldPool) {
		AAAlphabet[] arrayAlphabets = new AAAlphabet[oldPool.size()];
		int i = 0;
		for (AAAlphabet alphabet : oldPool) {
			double fitness = testFitness(alphabet);
			System.out.println(alphabet + "\t" + fitness);
			alphabet.setFitness(fitness);
			arrayAlphabets[i] = alphabet;
			i++;			
		}
		Arrays.sort(arrayAlphabets);
		Set<AAAlphabet> newPool = new HashSet<AAAlphabet>();
		for (int j = (arrayAlphabets.length / 2); j < arrayAlphabets.length; j++) {
			newPool.add(mutateAlphabet(arrayAlphabets[j]));
			newPool.add(mutateAlphabet(arrayAlphabets[j]));
		}
//		newPool.add(mutateAlphabet(arrayAlphabets[arrayAlphabets.length - 2]));
//		newPool.add(mutateAlphabet(arrayAlphabets[arrayAlphabets.length - 1]));
		return newPool;
	}
	
	
	private static AAAlphabet mutateAlphabet(AAAlphabet alphabet) {
		ArrayList<String> groups = new ArrayList<String>(Arrays.asList(alphabet.getGroups()));
		while (groups.size() < 20) {
			groups.add("");
		}
		Random rand = new Random();
		String targetAA = AMINO_ACIDS[rand.nextInt(AMINO_ACIDS.length)];
		int moveFrom = -1;
		int moveTo = rand.nextInt(AMINO_ACIDS.length);
		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).contains(targetAA)) {
				moveFrom = i;
				break;
			}
		}
		groups.set(moveFrom, groups.get(moveFrom).replace(targetAA, ""));
		groups.set(moveTo, groups.get(moveTo).concat(targetAA));
		String newString = "";
		for (String group : groups) {
			if (!group.isEmpty()) {
				newString += group;
				newString += ":";
			}
		}
		return new AAAlphabet(newString.substring(0, newString.length() - 1));
	}
}