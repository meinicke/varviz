package interaction;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.junit.Test;

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
	
	final static SingleFeatureExpr blocking = Conditional.createFeature("blocking");
	final static SingleFeatureExpr voiceMail = Conditional.createFeature("VoiceMail");
	final static SingleFeatureExpr parallel = Conditional.createFeature("Parallel");
	final static SingleFeatureExpr forward = Conditional.createFeature("Forward");
	final static SingleFeatureExpr weight = Conditional.createFeature("weight");
	final static SingleFeatureExpr executivefloor = Conditional.createFeature("executivefloor");
	final static SingleFeatureExpr overloaded = Conditional.createFeature("overloaded");
	
	@Test
	public void testASupBForm() {
		List<FeatureExpr> expressions = new ArrayList<>();
		expressions.add(blocking);
		expressions.add(blocking.not());
		
		expressions.add(blocking.not().and(voiceMail));
		expressions.add(blocking.not().and(voiceMail.not()));
//		expressions.add(blocking.not().and(forward));
		expressions.add(parallel.and(forward));
		expressions.add(parallel.and(forward.not()));		
		
//		expressions.add(weight);
		expressions.add(overloaded.and(blocking));
		//expressions.add(executivefloor);
		//expressions.add(weight.not());
		//expressions.add(executivefloor.not());
		//expressions.add(weight.and(executivefloor.not()));
//		expressions.add(weight.not().and(executivefloor));
		
//		expressions.add(overloaded);	
//		expressions.add(overloaded.and(executivefloor));
//		expressions.add(overloaded.and(executivefloor.not()));
//		expressions.add(parallel.and(forward));
//		expressions.add(overloaded.not());
//		
//		expressions.add(overloaded.not().or(executivefloor.not()));
//		expressions.add(overloaded.not().and(executivefloor));
//		
//		expressions.add(weight.and(executivefloor.not()));
		//expressions.add(weight.not().and(executivefloor.not()));

//		expressions.add(overloaded.not().and(executivefloor).and(weight));
		//expressions.add(weight.not().and(executivefloor));
//
//		expressions.add(overloaded.not().and(executivefloor.not()));
		
		// check for return value
		getSuppressionsForm(expressions);
	//	System.out.println(result);
		

	//	Suppression actualSuppression2 = result.get(0);
	//	assertEquals(expectedSuppression2, actualSuppression2);
	}
	
	static FeatureExpr createUnique(SingleFeatureExpr feature, List<FeatureExpr> contexts) {
		FeatureExpr unique = FeatureExprFactory.False();
		for (FeatureExpr ctx : contexts) {
			//System.out.println("ctx.unique(feature):" + ctx.unique(feature));
			unique = unique.or(ctx.unique(feature));
		}
		return unique;
	}
		
	private void getSuppressionsForm(List<FeatureExpr> expressions) {
		
		Collection<SingleFeatureExpr> features = Conditional.features.values();//the whole set of features
		List<PairExp> exprPairs = new ArrayList<>();//the pairs present in the expressions
		List<PairExp> contain = new ArrayList<>();//only to not repeat the same pair "do not interact"
		List<SingleFeatureExpr> noEffectlist = new ArrayList<>();
		Map<PairExp, List<String>> hashMap = new HashMap<>();
		
		exprPairs = getExpressionsPairs(expressions);//get all the pairs in the expressions
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
					//hashMap.get(pairBA).add(phrase);
				}
				else{
					if(!phrase.equals("a")){
					hashMap.get(pairAB).add(phrase);
					}
				}
			}	
		}
		
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
		
		//creates excel table
		createExcelTable(hashMap, features);
	
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
					PairExp pairNoEffect = new PairExp(noEffectlist.get(i), noEffectlist.get(j));
					PairExp pairNoEffect2 = new PairExp(noEffectlist.get(j), noEffectlist.get(i));
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
			if (Conditional.isContradiction(unique)) {//when a feature doesn't appear in the expressions
				noEffectlist.add(feature);
			}
		}
		return noEffectlist;
	}

	//get all the pairs in the expressions
	private List<PairExp> getExpressionsPairs(List<FeatureExpr> expressions) {
		
		List<PairExp> exprPairs = new ArrayList<>();
		
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
		}
		return exprPairs;
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
		

//	class Suppression {
//		FeatureExpr A, B;
//
//		public Suppression(FeatureExpr a, FeatureExpr b) {
//			A = a;
//			B = b;
//		}
//		
//		@Override
//		public String toString() {
//			return Conditional.getCTXString(A) + " suppresses " + Conditional.getCTXString(B);
//		}
//		
//		
//		@Override
//		public boolean equals(Object obj) {
//			Suppression other = (Suppression)obj;
//			return A.equals(other.A) && B.equals(other.B);
//		}
//	}	
//		
		
	//	
//		private List<Suppression> getSuppressionsForm(List<FeatureExpr> expressions) {
//			
//			final List<Suppression> suppressions = new ArrayList<>();		
//			Map<PairExp, List<FeatureExpr>> hashMap = new HashMap<>();
//			
//			// Step 1: find expr with 2 features 
//			for(FeatureExpr featureexpr : expressions){
//				
//				System.out.println("expression: " + Conditional.getCTXString(featureexpr));
//				Set<String> dist = featureexpr.collectDistinctFeatures();//get the features of an expression
//				
//				if(dist.size() == 2){ //ps: now I'm just considering pair of features
	//
//						scala.collection.Iterator<String> it = dist.iterator();
//						String s = it.next().substring(7);
//						String s2 = it.next().substring(7);
//						
//						// Step 2: get features
//						FeatureExpr f  = Conditional.createFeature(s);;
//						FeatureExpr f2 = Conditional.createFeature(s2);;
//						
//						PairExp pairAB = new PairExp(f, f2);
//						if (hashMap.containsKey(pairAB)) {
//							hashMap.get(pairAB).add(featureexpr);
//						}
//						else{					
//							//if (!hashMap.containsKey(pairAB)) {
//								hashMap.put(pairAB, new ArrayList<>());								
//							//}
//							hashMap.get(pairAB).add(featureexpr);					
//						}
//				}
//			}	
//			
	//
//			for (Entry<PairExp, List<FeatureExpr>> pair : hashMap.entrySet()) {
//				System.out.println("Pair = [" + pair.getKey() + " , " + pair.getValue() + "]");
//				SingleFeatureExpr key = (SingleFeatureExpr) pair.getKey().A; 
//				SingleFeatureExpr key2 = (SingleFeatureExpr) pair.getKey().B;
//				
//				//for(int i=0; i<expressions.size(); i++){
//				for(int i=0; i<pair.getValue().size(); i++){
//					//FeatureExpr featureexpr = expressions.get(i);
//					FeatureExpr featureexpr = pair.getValue().get(i);
//					Set<String> dist = featureexpr.collectDistinctFeatures();//get the features of an expression
//					scala.collection.Iterator<String> it = dist.iterator();
//					String s = it.next().substring(7);
//					String s2 = it.next().substring(7);
//					if( (Conditional.getCTXString(key).equals(s) && Conditional.getCTXString(key2).equals(s2) ) || ( Conditional.getCTXString(key).equals(s2) && Conditional.getCTXString(key2).equals(s) )){
	//
//						FeatureExpr ctx1 = featureexpr;
//						FeatureExpr ctx2 = null;			
//						for(int j=i; j<pair.getValue().size(); j++){
//						
//							FeatureExpr featureexpr2 = pair.getValue().get(j);
//							
//							if(featureexpr != featureexpr2){
//								System.out.println("Comparing: " + Conditional.getCTXString(featureexpr) + " or " + Conditional.getCTXString(featureexpr2));						
//								ctx2 = featureexpr2;						
//								FeatureExpr orResult = ctx1.or(ctx2);
//								FeatureExpr unique = orResult.unique(key);
//								FeatureExpr unique2 = orResult.unique(key2);
//								
//								scala.collection.Iterator<String> ite = orResult.collectDistinctFeatures().iterator();
//								String supresser = ite.next().substring(7);						
//								if (unique.isContradiction()){
//									System.out.println("Warning: " + supresser +  " supresses " + Conditional.getCTXString(key));	
//									suppressions.add(new Suppression(key, key2));
//								}
//								if (unique2.isContradiction()){
//									System.out.println("Warning: " + supresser +  " supresses " + Conditional.getCTXString(key2));
//									suppressions.add(new Suppression(key, key2));
//								}
//							}
//						}
//					}
//				}			
//			}		
//			
//			return suppressions;
//		}
	
	
}
