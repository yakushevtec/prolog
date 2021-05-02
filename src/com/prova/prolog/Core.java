package com.prova.prolog;

import java.util.*;
import java.util.stream.*;

public class Core
{
	int goalId = 0;
	List<Rule> rules = new ArrayList<>();
	boolean trace = false;
	String indent = "";

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
		// pop will take item from end, insert(0,val) will push item onto queue
		Rule r = null;
		try
		{
			r = new Rule("got(goal):-x(y)"); // Anything- just get a rule object
		}
		catch(Exception e){e.printStackTrace();}
		Goal goal = new Goal(this, r, null, null, true);

		goal.rule.goals = new ArrayList<>();
		goal.rule.goals.add(term); // target is the single goal

		Deque<Goal> queue = new LinkedList<>(); // Start our search
		queue.add(goal);

		Set<String> ops = Stream.of("is", "cut", "fail", "<", "==").collect(Collectors.toSet());
		while(!queue.isEmpty())
		{
			Goal g = queue.remove(); // Next goal to consider
			if(trace) System.out.println("Deque "+g);

			if(g.inx >= g.rule.goals.size()) // Is this one finished?
			{
				if(g.parent==null) // Yes. Our original goal?
				{
					if(g.env.size()>0)
						System.out.println(g.env); // Yes. tell user we
					else
						System.out.println("Yes"); // have a solution
					continue;
				}

				Goal parent = g.parent.deepCopy(); // Otherwise resume parent goal
				unify(g.rule.head, g.env, parent.rule.goals.get(parent.inx), parent.env);
				parent.inx++; // advance to next goal in body
				queue.add(parent); // let it wait its turn
				if(trace) System.out.println("Queue "+parent);
				continue;
			}

			// No. more to do with this goal.
			term = g.rule.goals.get(g.inx); // What we want to solve

			String pred = term.pred; // Special term?
			if(ops.contains(pred))
			{
				if(pred.equals("is"))
				{
					Term ques = eval(term.args.get(0), g.env);
					Term ans = eval(term.args.get(1), g.env);
					if(ques==null)
						g.env.put(term.args.get(0).pred, ans); // Set variable
					else if(!ques.pred.equals(ans.pred))
						continue; // Mismatch, fail
				}
				else if(pred.equals("cut"))
					queue.clear(); // Zap the competition
				else if(pred.equals("fail"))
					continue; // Dont succeed
				else if(eval(term, g.env)==null)
					continue; // Fail if not true

				g.inx++;

				queue.offerFirst(g);
				continue;
			}

			for(Rule rule:rules) // Not special. Walk rule database
			{
				if(!rule.head.pred.equals(term.pred)) continue;
				if(rule.head.args.size() != term.args.size()) continue;
				Goal child = new Goal(this, rule, g, null, true); // A possible subgoal
				boolean ans = unify(term, g.env, rule.head, child.env);
				if(ans) // if unifies, stack it up
				{
					queue.add(child);
					if(trace) System.out.println("Queue "+child);
				}
			}
		}
	}

	Set<String> ops = Stream.of("+", "-", "*", "<", "==").collect(Collectors.toSet());
	Term eval(Term term, Map<String, Term> env) // eval all variables within a term to constants
	{
		if(ops.contains(term.pred))
		{
			Term t1 = eval(term.args.get(0), env);
			Term t2 = eval(term.args.get(1), env);
			return executeOp(term.pred, t1, t2);
		}

		if(isConstant(term))
			return term;
		if(isVariable(term))
		{
			Term ans = env.get(term.pred);
			if(ans==null)
				return null;
			else
				return eval(ans, env);
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

	Term executeOp(String op, Term t1, Term t2)
	{
		int n1 = Integer.parseInt(t1.pred);
		int n2 = Integer.parseInt(t2.pred);

		String res = null;
		if(op.equals("+")) res = ""+(n1 + n2);
		else if(op.equals("-")) res = ""+(n1 - n2);
		else if(op.equals("*")) res = ""+(n1 * n2);
		else if(op.equals("<")) res = ""+(n1 < n2);
		else if(op.equals("==")) res = ""+(n1 == n2);

		Term t = null;
		if(res!=null)
		try
		{
			t = new Term(res, new ArrayList<Term>());
		}
		catch(Exception e){e.printStackTrace();}
		return t;
	}

	static String[] split(String str, String sep, boolean all) // Split l by sep but honoring () and []
	{
		str = str==null ? "" : str.trim();
		int nest = 0;
		if(str.isEmpty())
			return new String[]{};
		int lSep = sep.length();
		for(int i=0; i<str.length()-lSep; i++)
		{
			if(nest <= 0 && str.substring(i, i+lSep).equals(sep))
	        {
	        	if(all)
	        	{
					String[] a1 = new String[]{str.substring(0, i)};
					String[] a2 = split(str.substring(i+lSep), sep, true);
					return Stream.concat(Arrays.stream(a1), Arrays.stream(a2)).toArray(String[]::new);
	        	}
				else
					return new String[]{str.substring(0, i), str.substring(i+lSep)};
	        }
			char c = str.charAt(i);
			if(c=='[' || c=='(') nest++;
			if(c==']' || c==')') nest--;
		}
		return new String[]{str};
	}

	static String[] infixOps = {" is ","==","<",">","+","-","*","/"};
	static Object[] splitInfix(String str)
	{
		for(String op:infixOps)
		{
			String[] parts = split(str, op, false);
			if(parts.length>1)
				return new Object[]{op, parts};
		}
		return null;
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

	static List<Term> deepCopyTerms(List<Term> list)
	{
		List<Term> res = new ArrayList<>();
		for(Term t:list)
			res.add(t.deepCopy());
		return res;
	}

	static Map<String, Term> deepCopyTerms(Map<String, Term> map)
	{
		Map<String, Term> res = new HashMap<>();
		for(String key:map.keySet())
		{
			Term t = map.get(key).deepCopy();
			res.put(key, t);
		}
		return res;
	}

	static void error(String msg) throws Exception
	{
		throw new Exception(msg);
	}
}
