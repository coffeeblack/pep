/*
 * $Id: Pep.java 3114 2012-02-12 19:59:59Z scott $ 
 * Copyright (C) 2007 Scott Martin
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version. The GNU Lesser General Public License is
 * distributed with this software in the file COPYING.
 */
package edu.osu.ling.pep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Command line front end for {@link EarleyParser Earley parsers}.
 * <p>
 * In addition to tokenizing input strings and loading the {@link Grammar} that
 * a parser will use, Pep also a {@link ParserListener listens} for events
 * from the Earley parsers it invokes. An example invocation might be something
 * like:
 * 	<ol>
 * 		<li>
 * 			<code>pep -g samples/miniscule.xml -s S the boy left -v</code>
 * 		</li>
 * 		<li>
 * 			<code>echo "the boy left" | pep -g samples/miniscule.xml -v 2 -s S -
 * 			</code>
 * 		</li>
 *  </ol>
 * 	</blockquote>
 * The above commands cause Pep to create an Earley parser using the
 * grammar specified in the file <code>samples/miniscule.xml</code> for the
 * {@link Parse#getSeed() seed category} <code>S</code> and the input 
 * string &quot;<code>the boy left</code>&quot;. Example (2) shows how to 
 * configure Pep to read input from the standard input stream. For either of
 * these commands, Pep prints out the result of the parse as well as all
 * {@link ParseTree parse trees} for the specified string. 
 * </p>
 * @author <a href="http://www.ling.osu.edu/~scott/">Scott Martin</a>
 * @version $LastChangedRevision: 3114 $
 * @see EarleyParser
 */
public class Pep implements ParserListener {
	static final float VERSION = 0.4f;
	static final int V_ALL = 0, V_RECOGNITION = 0, V_WARN = 1, V_PARSE = 1,
		V_CHART = 2, V_GRAMMAR = 3, V_STATS = 3, V_DEBUG = 3;
	
	static int verbosity = 0;
	
	EarleyParser earleyParser;
	Map<ParserOption, Boolean> parserOptions;
	long lastParseStart;
	
	/**
	 * Do not allow this class to be instantiated except by its own
	 * {@link Pep#main(String[]) main} method.
	 */
	Pep(Map<ParserOption, Boolean> parserOptions) {
		this.parserOptions = parserOptions;
	}
	
	/**
	 * Parses a string (list of tokens) using the specified grammar and
	 * seed category.
	 * @param grammar The grammar to use in parsing.
	 * @param tokens The string to parse.
	 * @param seed The category to seed the parser with.
	 */
	Parse parse(Grammar grammar, List<String> tokens, Category seed) 
			throws PepException {
		if(earleyParser == null) {
			earleyParser = new EarleyParser(grammar, this);
			
			if(parserOptions != null) { // configure parser if options present
				for(Map.Entry<ParserOption, Boolean> entry
						: parserOptions.entrySet()) {
					earleyParser.setOption(entry.getKey(), entry.getValue());
				}
			}
		}
		
		if(Pep.verbosity >= Pep.V_STATS) {
			Pep.printMessage("Parsing " + tokens + " for category " + seed,
				Pep.V_STATS);
		}
		
		return earleyParser.parse(tokens, seed);
	}
	
	/**
	 * Consumes events generated when options are set on the parser.
	 */
	public void optionSet(ParserOptionEvent optionEvent) {
		if(Pep.verbosity >= Pep.V_DEBUG) {
			Pep.printMessage("Option set: " + optionEvent.option.name() + "="
					+ optionEvent.value, Pep.V_DEBUG);
		}
	}

	/**
	 * Consumes events generated when the parser is seeded.
	 */
	public void parserSeeded(EdgeEvent edgeEvent) {
		Pep.printParser(edgeEvent.index, "seed     ", edgeEvent.edge);		
	}

	/**
	 * Consumes events generated when edges are added to the parser's chart
	 * because of completion.
	 */
	public void edgeCompleted(EdgeEvent edgeEvent) {
		Pep.printParser(edgeEvent.index, "complete", edgeEvent.edge);
	}

	/**
	 * Consumes events generated when edges are added to the parser's chart
	 * because of prediction.
	 */
	public void edgePredicted(EdgeEvent edgeEvent) {
		Pep.printParser(edgeEvent.index, "predict ", edgeEvent.edge);
	}

	/**
	 * Consumes events generated when the parser scans a token from the input 
	 * string.
	 */
	public void edgeScanned(EdgeEvent edgeEvent) {
		Pep.printParser(edgeEvent.index, "scan    ", edgeEvent.edge);
	}
	
	/**
	 * Consumes events generated when the parser completes a parse.
	 */
	public void parseComplete(ParseEvent parseEvent) {
		long now = System.currentTimeMillis();
		Parse parse = parseEvent.parse;
		
		Pep.print("", Pep.V_CHART); // only if other output above
		Pep.print(parse, Pep.V_RECOGNITION);
		
		if(Pep.verbosity >= Pep.V_PARSE) {
			Set<ParseTree> parseTrees = parse.getParseTrees();
			if(!parseTrees.isEmpty()) {
				int count = 1;
				for(ParseTree pt : parseTrees) {
					Pep.print(count++ + ". " + pt.toString(), Pep.V_PARSE);
				}
			}
		}
		
		if(Pep.verbosity >= Pep.V_STATS) {
			Pep.printMessage("", Pep.V_STATS);
			Pep.printMessage("Parse complete: " + parse.chart.countEdges()
					+ " edges added to chart in " + (now - lastParseStart)
					+ " ms", Pep.V_STATS);
		}
	}

	public void parseMessage(ParseEvent parseEvent, String message) {
		Pep.printParser(parseEvent.index, "message ", message);		
	}

	public void parseError(ParseErrorEvent parseErrorEvent)
			throws PepException {
		Pep.printParser(parseErrorEvent.index, "error   ",
				parseErrorEvent.cause);
	}

	/**
	 * Prints an object to System.out.
	 * @param line The object to print out.
	 * @param requiredVerbosity The required verbosity level for this 
	 * message to actually be printed out.
	 */
	private static void print(Object line, int requiredVerbosity) {
		if(Pep.verbosity >= requiredVerbosity) {
			System.out.println(line);
		}
	}
	
	/**
	 * Prints an object to System.err.
	 * @param line The object to print out.
	 * @param requiredVerbosity The required verbosity level for this 
	 * message to actually be printed out.
	 */
	static void printMessage(String message, int requiredVerbosity) {
		if(Pep.verbosity >= requiredVerbosity) {
			System.err.println(message);
		}
	}
	
	/**
	 * Prints an error message to System.err.
	 */
	private static void printError(String msg) {
		Pep.printMessage("Error: " + msg, Pep.V_ALL);
	}
	
	/**
	 * Prints a throwable.
	 * @param error The throwable that was intercepted.
	 * @see #printError(String)
	 */
	private static void printError(Throwable error) {
		if(error instanceof SAXParseException) {
			SAXParseException spe = (SAXParseException)error;
			Pep.printError("line " + spe.getLineNumber() + ": "
					+ spe.getMessage());
		}
		else {
			String msg = error.getMessage();
			Throwable cause = error.getCause();
			if(cause != null && !cause.equals(error)) {
				msg += ": " + cause.getMessage();
			}
			
			Pep.printError(msg);
		}
	}
	
	/**
	 * Prints a warning string.
	 * @see #print(Object, int)
	 */
	static void printWarning(String msg) {
		Pep.printMessage("Warning: " + msg, Pep.V_WARN);
	}
	
	/**
	 * Prints usage information for the Pep executable.
	 */
	private static void printHelp(Options options) {
		String name = Pep.class.getSimpleName();
		Pep.printMessage(name + " is an Earley Parser, version " + Pep.VERSION,
				Pep.V_ALL);
		
		Pep.printMessage("", Pep.V_ALL);
		Pep.printMessage(name
				+ " is free software, copyright (C) 2007 Scott Martin.", 
				Pep.V_ALL);
		Pep.printMessage("See the COPYING file for details.", Pep.V_ALL);
		
		Pep.printMessage("", Pep.V_ALL);
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("pep [options] [string] (use - for stdin)",
				options);
		
		Pep.printMessage("", Pep.V_ALL);
		Pep.printMessage("where the default OPTION=value pairs are:",
				Pep.V_ALL);
		
		for(ParserOption option : ParserOption.values()) {
			Pep.printMessage(' ' + option.toString(), Pep.V_ALL);
		}
	}
	
	/**
	 * Auxiliary method used by event handlers to print out information about
	 * what the parser is doing in a readable format.
	 */
	private static void printParser(int index, String label, Object obj) {
		if(Pep.verbosity >= Pep.V_CHART) {
			StringBuilder sb = new StringBuilder(index + ": " + label + "\t");
			if(obj instanceof String) {
				sb.append('\'');
				sb.append(obj);
				sb.append('\'');
			}
			else if(obj instanceof Throwable) {
				sb.append(((Throwable)obj).getMessage());
			}
			else {
				sb.append(obj);
			}
			
			Pep.print(sb.toString(), Pep.V_CHART);
		}
	}

	/**
	 * Invokes Pep from the command line.
	 * <p>
	 * The main work this method does, apart from tokenizing the arguments and
	 * input tokens, is to load and parse the XML grammar file (as specified
	 * by <code>-g</code> or <code>--grammar</code>). If any of the arguments
	 * <code>-g</code>, <code>--grammar</code>, <code>-s</code>,
	 * <code>--seed</code>, <code>-o</code>, <code>--option</code>, occur
	 * with no argument following, this method prints an error notifying the
	 * user. 
	 * @param args The expected arguments are as follows, and can occur in
	 * any particular order:
	 * <ul>
	 * 	<li><code>-g|--grammar &lt;grammar file&gt;</code></li>
	 * 	<li><code>-s|--seed &lt;seed category&gt;</code></li>
	 * 	<li><code>-v|--verbose {verbosity level}</code></li>
	 * 	<li><code>-o|--option &lt;OPTION_NAME=value&gt;</code></li>
	 * 	<li><code>-h|--help (prints usage information)</code></li>
	 *	<li><code>&lt;token1 ... token<em>n</em>&gt;</code>
	 * 		(or <code>-</code> for standard input)</li>
	 * </ul>
	 * <code>OPTION_NAME</code> must be the name of one of the recognized
	 * {@link ParserOption options}. If <code>-h</code> or
	 * <code>--help</code> occur anywhere in the arguments, usage information
	 * is printed and no parsing takes place.
	 */
	@SuppressWarnings("static-access")
	public static final void main(String[] args) {
		try {
			Options opts = new Options();
			
			opts.addOption(OptionBuilder.withLongOpt("grammar")
					.withDescription("the grammar to use")
					.hasArg().isRequired().withArgName("grammar file")
					.create('g'));
			
			opts.addOption(OptionBuilder.withLongOpt("seed")
					.withDescription("the seed category to parse for")
					.hasArg().isRequired().withArgName("seed category")
					.create('s'));
			
			opts.addOption(OptionBuilder.withLongOpt("verbose")
					.withDescription("0-3")
					.hasOptionalArg().withArgName("verbosity level")
					.create('v'));
			
			opts.addOption(OptionBuilder.withLongOpt("option")
					.withDescription("sets parser options")
					.withArgName("OPTION=value")
					.hasArgs(2)
					.withValueSeparator()
					.withDescription("use value for given property")
					.create( "o" ));			
			
			opts.addOption(OptionBuilder.withLongOpt("help")
					.withDescription("prints this message")
					.create('h'));
			
			CommandLineParser parser = new GnuParser();
		    try {
		        CommandLine line = parser.parse(opts, args);
		        if(line.hasOption('h')) {
		        	Pep.printHelp(opts);
		        }
		        else {
		        	int v = Integer.parseInt(line.getOptionValue('v', 
		        		Integer.toString(Pep.V_PARSE)));
		        	if(v < 0) {
						throw new PepException("verbosity < 0: " + v);
					}
					
					Pep.verbosity = v;
					Map<ParserOption, Boolean> options
						= new EnumMap<ParserOption, Boolean>(
							ParserOption.class);
					
					Properties props = line.getOptionProperties("o");
					for(Object key : props.keySet()) {
						try {
							options.put(
								ParserOption.valueOf(key.toString()),
								Boolean.valueOf(props.get(key).toString()));
						}
						catch(IllegalArgumentException iae) {
							Pep.printError("no option named " + key.toString());
							Pep.printHelp(opts);
							return;
						}
					}
					
					Pep pep = new Pep(options);
					Grammar grammar = new GrammarParser(
						Pep.findGrammar(line.getOptionValue('g'))).parse();
					
					List<?> ts = line.getArgList();
					List<String> tokens = null;
					if(ts.isEmpty() || ts.get(0).equals("-")) {
						tokens = Pep.readTokens(new Scanner(System.in));
					}
					else {
						tokens = new ArrayList<String>(ts.size());
						for(Object t : ts) {
							tokens.add(t.toString());
						}
					}
					
					pep.lastParseStart = System.currentTimeMillis();
					try {
						pep.parse(grammar, tokens,
							new Category(line.getOptionValue('s')));
					}
					catch(PepException ignore) {
						// ignore here, we're listening
					}
		        }
		    }
		    catch(ParseException pe) {
		        Pep.printError("command-line syntax problem: "
		        		+ pe.getMessage());
		        Pep.printHelp(opts);
		    }
		}
		catch(PepException pe) {
			Throwable cause = pe.getCause();
			Pep.printError((cause == null) ? pe : cause);
		}
		catch(RuntimeException re) {
			Pep.printError(re);
		}
	}
	
	/**
	 * Locates the grammar file as specified on the command line. Attempts to
	 * find the file based on the current directory.
	 * @param grammarLoc The grammar location string specified on the command
	 * line.
	 * @return The located file.
	 * @throws PepException If the file does not exist or is a directory.
	 */
	private static final File findGrammar(String grammarLoc)
			throws PepException {
		File g = new File(System.getProperty("user.dir"), grammarLoc);
		
		if(!g.exists()) {
			throw new PepException("grammar file does not exist");
		}
		if(!g.isFile()) {
			throw new PepException(
					"specified grammar is not a file");
		}
		
		return g;
	}
	
	/**
	 * Tokenizes the string input that occurs on the command line, removing
	 * &quot; characters.
	 * @param args The strings that occurred after <code>-t</code> or
	 * <code>--tokens</code> on the command line.
	 * @return A list of tokens suitable for use in parsing.
	 */
	private static final List<String> readTokens(Iterator<String> args) {
		List<String> tokens = new ArrayList<String>();
		Pattern q = Pattern.compile("\"");
		
		while(args.hasNext()) {
			String t = args.next();
			if(t == null) { // let parser deal with this
				tokens.add(t);
			}
			else {
				if(tokens.isEmpty()) { // it's the first token
					if(t.startsWith("\"")) {
						// strip off leading quote
						tokens.add(q.matcher(t).replaceAll(""));
					}
					else {
						tokens.add(t);
					}
				}
				else {
					if(t.endsWith("\"")) { // it's the last token
						// strip off trailing quote
						tokens.add(q.matcher(t).replaceAll(""));
						break; // done parsing
					}
					
					tokens.add(t); // no quotes to strip
				}
			}
		}
		
		return tokens;
	}
	
	// TODO make GrammarParser public (usable by users)
	
	/**
	 * Parses XML grammar files. 
	 */
	static class GrammarParser implements ErrorHandler {
		File grammarFile;
		DocumentBuilder documentBuilder;
		
		/**
		 * Create a grammar parser for the specified file.
		 */
		GrammarParser(File grammarFile) throws PepException {
			this.grammarFile = grammarFile;
			
			try {
				documentBuilder
					= DocumentBuilderFactory.newInstance().newDocumentBuilder();
				documentBuilder.setErrorHandler(this);
			}
			catch(ParserConfigurationException pce) {
				throw new PepException("problem instantiating parser", pce);
			}
		}
		
		/**
		 * Parse the XML file specified when this grammar parser was created,
		 * returning a grammar.
		 */
		Grammar parse() throws PepException {
			Grammar g = null;
			
			try {
				Document d = documentBuilder.parse(grammarFile);
				// validate
				SchemaFactory sf = SchemaFactory.newInstance(
					XMLConstants.W3C_XML_SCHEMA_NS_URI);
				StreamSource src = new StreamSource(
					getClass().getClassLoader().getResourceAsStream(
							"META-INF/etc/grammar.xsd"));
				
				if(src.getInputStream() == null) {
					Pep.printMessage("Unable to locate grammar.xsd", Pep.V_ALL);
				}
				else {
					Schema schema = sf.newSchema(src);
					Validator vlad = schema.newValidator();
					
					try {
						vlad.validate(new DOMSource(d));
					}
					catch(SAXException se) {
						throw new PepException("invalid grammar", se);
					}
				}
				
				Element root = d.getDocumentElement();
				
				g = new Grammar(root.getAttribute("name"));
				Pep.printMessage("Loading grammar " + g.name + " from "
						+ grammarFile.getPath(), Pep.V_GRAMMAR);
				
				NodeList rules = root.getElementsByTagName("rule");
				Element ruleEl;
				Category left;
				List<Category> right;
				for(int i = 0; i < rules.getLength(); i++) {
					ruleEl = (Element)rules.item(i);
					left = new Category(ruleEl.getAttribute("category"));
					
					NodeList rightList = ruleEl.getChildNodes();
					right = new ArrayList<Category>(rightList.getLength());
					
					Node rightNode;
					Element rightEl;
					for(int j = 0; j < rightList.getLength(); j++) {
						rightNode = rightList.item(j);
						if(rightNode instanceof Element) {
							rightEl = (Element)rightNode;
							Attr termAttr
								= rightEl.getAttributeNode("terminal");
							
							right.add(new Category(rightEl.getAttribute("name"), 
								(termAttr != null && termAttr.getTextContent()
									.equals("true"))));
						}
					}
				
					g.addRule(new Rule(left,
							right.toArray(new Category[right.size()])));
				}
			}
			catch(IllegalArgumentException iae) {
				throw new PepException("problem loading grammar", iae);
			}
			catch(SAXException se) {
				throw new PepException("problem parsing", se);
			}
			catch(IOException io) {
				throw new PepException("problem reading grammar", io);
			}
			
			Pep.printMessage(g.toString(), Pep.V_GRAMMAR);
			Pep.printMessage("", Pep.V_GRAMMAR);
			
			return g;
		}
		
		/**
		 * Does nothing because an exception are generated. Present for
		 * binary compatibility with ErrorHandler.
		 */
		public void error(SAXParseException e) throws SAXException {
			// do nothing
		}
	
		/**
		 * Does nothing because an exception are generated. Present for
		 * binary compatibility with ErrorHandler.
		 */
		public void fatalError(SAXParseException e) throws SAXException {
			// do nothing
		}
	
		/**
		 * Event handler for warnings that occur during parsing. Prints a
		 * warning message to System.err.
		 * @see Pep#printWarning(String)
		 */
		public void warning(SAXParseException e) throws SAXException {
			Pep.printWarning("line " + e.getLineNumber() + ": "
				+ e.getMessage());
		}
	}
	
}
