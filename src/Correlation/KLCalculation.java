package Correlation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class KLCalculation {

	private static double EPSILON = 1e-7;
	public static double computeKL(Map<String, Double> sample1, Map<String, Double> sample2) {
		Set<String> sample1Ids = sample1.keySet();
		Set<String> sample2Ids = sample2.keySet();
		Set<String> union = new HashSet<String>(sample1Ids);
		union.addAll(sample2Ids);
		// System.out.println(union); 
		
		HashMap<String, Double> pi = smoothPdf(union, sample1Ids, sample1);
		HashMap<String, Double> qi = smoothPdf(union, sample2Ids, sample2);
		
		// System.out.println(union);
		double sum = 0;
		for (Iterator<String> iterator = union.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			// System.out.println(string); 
			sum += pi.get(string)*Math.log(pi.get(string)/qi.get(string));
		}
		
		return sum;		
	}
	
	public static HashMap<String, Double> smoothPdf(Set<String> union, Set<String> entries, Map<String, Double> sample) {
		double len = entries.size();
		Set<String> difference = new HashSet<String>(union);
		difference.removeAll(entries);
		double dlen = difference.size();
		double pqc = EPSILON*(dlen/len);		
		HashMap<String, Double> pdf = new HashMap<String, Double>(union.size());
		
		double sum = 0;
		Iterator<String> iterator = entries.iterator();		
		while(iterator.hasNext()) {
			String string = (String) iterator.next(); // this is id
			double value = sample.get(string);			
			sum += value;
		}
		
		for (Iterator<String> iterator2 = union.iterator(); iterator2.hasNext();) {
			String string = (String) iterator2.next();
			iterator = entries.iterator();
			String it = null; 
			while(iterator.hasNext()) {
				it = iterator.next();				
				if(it.equalsIgnoreCase(string)) {
					double pqi = sample.get(string)/sum;
					pdf.put(string, pqi - pqc);					
					break;
				}
			}
			if(!string.equalsIgnoreCase(it)) {				
				pdf.put(string, EPSILON);
			}
			// System.out.println(string + "\t" + pdf.get(string));
			
		}			
		return pdf;		
	}
}