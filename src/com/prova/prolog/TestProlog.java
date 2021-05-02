package com.prova.prolog;

import java.util.*;

import com.prova.common.*;

public class TestProlog
{
	Core core = new Core();

	void testUnify()
	{
		try
		{
			Term t = new Term("mother(alice,bill)", null);
			System.out.println("\n"+t);

			Rule r = new Rule("son(X,Y):-mother(Y,X),boy(X)");
			System.out.println("\n"+r);

			Term t1 = new Term("boy(bill)", null);
			Term t2 = new Term("boy(alex)", null);
			Map<String, Term> srcEnv = new HashMap<>();
			Map<String, Term> destEnv = new HashMap<>();
			boolean res = core.unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Term("boy(alex)", null);
			t2 = new Term("boy(alex)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = core.unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Term("boy(alex)", null);
			t2 = new Term("boy(X)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			res = core.unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Term("boy(A)", null);
			t2 = new Term("boy(X)", null);
			srcEnv = new HashMap<>();
			srcEnv.put("A", new Term("alex", null));
			destEnv = new HashMap<>();
			res = core.unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);

			t1 = new Term("boy(alex)", null);
			t2 = new Term("boy(X)", null);
			srcEnv = new HashMap<>();
			destEnv = new HashMap<>();
			destEnv.put("X", new Term("bill", null));
			res = core.unify(t1, srcEnv, t2, destEnv);
			System.out.println("\nt1="+t1+", t2="+t2+"\n\tsrc="+srcEnv+"\n\tdest="+destEnv+"\n\t"+res);
		}
		catch(Exception e){e.printStackTrace();}
	}

	void testSearch(String fileName, String term)
	{
		core.trace=true;
		try
		{
			core.rules = new ArrayList<>();
			List<String> list = IOHelper.fileToList(fileName);
			for(String line:list)
			{
				line = line.trim();
				if(line.length()==0) continue;
				if(line.charAt(0)=='#') continue;

				if(line.charAt(line.length()-1)=='.')
					line = line.substring(0, line.length()-1);
				core.rules.add(new Rule(line));
			}

			System.out.println();
			Term t = new Term(term, null);
			core.search(t);
		}
		catch(Exception e){e.printStackTrace();}
	}

	public static void main(String[] args)
	{
		TestProlog p = new TestProlog();
		p.testUnify();
		p.testSearch("python/test-java1.pl", "child(Q)");
		p.testSearch("python/test-java2.pl", "member(a,[a,b,c])");
		p.testSearch("python/test-java2.pl", "member(X,[a,b,c])");
		p.testSearch("python/test-java2.pl", "append([a,b],[c,d],X)");
		p.testSearch("python/test-java2.pl", "append([a,b],Y,[a,b,c,d])");
		p.testSearch("python/test-java2.pl", "append(X,Y,[a,b,c])");

		p.testSearch("python/test-java3.pl", "length([a,b,c],X)");
		p.testSearch("python/test-java3.pl", "length([a,b,c],3)");
		p.testSearch("python/test-java3.pl", "childOf(A,B)");
		p.testSearch("python/test-java3.pl", "childOfCut(A,B)");
	}
}
