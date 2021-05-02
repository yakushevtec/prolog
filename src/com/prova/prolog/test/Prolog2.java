package com.prova.prolog.test;

import java.util.*;
import java.util.stream.Stream;

import com.prova.common.*;

public class Prolog2
{
	int goalId = 0;
	List<Rule> rules = new ArrayList<>();
	boolean trace = false;
	String indent = "";

	class Term
	{
		String pred;
		List<Term> args;

		Term(String str, List<Term> tArgs) throws Exception // expect "x(y,z...)"
		{
			if(str!=null)
				str = str.trim();

			if(tArgs!=null) // Predicate and args seperately
			{
				pred = str;
				args = tArgs;
			}
			else if(str.charAt(str.length()-1) == ']') // Build list "term"
			{
				String[] flds = split(str.substring(1, str.length()-1), ',', true);
				String[] flds2 = split(str.substring(1, str.length()-1), '|', true);
				if(flds2.length>1)
				{
					args = new ArrayList<Term>();
					for(String s:flds2)
						args.add(new Term(s, null));
					this.pred = ".";
				}
				else
				{
					flds = reverse(flds);
					Term t = new Term(".", new ArrayList<Term>());
					for(String f:flds)
					{
						List<Term> as = new ArrayList<Term>();
						as.add(new Term(f, null));
						as.add(t);
						t = new Term(".", as);
					}
					pred = t.pred;
					args = t.args;
				}
			}
			else if(str.charAt(str.length()-1) == ')') // Compile from "pred(a,b,c)" string
			{
				String[] flds = split(str, '(', false);
	 			if(flds.length != 2)
	 				new Exception("Syntax error in term: " + str);

				args = new ArrayList<Term>();
				for(String s:split(flds[1].substring(0, flds[1].length()-1), ',', true))
					args.add(new Term(s, null));
				pred = flds[0];
			}
			else // Simple constant or variable
			{
				pred = str;
				args = new ArrayList<Term>();
			}
		}

		public Term deepCopy()
		{
			Term res = null;
			List<Term> tArgs = deepCopyTerms(args);
			try
			{
				res = new Term(pred, tArgs);
			}
			catch(Exception e){e.printStackTrace();}
			return res;
		}

		public String toString()
		{
			if(pred.equals("."))
			{
				if(args.size()==0) return "[]";
				Term nxt = args.get(1);
				if(nxt.pred.equals(".") && nxt.args.size()==0)
					return "["+args.get(0).toString()+"]";
				else if(nxt.pred.equals("."))
				{
					String a1 = args.get(1).toString();
					return "["+args.get(0)+","+a1.substring(1, a1.length()-1)+"]";
				}
				else
					return "["+args.get(0)+"|"+args.get(1)+"]";
			}
			else if(args!=null && args.size()>0)
			{
				String res = "";
				for(Term t:args)
					res += res.length()>0 ? ","+t : t;
				return pred+"("+res+")";
			}
			else
				return pred;
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
			head = new Term(flds[0], null);
			if(flds.length == 2)
			{
				flds[1] = flds[1].replace("),", ")#");
				for(String s:flds[1].split("#"))
					goals.add(new Term(s, null));
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
			List<Term> tGoals = deepCopyTerms(goals);
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

	// A Goal is a rule in at a certain point in its computation. 
	// env contains definitions (so far), inx indexes the current term
	// being satisfied, parent is another Goal which spawned this one
	// and which we will unify back to when this Goal is complete.
	class Goal
	{
		int id;
		Goal parent;
		Rule rule;
		Map<String, Term> env;
		int inx = 0; // start search with 1st subgoal

		Goal(Rule rule, Goal parent, Map<String, Term> env, boolean changeID)
		{
			if(changeID) id = ++goalId;
			this.rule = rule;
			this.parent = parent;

			if(env==null) env = new HashMap<>();
			this.env = deepCopyTerms(env); // deep copy of env.
		}

		public String toString()
		{
			return "Goal "+id+" rule="+rule+" inx="+inx+" env="+env;
		}

		public Goal deepCopy()
		{
			Rule gRule = rule.deepCopy();

			Map<String, Term> gEnv = deepCopyTerms(env); // deep copy

			Goal gParent = parent!=null ? parent.deepCopy() : null;
			Goal res = new Goal(gRule, gParent, gEnv, false);
			res.id = id;
			res.inx = inx;
			return res;
		}
	}

	// update dest env from src. return true if unification succeeds.
	boolean unify(Term srcTerm, Map<String, Term> srcEnv, Term destTerm, Map<String, Term> destEnv)
	{
		if(trace) System.out.println(indent+"Unify "+srcTerm+" "+srcEnv+" to "+destTerm+" "+destEnv);
		indent += "  ";
		if(srcTerm.pred.equals("_") || destTerm.pred.equals("_"))
			return sts(true, "Wildcard");

		if(isVariable(srcTerm))
		{
			Term srcVal = eval(srcTerm, srcEnv);
			if(srcVal==null)
				return sts(true, "Src unset");
			else
				return sts(unify(srcVal, srcEnv, destTerm, destEnv), "Unify to Src Value");
		}

		if(isVariable(destTerm))
		{
			Term destVal = eval(destTerm, destEnv); // evaluate destination
			if(destVal!=null)
				return sts(unify(srcTerm, srcEnv, destVal, destEnv), "Unify to Dest value");
			else
			{
				destEnv.put(destTerm.pred, eval(srcTerm, srcEnv));
				return sts(true, "Dest updated 1"); // unifies. destination updated
			}
		}
	    else if(!srcTerm.pred.equals(destTerm.pred))
			return sts(false, "Diff predicates");
		else if(srcTerm.args.size()!=destTerm.args.size())
			return sts(false, "Diff # args");
		else
		{
			Map<String, Term> dde = deepCopyTerms(destEnv); // deepcopy

			for(int i=0; i<srcTerm.args.size(); i++)
			if(!unify(srcTerm.args.get(i), srcEnv, destTerm.args.get(i), dde))
				return sts(false, "Arg doesn't unify");
			for(String key:dde.keySet())
				destEnv.put(key, dde.get(key));
        	return sts(true, "All args unify");
		}
	}

	void search(Term term)
	{
		goalId = 0;
		if(trace) System.out.println("search "+term);

		Rule r = null;
		try
		{
			r = new Rule("got(goal):-x(y)"); // Anything- just get a rule object
		}
		catch(Exception e){e.printStackTrace();}
		Goal goal = new Goal(r, null, null, true);

		goal.rule.goals = new ArrayList<>();
		goal.rule.goals.add(term); // target is the single goal

		Queue<Goal> queue = new LinkedList<>(); // Start our search
		queue.add(goal);

		while(!queue.isEmpty())
		{
			Goal c = queue.remove(); // Next goal to consider
			if(trace) System.out.println("Deque "+c);

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
				if(trace) System.out.println("Queue "+parent);
				queue.add(parent); // let it wait its turn
				continue;
			}

			// No. more to do with this goal.
			term = c.rule.goals.get(c.inx); // What we want to solve
			for(Rule rule:rules) // Walk down the rule database
			{
				if(!rule.head.pred.equals(term.pred)) continue;
				if(rule.head.args.size() != term.args.size()) continue;
				Goal child = new Goal(rule, c, null, true); // A possible subgoal
				boolean ans = unify(term, c.env, rule.head, child.env);
				if(ans) // if unifies, queue it up
				{
					if(trace) System.out.println("Queue "+child);
					queue.add(child);
				}
			}
		}
	}

	Term eval(Term term, Map<String, Term> env) // eval all variables within a term to constants
	{
		if(isConstant(term))
			return term;
		if(isVariable(term))
		{
			Term ans = env.get(term.pred);
			if(ans==null)
				return null;
			else return eval(ans, env);
		}
		List<Term> args = new ArrayList<>();
		for(Term arg:term.args)
		{ 
			Term t = eval(arg, env);
			if(t==null)
				return null;
			args.add(t);
		}
		Term t = null;
		try
		{
			t = new Term(term.pred, args);
		}
		catch(Exception e){e.printStackTrace();}
		return t;
	}

	String[] split(String str, char sep, boolean all) // Split l by sep but honoring () and []
	{
		str = str==null ? "" : str.trim();
		int nest = 0;
		if(str.isEmpty())
			return new String[]{};
		for(int i=0; i<str.length(); i++)
		{
			char c = str.charAt(i);
			if(nest<=0 && c==sep)
			if(all)
			{
				String[] a1 = new String[]{str.substring(0, i)};
				String[] a2 = split(str.substring(i+1), sep, true);
				return Stream.concat(Arrays.stream(a1), Arrays.stream(a2)).toArray(String[]::new);
			}
			else
				return new String[]{str.substring(0, i), str.substring(i+1)};
			if(c=='[' || c=='(') nest++;
			if(c==']' || c==')') nest--;
		}
		return new String[]{str};
	}

	boolean sts(boolean ok, String why)
	{
		indent = indent.substring(0, indent.length()-2);
		if(trace) System.out.println(indent+(ok ? "Yes" : "No")+" "+why);
		return ok;
	}

	boolean isVariable(Term term)
	{
		return term.args.size()==0 && Character.isUpperCase(term.pred.charAt(0));
	}

	boolean isConstant(Term term)
	{
		return term.args.size()==0 && !Character.isUpperCase(term.pred.charAt(0));
	}

	List<Term> deepCopyTerms(List<Term> list)
	{
		List<Term> res = new ArrayList<>();
		for(Term t:list)
			res.add(t.deepCopy());
		return res;
	}

	Map<String, Term> deepCopyTerms(Map<String, Term> map)
	{
		Map<String, Term> res = new HashMap<>();
		for(String key:map.keySet())
		{
			Term t = map.get(key).deepCopy();
			res.put(key, t);
		}
		return res;
	}

	public String[] reverse(String[] arr)
	{
		String[] res = new String[arr.length];
		for(int i = 0; i < arr.length; i++)
		    res[i] = arr[arr.length - i - 1];
		return res;
	}

	void testUnify()
	{
		try
		{
			Term t = new Prolog2.Term("mother(alice,bill)", null);
			System.out.println("\n"+t);

			Rule r = new Rule("son(X,Y):-mother(Y,X),boy(X)");
			System.out.println("\n"+r);

			Term t1 = new Prolog2.Term("boy(bill)", null);
			Term t2 = new Prolog2.Term("boy(alex)", null);
			Map<String, Term> srcEnv = new HashMap<>();
			Map<String, Term> destEnv = new HashMap<>();
			boolean res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog2.Term("boy(alex)", null);
			t2 = new Prolog2.Term("boy(alex)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog2.Term("boy(alex)", null);
			t2 = new Prolog2.Term("boy(X)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog2.Term("boy(A)", null);
			t2 = new Prolog2.Term("boy(X)", null);
			srcEnv = new HashMap<>();
			srcEnv.put("A", new Prolog2.Term("alex", null));
			destEnv = new HashMap<>();
			res = unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Prolog2.Term("boy(alex)", null);
			t2 = new Prolog2.Term("boy(X)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			destEnv.put("X", new Prolog2.Term("bill", null));
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
			Term t = new Term(term, null);
			search(t);
		}
		catch(Exception e){e.printStackTrace();}
	}

	public static void main(String[] args)
	{
		Prolog2 p = new Prolog2();
		p.testUnify();
		p.testSearch("python/test-java1.pl", "child(Q)");
		p.testSearch("python/test-java2.pl", "member(a,[a,b,c])");
		p.testSearch("python/test-java2.pl", "member(X,[a,b,c])");
		p.testSearch("python/test-java2.pl", "append([a,b],[c,d],X)");
		p.testSearch("python/test-java2.pl", "append([a,b],Y,[a,b,c,d])");
		p.testSearch("python/test-java2.pl", "append(X,Y,[a,b,c])");
	}
}
