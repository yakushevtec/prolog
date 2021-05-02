package com.prova.prolog;

import java.util.*;

public class Rule
{
	Term head;
	List<Term> goals = new ArrayList<>();

	Rule(String str) throws Exception // expect "term-:term;term;..."
	{
		str = str.trim();
		String[] flds = str.split(":-");
		head = new Term(flds[0], null);
		if(flds.length == 2)
		{
			for(String f:Core.split(flds[1], ",", true))
				goals.add(new Term(f, null));
		}
	}
	Rule(Term head, List<Term> goals)
	{
		this.head = head;
		this.goals = goals;
	}

	public Rule deepCopy()
	{
		Rule res = null;
		List<Term> tGoals = Core.deepCopyTerms(goals);
		res = new Rule(head.deepCopy(), tGoals);
		return res;
	}

	public String toString()
	{
		String res = ""+head;
		res += " :- ";
		int i = 0;
		for(Term t:goals)
			res += i++>0 ? ","+t : t;
		return res;
	}
}

