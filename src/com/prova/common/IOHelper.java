package com.prova.common;

import java.util.*;
import java.io.*;

public class IOHelper
{
	static public List<String> fileToList(String fileName) throws IOException
	{
		List<String> res=new ArrayList<String>();
		FileInputStream fis = new FileInputStream(fileName);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null)
			res.add(line);
		return res;
	}
}
