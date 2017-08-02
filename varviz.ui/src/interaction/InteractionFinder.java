package interaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Edge;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Set;

public class InteractionFinder {
	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}
	
	
	static FeatureExpr createUnique(SingleFeatureExpr feature, List<FeatureExpr> contexts) {
		FeatureExpr unique = FeatureExprFactory.False();
		for (FeatureExpr ctx : contexts) {
			unique = unique.or(ctx.unique(feature));
		}
		return unique;
	}
		
	public void getInteractionsTable(List<FeatureExpr> expressions) {
		
		Collection<SingleFeatureExpr> features = Conditional.features.values();//the whole set of features
		List<PairExp> exprPairs = new ArrayList<>();//the pairs present in the expressions
		List<PairExp> contain = new ArrayList<>();//only to not repeat the same pair "do not interact"
		List<SingleFeatureExpr> noEffectlist = new ArrayList<>();
		Map<PairExp, List<String>> hashMap = new HashMap<>();
		
		List<List> lists = getExpressionsPairs(expressions);
		exprPairs = lists.get(0);//get all the pairs in the expressions
		List<TriExp> exprTri = lists.get(1);
		noEffectlist = getNoEffectlist(features, expressions);//list of features that do not appear in the expressions
		
		for (SingleFeatureExpr feature1 : features) {
			
			if (Conditional.isTautology(feature1)) {
				continue;
			}
			final FeatureExpr unique = createUnique(feature1, expressions);
			
			if (Conditional.isContradiction(unique)) {//when a feature doesn't appear in the expressions
				continue;
			}		
			
			for (SingleFeatureExpr feature2 : features) {
				if (feature1 == feature2 || Conditional.isTautology(feature2)) {
					continue;//Conditional.isTautology(feature2) when the feature is the feature model root feature
				}			
				FeatureExpr first = feature2.implies(unique.not());
				FeatureExpr second = feature2.not().implies(unique.not());
				String phrase = new String("a");
									
				if (first.isTautology()) {
					//System.out.println(Conditional.getCTXString(feature1) + " suppresses " + Conditional.getCTXString(feature2));
					phrase = Conditional.getCTXString(feature1) + " suppresses " + Conditional.getCTXString(feature2);
				}
				if (second.isTautology()) {
					//System.out.println(Conditional.getCTXString(feature1) + " enables " + Conditional.getCTXString(feature2));
					phrase = Conditional.getCTXString(feature1) + " enables " + Conditional.getCTXString(feature2);
				}			
					
				PairExp pairAB = new PairExp(feature1, feature2);
				PairExp pairBA = new PairExp(feature2, feature1);
				//System.out.println("Features: " + feature1 + " , " + feature2);
				
				//if the pair is no present in the expressions
				if (!exprPairs.contains(pairAB) && !exprPairs.contains(pairBA) && !contain.contains(pairAB)){
					
					if (!noEffectlist.contains(feature1) && !noEffectlist.contains(feature2)) {				
						phrase = "do not interact";
					}
					else if(noEffectlist.contains(feature1)){
						phrase = Conditional.getCTXString(feature1) + " has no effect";
					}
					
					else if(noEffectlist.contains(feature2)){
						phrase = Conditional.getCTXString(feature2) + " has no effect";
					}
					contain.add(pairAB);//to avoid repeat the same pair in a different order
					contain.add(pairBA);
				}
				
				if((!hashMap.containsKey(pairAB)) && (!hashMap.containsKey(pairBA)) && (!phrase.equals("a"))){
					hashMap.put(pairAB, new ArrayList<>());
					hashMap.get(pairAB).add(phrase);
				}
				else{
					if(!phrase.equals("a")){
					hashMap.get(pairAB).add(phrase);
					}
				}
			}	
		}
		System.out.println("teste");
		//when both features of a pair have no effect
		addDoubleNoEffect(noEffectlist, hashMap);
		
		//when both features of a pair interact but they are not suppressing or enabling each other
		String phrase = "a";
		for(PairExp pair: exprPairs){
			if(!hashMap.containsKey(pair)){
				phrase = "do interact";
				hashMap.put(pair, new ArrayList<>());
				hashMap.get(pair).add(phrase);			
			}
		}
		
		//verify features in a tri-interaction by removing "do not interact" statement when necessary
		for(TriExp pair: exprTri){
			
			SingleFeatureExpr A  = (SingleFeatureExpr) pair.A;
			SingleFeatureExpr B  = (SingleFeatureExpr) pair.B;
			SingleFeatureExpr C  = (SingleFeatureExpr) pair.C;
			
			PairExp pairAB = new PairExp(A,B);
			PairExp pairAC = new PairExp(A,C);
			PairExp pairBC = new PairExp(B,C);
			
			verifyTriInteraction(hashMap,pairAB);
			verifyTriInteraction(hashMap,pairAC);
			verifyTriInteraction(hashMap,pairBC);		
		}
				
		//creates excel table
		createExcelTable(hashMap, features);
		
		
	
	}

	//verify the interactions in a tri-interaction
	private void verifyTriInteraction(Map<PairExp, List<String>> hashMap, PairExp pair) {
			if(!hashMap.containsKey(pair)){
				hashMap.put(pair, new ArrayList<>());
				hashMap.get(pair).add("do interact");
			}
			else{
				if(hashMap.get(pair).size() == 1 && hashMap.get(pair).get(0).equals("do not interact")){
					hashMap.get(pair).remove(0);				
					hashMap.get(pair).add("do interact");
				}
				if(hashMap.get(pair).size() > 1){
					int s = hashMap.get(pair).size();
					for(int i=0; i<s; i++){		
						String p = hashMap.get(pair).get(i);
						if(p.equals("do not interact")){
							hashMap.get(pair).remove(i);
							i--;
							s--;
						}
					}
				}
			}
			
		}

	
	private void createExcelTable(Map<PairExp, List<String>> hashMap, Collection<SingleFeatureExpr> features) {
		//print hash
		for (Entry<PairExp, List<String>> pair : hashMap.entrySet()) {
			System.out.println("Pair = [" + pair.getKey() + " , " + pair.getValue() + "]");
		}
		
		 Map < String, Object[] > excelTable = new TreeMap < String, Object[] >();
		 int count = 0;
		 Object[] line1 = new Object[features.size()+1];
		 line1[count++] = "Features";//array of 1 line
		 for (SingleFeatureExpr feature1 : features) {
			 line1[count++] = Conditional.getCTXString(feature1);
		 }
		 excelTable.put( Integer.toString(1), line1);//first line with the name of all features
		 
		 
		 int excelline = 2;
		 for (int i = 1; i< line1.length; i++) {
			 count = 1;
			 Object[] line = new Object[features.size()+1];
			 line[0] = line1[i];
			 for (int j = 1; j< line1.length; j++) {
				 
				 if(line1[j].equals(line[0])){
					 line[count] = " X ";
					 System.out.println("line[" + count + "] = " + line[count]);
					 count++;
					 continue;
				 }
				 
				 for (Entry<PairExp, List<String>> pair : hashMap.entrySet()) {
					 String A = Conditional.getCTXString(pair.getKey().A);
					 String B = Conditional.getCTXString(pair.getKey().B);
					 //System.out.println("A: " + A + " = " + line[0] + " && " + "B: " + B + " = " + line1[j]);
					// System.out.println("A: " + A + " = " + line1[j] + " && " + "B: " + B + " = " + line[0]);
					 
					 
					 if(A.equals(line[0]) && B.equals(line1[j])
						|| A.equals(line1[j]) && B.equals(line[0])) {
						 
						 line[count] = pair.getValue().get(0);
						 if(pair.getValue().size() >1){
							 line[count] = "both have no effect";
						 }
						 
						 System.out.println("line[" + count + "] = " + line[count]);
						 count++;
					 }
				 }
			 }
			 
			 excelTable.put( Integer.toString(excelline++), line);
		 }
		
		Excel ex = new Excel();
		try {
			ex.writesheet(excelTable, new File(""));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	//add in the table when both features of a pair have no effect
	private void addDoubleNoEffect(List<SingleFeatureExpr> noEffectlist, Map<PairExp, List<String>> hashMap) {
		
		if(noEffectlist.size()>1){
			String phrase = "a";
			for(int i = 0; i<noEffectlist.size(); i++){
				for(int j = i+1; j<noEffectlist.size(); j++){	
					System.out.println("no effect list: " + "i " + i + " " + noEffectlist.get(i) + " j " + j + " " + noEffectlist.get(j));
					SingleFeatureExpr noA = noEffectlist.get(i);
					SingleFeatureExpr noB = noEffectlist.get(j);
					PairExp pairNoEffect = new PairExp(noA, noB);
					PairExp pairNoEffect2 = new PairExp(noB, noA);
					if(!hashMap.containsKey(pairNoEffect) && !hashMap.containsKey(pairNoEffect2)){
						hashMap.put(pairNoEffect, new ArrayList<>());
						phrase = Conditional.getCTXString(noEffectlist.get(i)) + " has no effect";
						hashMap.get(pairNoEffect).add(phrase);
						phrase = Conditional.getCTXString(noEffectlist.get(j)) + " has no effect";
						hashMap.get(pairNoEffect).add(phrase);
					}
					else{
						hashMap.get(pairNoEffect).add(phrase);
					}
				}
			}
		}		
	}

	//list of features that do not appear in the expressions
	private List<SingleFeatureExpr> getNoEffectlist(Collection<SingleFeatureExpr> features,
			List<FeatureExpr> expressions) {
		
		List<SingleFeatureExpr> noEffectlist = new ArrayList<>();
		for (SingleFeatureExpr feature : features) {
			final FeatureExpr unique = createUnique(feature, expressions);
			
			if (Conditional.isTautology(feature)) {
				continue;
			}
			
			if (Conditional.isContradiction(unique)) {//when a feature doesn't appear in the expressions
				noEffectlist.add(feature);
			}
		}
		return noEffectlist;
	}

	//get all the pairs in the expressions
	private List<List> getExpressionsPairs(List<FeatureExpr> expressions) {
		List<PairExp> exprPairs = new ArrayList<>();
		List<TriExp> exprTri = new ArrayList<>();
		
		for(FeatureExpr featureexpr : expressions){
			
			Set<String> dist = featureexpr.collectDistinctFeatures();
			if(dist.size() == 2){
				scala.collection.Iterator<String> it = dist.iterator();
				String s = it.next().substring(7);
				String s2 = it.next().substring(7);
				
				// Step 2: get features
				SingleFeatureExpr f1  = Conditional.createFeature(s);
				SingleFeatureExpr f2 = Conditional.createFeature(s2);
				
				PairExp pairAB = new PairExp(f1, f2);			
				if (!exprPairs.contains(pairAB)){
					exprPairs.add(pairAB);
				}
			}
			if(dist.size() == 3){
				scala.collection.Iterator<String> it = dist.iterator();
				String s = it.next().substring(7);
				String s2 = it.next().substring(7);
				String s3 = it.next().substring(7);
				
				// Step 2: get features
				SingleFeatureExpr f1  = Conditional.createFeature(s);
				SingleFeatureExpr f2 = Conditional.createFeature(s2);
				SingleFeatureExpr f3 = Conditional.createFeature(s3);
				
				TriExp pairABC = new TriExp(f1, f2, f3);			
				if (!exprTri.contains(pairABC)){
					exprTri.add(pairABC);
				}
			}
		}

		List<List> lists = new ArrayList<>();
		lists.add(exprPairs);
		lists.add(exprTri);
		
		return lists;
	}

	class PairExp {
		FeatureExpr A, B;

		public PairExp(SingleFeatureExpr a, SingleFeatureExpr b) {
			A = a;
			B = b;
		}
		public FeatureExpr getA() {
			return A;
		}
		public FeatureExpr getB() {
			return B;
		}
		
		@Override
		public boolean equals(Object obj) {
			PairExp other =  (PairExp) obj;
			return (A.equals(other.A) && B.equals(other.B)) ||
					(A.equals(other.B) && B.equals(other.A));
		}
		
		@Override
		public int hashCode() {
			return A.hashCode() * B.hashCode() * 31;
		}
		
		@Override
		public String toString() {
			return Conditional.getCTXString(A) + ", " + Conditional.getCTXString(B);
		}
	}
	
	class TriExp {
		FeatureExpr A, B, C;

		public TriExp(SingleFeatureExpr a, SingleFeatureExpr b, SingleFeatureExpr c) {
			A = a;
			B = b;
			C = c;
		}
		public FeatureExpr getA() {
			return A;
		}
		public FeatureExpr getB() {
			return B;
		}
		public FeatureExpr getC() {
			return C;
		}
		
		@Override
		public boolean equals(Object obj) {
			TriExp other =  (TriExp) obj;
			return (A.equals(other.A) && B.equals(other.B) &&  C.equals(other.C)) ||
					(A.equals(other.B) && B.equals(other.A) && C.equals(other.C)) ||
					(A.equals(other.B) && B.equals(other.C) && C.equals(other.A)) ||
					(A.equals(other.A) && B.equals(other.C) && C.equals(other.B)) ||
					(A.equals(other.C) && B.equals(other.A) && C.equals(other.B)) ||
					(A.equals(other.C) && B.equals(other.B) && C.equals(other.A));
		}
		
		@Override
		public int hashCode() {
			return A.hashCode() * B.hashCode() * C.hashCode() * 31;
		}
		
		@Override
		public String toString() {
			return Conditional.getCTXString(A) + ", " + Conditional.getCTXString(B) + ", " + Conditional.getCTXString(C);
		}
	}
	
	
}
