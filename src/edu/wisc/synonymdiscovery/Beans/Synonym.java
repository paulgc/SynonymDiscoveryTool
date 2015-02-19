package edu.wisc.synonymdiscovery.Beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Synonym implements Serializable{

	private static final long serialVersionUID = 1L;

	private final List<String> synonyms;

	public static Synonym constructSynonym(String synStr, String sep) {
		Synonym syn = new Synonym();
		for(String s : synStr.split(sep))
			syn.getSynonyms().add(s);
		return syn;
	}

	public Synonym() {
		this.synonyms = new ArrayList<String>();
		for(String syn : synonyms) 
			if(!syn.equals("\\syn"))
				this.synonyms.add(syn);
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public void addSynonym(List<String> syn) {
		for(String s : syn)
			synonyms.add(s);
	}

	@Override
	public int hashCode() {
		int res = synonyms.get(0).hashCode();
		for(int i=1;i<synonyms.size();i++)
			res = res ^ synonyms.get(i).hashCode();
		return res;
	} 

	@Override
	public boolean equals(Object obj) {
		Synonym cand = (Synonym) obj;
		if(cand.synonyms.size() != synonyms.size())
			return false;
		for(int i=0;i<cand.synonyms.size();i++)
			if(!cand.synonyms.get(i).equals(synonyms.get(i)))
				return false;
		return true;
	}

	public String toString() {
		String s="";
		for(String syn : synonyms)
			s+=syn+",";
		return s;
	}

}
