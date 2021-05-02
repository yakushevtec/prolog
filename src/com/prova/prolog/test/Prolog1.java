package com.prova.prolog.test;

import java.util.*;

import com.prova.common.*;

public class Prolog1
{
	int goalId = 0;
	List<Rule> rules = new ArrayList<>();
	boolean trace = false;

	class Term implements Cloneable
	{
		String pred;
		String[] args;

		Term(String str) throws Exception // expect "x(y,z...)"
		{
			str = str.trim();
			if(str.charAt(str.length()-1) != ')')
				new Exception("Syntax error in term: " + str);
			String[] flds = str.split("\\(");
 			if(flds.length != 2)
 				new Exception("Syntax error in term: " + str);
			pred = flds[0];
			flds[1] = flds[1].substring(0, flds[1].length()-1);
 			args = flds[1].split(",");
		}

		public String toString()
		{
			return pred+"("+arrayToString(args)+")";
		}

		public Term clone() throws CloneNotSupportedException
		{  
			return (Term)super.clone();  
		}
	}

	class Rule
	{
		Term head;
		List<Term> goals = new ArrayList<>();

		Rule(String str) throws Exception // expect "term-:term;term;..."
		{
			str = str.trim();
			String[] flds = str.split(":-");
			head = new Term(flds[0]);
			if(flds.length == 2)
			{
				flds[1] = flds[1].replace("),", ")#");
				for(String s:flds[1].split("#"))
					goals.add(new Term(s));
			}
		}
		Rule(Term head, List<Term> goals)
		{
			this.head = head;
			this.goals = goals;
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

		public Rule deepCopy()
		{
			Rule res = null;
			try
			{
				List<Term> tGoals = new ArrayList<>();
				for(Term t:goals)
					tGoals.add(t.clone());
				res = new Rule(head.clone(), tGoals);
			}
			catch(Exception e){e.printStackTrace();}
			return res;
		}
	}

	// A Goal is a rule in at a certain point in its computation. 
	// env contains definitions (so far), inx indexes the current term
	// being satisfied, parent is another Goal which spawned this one
	// and which we will unify back to when this Goal is complete.
	class Goal
	{
		int id;
		Goal parent;
		Rule rule;
		Map<String, String> env;
		int inx = 0; // start search with 1st subgoal

		Goal(Rule rule, Goal parent, Map<String, String> env, boolean changeID)
		{
			if(changeID) id = ++goalId;
			this.rule = rule;
			this.parent = parent;
			if(env==null)
				env = new HashMap<String, String>();
			this.env = new HashMap<String, String>(env); // deep copy of env.
		}

		public String toString()
		{
			return "Goal "+id+" rule="+rule+" inx="+inx+" env="+env;
		}

		public Goal deepCopy()
		{
			Rule gRule = rule.deepCopy();
			Map<String, String> gEnv = new HashMap<String, String>(env);
			Goal gParent = parent!=null ? parent.deepCopy() : null;
			Goal res = new Goal(gRule, gParent, gEnv, false);
			res.id = id;
			res.inx = inx;
			return res;
		}
	}

	// update dest env from src. return true if unification succeeds.
	boolean unify(Term srcTerm, Map<String, String> srcEnv, Term destTerm, Map<String, String> destEnv)
	{
		if(srcTerm.args.length!=destTerm.args.length)
			return false;
		if(!srcTerm.pred.equals(destTerm.pred))
			return false;
		for(int i=0; i<srcTerm.args.length; i++)
		{
			String srcArg  = srcTerm.args[i];
			String destArg  = destTerm.args[i];

			String srcVal = srcArg;
			if(Character.isUpperCase(srcArg.charAt(0)))
				srcVal = srcEnv.get(srcArg);
			if(srcVal!=null)
			{
				if(Character.isUpperCase(destArg.charAt(0)))
				{
					String destVal = destEnv.get(destArg);
					if(destVal==null)
						destEnv.put(destArg, srcVal); // Unify !
					else if(!destVal.equals(srcVal)) // Won't unify
						return false;
				}
				else if(!destArg.equals(srcVal)) // Won't unify
					return false;
			}
		}
		return true;
	}

	void search(Term term)
	{
		goalId = 0;
		if(trace) System.out.println("search "+term);

		try
		{
			Rule r = new Rule("got(goal):-x(y)"); // Anything- just get a rule object
			Goal goal = new Goal(r, null, null, true);

			goal.rule.goals = new ArrayList<>();
			goal.rule.goals.add(term); // target is the single goal
			if(trace) System.out.println("stack "+goal);

			Stack<Goal> stack = new Stack<>();
			stack.push(goal);
			while(!stack.isEmpty())
			{
				Goal c = stack.pop(); // Next goal to consider
				if(trace) System.out.println("\tpop "+c);

				if(c.inx >= c.rule.goals.size()) // Is this one finished?
				{
					if(c.parent==null) // Yes. Our original goal?
					{
						if(c.env.size()>0)
							System.out.println(c.env); // Yes. tell user we
						else
							System.out.println("Yes"); // have a solution
						continue;
					}

					Goal parent = c.parent.deepCopy(); // Otherwise resume parent goal
					unify(c.rule.head, c.env, parent.rule.goals.get(parent.inx), parent.env);
					parent.inx++; // advance to next goal in body
					if(trace) System.out.println("stack "+parent);
					stack.push(parent); // let it wait its turn
					continue;
				}

				// No. more to do with this goal.
				term = c.rule.goals.get(c.inx); // What we want to solve
				for(Rule rule:rules) // Walk down the rule database
				{
					if(!rule.head.pred.equals(term.pred)) continue;
					if(rule.head.args.length != term.args.length) continue;
					Goal child = new Goal(rule, c, null, true); // A possible subgoal
					boolean ans = unify(term, c.env, rule.head, child.env);
					if(ans) // if unifies, stack it up
					{
						if(trace) System.out.println("stack "+child);
						stack.push(child);
					}
				}
			}
		}
		catch(Exception e){e.printStackTrace();}
	}

	public String arrayToString(String[] array)
	{
		String res = "";
		for(String s:array)
			res += res.length()>0 ? ","+s : s;
		res = res.length()>0 ? "["+res+"]" : "_";
		return res;
	}

	void testUnify()
	{
		try
		{
			Term t = new Prolog1.Term("mother(alice,bill)");
			System.out.println("\n"+t);

			Rule r = new Rule("son(X,Y):-mother(Y,X),boy(X)");
			System.out.println("\n"+r);

			Term t1 = new Prolog1.Term("boy(bill)");
			Term t2 = new Prolog1.Term("boy(alex)");
			Map<String, String> srcEnv = new HashMap<>();
			Map<String, String> destEnv = new HashMap<>();
			boolean res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog1.Term("boy(alex)");
			t2 = new Prolog1.Term("boy(alex)");
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog1.Term("boy(alex)");
			t2 = new Prolog1.Term("boy(X)");
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog1.Term("boy(A)");
			t2 = new Prolog1.Term("boy(X)");
			srcEnv = new HashMap<>();
			srcEnv.put("A", "alex");
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog1.Term("boy(alex)");
			t2 = new Prolog1.Term("boy(X)");
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			destEnv.put("X", "bill");
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);
		}
		catch(Exception e){e.printStackTrace();}
	}

	void testSearch(String fileName, String term)
	{
		trace=true;
		try
		{
			rules = new ArrayList<>();
			List<String> list = IOHelper.fileToList(fileName);
			for(String line:list)
			{
				line = line.trim();
				if(line.length()==0) continue;
				if(line.charAt(0)=='#') continue;

				if(line.charAt(line.length()-1)=='.')
					line = line.substring(0, line.length()-1);
				rules.add(new Rule(line));
			}

			System.out.println();
			Term t = new Term(term);
			search(t);
		}
		catch(Exception e){e.printStackTrace();}
	}

	public static void main(String[] args)
	{
		Prolog1 p = new Prolog1();
		p.testUnify();
		p.testSearch("python/test-java1.pl", "child(Q)");
	}
}
