package com.prova.prolog;

import java.util.*;

public class Term
{
	String pred;
	List<Term> args;

	Term(String str, List<Term> tArgs) throws Exception // expect "x(y,z...)"
	{
		if(str!=null)
			str = str.trim();

		Object[] parts = null;
		if(tArgs==null)
			parts = Core.splitInfix(str);

		if(tArgs!=null) // Predicate and args seperately
		{
			pred = str;
			args = tArgs;
		}
		else if(parts!=null)
		{
			pred = (String)parts[0];
			args = new ArrayList<Term>();
			for(String s:(String[])parts[1])
				args.add(new Term(s, null));
		}
		else if(str.charAt(str.length()-1) == ']') // Build list "term"
		{
			String[] flds = Core.split(str.substring(1, str.length()-1), ",", true);
			String[] flds2 = Core.split(str.substring(1, str.length()-1), "|", true);
			if(flds2.length>1)
			{
				this.pred = ".";
				args = new ArrayList<Term>();
				for(String s:flds2)
					args.add(new Term(s, null));
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
			String[] flds = Core.split(str, "(", false);
 			if(flds.length != 2)
 				Core.error("Syntax error in term: " + str);

			pred = flds[0];
			args = new ArrayList<Term>();
			for(String s:Core.split(flds[1].substring(0, flds[1].length()-1), ",", true))
				args.add(new Term(s, null));
		}
		else // Simple constant or variable
		{
			pred = str;
			args = new ArrayList<Term>();
		}
	}

	public String[] reverse(String[] arr)
	{
		String[] res = new String[arr.length];
		for(int i = 0; i < arr.length; i++)
		    res[i] = arr[arr.length - i - 1];
		return res;
	}

	public Term deepCopy()
	{
		Term res = null;
		List<Term> tArgs = Core.deepCopyTerms(args);
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
			if(args.size()==0)
				return "[]";
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
