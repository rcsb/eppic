package eppic.assembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.vecmath.Matrix4d;

import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.contact.StructureInterface;
import org.biojava.nbio.structure.contact.StructureInterfaceList;
import org.biojava.nbio.structure.xtal.CrystalTransform;
import org.biojava.nbio.structure.xtal.SpaceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;



public class LatticeGraph {
	
	private static final Logger logger = LoggerFactory.getLogger(LatticeGraph.class);


	private Graph<ChainVertex,InterfaceEdge> graph;
	
	
	public LatticeGraph(Structure struct, StructureInterfaceList interfaces) {
		
		graph = new UndirectedOrderedSparseMultigraph<ChainVertex, InterfaceEdge>();

		// init the graph
		initLatticeGraph(struct, interfaces);
		
		logger.info("Found {} vertices and {} edges in unit cell", graph.getVertexCount(), graph.getEdgeCount());
		
		List<InterfaceEdge> sortedEdges = new ArrayList<InterfaceEdge>();
		sortedEdges.addAll(graph.getEdges());
		Collections.sort(sortedEdges, new Comparator<InterfaceEdge>() {
			@Override
			public int compare(InterfaceEdge o1, InterfaceEdge o2) {
				return new Integer(o1.getInterfaceId()).compareTo(new Integer(o2.getInterfaceId()));
			}			
		});
		
		for (InterfaceEdge edge:sortedEdges) {
			Pair<ChainVertex> vertices = graph.getEndpoints(edge);
			//logger.info("Edge {} (cluster {}) between {} (entity {}) and {} (entity {})", 
			//		edge.getInterfaceId(), edge.getClusterId(),
			//		vertices.getFirst().getChainId()+vertices.getFirst().getOpId(), vertices.getFirst().getEntity(),
			//		vertices.getSecond().getChainId()+vertices.getSecond().getOpId(), vertices.getSecond().getEntity());
			logger.info("Edge {} between {} - {} ", 
					edge.getInterfaceId(), 
					vertices.getFirst().getChainId()+vertices.getFirst().getOpId(), 
					vertices.getSecond().getChainId()+vertices.getSecond().getOpId());

		}
		
	}

	public Graph<ChainVertex, InterfaceEdge> getGraph() {
		return graph;
	}

	private void initLatticeGraph(Structure struct, StructureInterfaceList interfaces) {
		
		SpaceGroup sg = struct.getCrystallographicInfo().getSpaceGroup();

		for (int i=0;i<sg.getNumOperators();i++) {

			Matrix4d t0i = sg.getTransformation(i);

			for (int a=-1;a<=1;a++) {
				for (int b=-1;b<=1;b++) {
					for (int c=-1;c<=1;c++) {

						//if (a==0 && b==0 && c==0) continue;

						for (int j=0;j<sg.getNumOperators();j++) {

							Matrix4d t0j = (Matrix4d)sg.getTransformation(j).clone();
							t0j.m03 += a;
							t0j.m13 += b;
							t0j.m23 += c;

							Matrix4d t0k = getEquivalentAuTransform(t0i, t0j);

							for (Chain iChain:struct.getChains()) {
								String iChainId = iChain.getChainID();
								for (Chain jChain:struct.getChains()) {
									String jChainId = jChain.getChainID();

									int interfaceId = 
											getMatchingInterfaceId(iChainId, jChainId, t0k, interfaces);

									if (interfaceId>0) {
										logger.info("Interface id {} matched for pair \n{}\n{}{}\n{}",
												interfaceId, iChainId, t0i.toString(), jChainId, t0j.toString());

										ChainVertex ivert = new ChainVertex(iChainId, i);
										ivert.setEntity(iChain.getCompound().getMolId());
										ChainVertex jvert = new ChainVertex(jChainId, j);
										jvert.setEntity(jChain.getCompound().getMolId());
										
										List<ChainVertex> vertexPair = new ArrayList<ChainVertex>();
										vertexPair.add(ivert);
										vertexPair.add(jvert);
										ChainVertex maxVert = Collections.max(vertexPair,new Comparator<ChainVertex>() {
											@Override
											public int compare(ChainVertex o1, ChainVertex o2) {
												if (o1.getChainId().compareTo(o2.getChainId())==0) {
													return new Integer(o1.getOpId()).compareTo(new Integer(o2.getOpId()));
												} 
												return o1.getChainId().compareTo(o2.getChainId());
											}
										});
										ChainVertex minVert = (ivert==maxVert)?jvert:ivert;
										
										InterfaceEdge edge = new InterfaceEdge(interfaceId);
										edge.setClusterId(interfaces.get(interfaceId).getCluster().getId());
										edge.setIsologous(interfaces.get(interfaceId).isIsologous());
										edge.setInfinite(interfaces.get(interfaceId).isInfinite());

										graph.addEdge(edge, minVert, maxVert, EdgeType.UNDIRECTED);
										
									} else {
										//logger.info("No interface id matched for pair {}  {}",
										//		iChainId+"-"+t0i.toString(), jChainId+"-"+t0j.toString());

									}
								}
							}
						}						
					}					
				}
			}
		}
	}
	
	private int getMatchingInterfaceId(String iChainId, String jChainId, Matrix4d t0k,  
			StructureInterfaceList interfaces) {
		
		// find matching interface id from given list
		
		for (StructureInterface interf:interfaces) {
			
			if ( (interf.getMoleculeIds().getFirst().equals(iChainId) && 
				  interf.getMoleculeIds().getSecond().equals(jChainId)   )  ||
				 (interf.getMoleculeIds().getFirst().equals(jChainId) && 
				  interf.getMoleculeIds().getSecond().equals(iChainId)   )) {

				if (interf.getTransforms().getSecond().getMatTransform().epsilonEquals(t0k, 0.0001)) {
					return interf.getId();
				}

				Matrix4d mul = new Matrix4d();
				mul.mul(interf.getTransforms().getSecond().getMatTransform(), t0k);

				if (mul.epsilonEquals(CrystalTransform.IDENTITY, 0.0001)) {
					return interf.getId();
				}
				
				
			}
		}
		
		// if not found, return -1
		return -1;
	}
	
	/**
	 * Finds the equivalent m0k operator given 2 operators m0i and m0j.
	 * That is, this will find the operator mij expressed in terms of an 
	 * original-AU operator (what we call m0k)
	 * @param m0i
	 * @param m0j
	 * @return the m0k original-AU operator
	 */
	public static Matrix4d getEquivalentAuTransform(Matrix4d m0i, Matrix4d m0j) {
		Matrix4d m0k = new Matrix4d();

		// we first need to find the mij
		// mij = m0j * m0i_inv
		Matrix4d m0iinv = new Matrix4d();
		m0iinv.invert(m0i);

		// Following my understanding, it should be m0j*m0i_inv as indicated above, but that didn't work 
		// and for some reason inverting the order in mul does work. Most likely my understanding is 
		// wrong, but need to check this better at some point
		// A possible explanation for this is that vecmath treats transform() as a pre-multiplication
		// rather than a post-multiplication as I was assuming, i.e. x1 = x0 * m0i instead of x1 = m0i * x0,
		// in that case then it is true that: mij = m0i_inv * m0j, which would explain the expression below
		m0k.mul(m0iinv, m0j);

		return m0k;
	}
	
	private class UnitCellChain {
		public Chain c;
		public Matrix4d m;
		public int opId;
		public UnitCellChain(Chain c, Matrix4d m, int opId) { this.c=c;this.m=m;this.opId=opId; };
	}
	
	private List<UnitCellChain> listUnitCells(Structure struct, int numCells) {
		List<UnitCellChain> list = new ArrayList<LatticeGraph.UnitCellChain>();
		SpaceGroup sg = struct.getCrystallographicInfo().getSpaceGroup();

		for (int opId=0;opId<sg.getNumOperators();opId++) {

			Matrix4d m = sg.getTransformation(opId);

			for (int a=-numCells;a<=numCells;a++) {
				for (int b=-numCells;b<=numCells;b++) {
					for (int c=-numCells;c<=numCells;c++) {
						m.m03 += a;
						m.m13 += b;
						m.m23 += c;
						for (Chain chain:struct.getChains()) {
							list.add(new UnitCellChain(chain, m, opId));
						}
					}
				}
			}
		}
		return list;
	}
}
