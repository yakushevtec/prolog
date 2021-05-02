package com.prova.prolog;

import java.util.*;

public class Goal
{
	Core core;
	int id;
	Goal parent;
	Rule rule;
	Map<String, Term> env;
	int inx = 0; // start search with 1st subgoal

	Goal(Core core, Rule rule, Goal parent, Map<String, Term> env, boolean changeID)
	{
		this.core = core;
		if(changeID) id = ++core.goalId;
		this.rule = rule;
		this.parent = parent;

		if(env==null)
			env = new HashMap<>();
		this.env = Core.deepCopyTerms(env); // deep copy of env.
	}

	public String toString()
	{
		return "Goal "+id+" rule="+rule+" inx="+inx+" env="+env;
	}

	public Goal deepCopy()
	{
		Rule gRule = rule.deepCopy();

		Map<String, Term> gEnv = Core.deepCopyTerms(env);

		Goal gParent = parent!=null ? parent.deepCopy() : null;
		Goal res = new Goal(core, gRule, gParent, gEnv, false);
		res.id = id;
		res.inx = inx;
		return res;
	}
}
