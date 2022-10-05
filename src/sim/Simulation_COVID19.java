package sim;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;

import run.Run_Population_Remote_MetaPopulation_COVID19;
import util.PersonClassifier;
import util.PropValUtils;

/**
 * Define a set of simulation using properties file
 *
 * @author Ben Hui
 * @version 20221005
 *
 */
public class Simulation_COVID19 implements SimulationInterface {

	public static final String[] PROP_NAME_RMP = { "PROP_RMP_SIM_TYPE", "PROP_STORE_INFECTION_HISTORY",
			"PROP_STORE_TESTING_HISTORY", "PROP_STORE_TREATMENT_HISTORY", "PROP_RMP_OPT_TARGET",
			"PROP_RMP_OPT_WEIGHT", };
	public static final Class<?>[] PROP_CLASS_RMP = { Integer.class, // 0 = NG_CT, 1 = Syphilis
			Boolean.class, Boolean.class, Boolean.class, double[].class, double[].class, };

	public static final int PROP_RMP_SIM_TYPE = PROP_NAME.length;
	public static final int PROP_STORE_INFECTION_HISTORY = PROP_RMP_SIM_TYPE + 1;
	public static final int PROP_STORE_TESTING_HISTORY = PROP_STORE_INFECTION_HISTORY + 1;
	public static final int PROP_STORE_TREATMENT_HISTORY = PROP_STORE_TESTING_HISTORY + 1;
	public static final int PROP_RMP_OPT_TARGET = PROP_STORE_TREATMENT_HISTORY + 1;
	public static final int PROP_RMP_OPT_WEIGHT = PROP_RMP_OPT_TARGET + 1;

	public static final String POP_PROP_INIT_PREFIX = "POP_PROP_INIT_PREFIX_";
	protected String[] propModelInitStr = null;

	protected Object[] propVal = new Object[PROP_NAME.length + PROP_NAME_RMP.length];
	protected File baseDir = new File("");

	protected boolean stopNextTurn = false;
	protected String extraFlag = "";

	public void setExtraFlag(String extraFlag) {
		this.extraFlag = extraFlag;
	}

	@Override
	public void loadProperties(Properties prop) {
		for (int i = 0; i < PROP_NAME.length; i++) {
			String ent = prop.getProperty(PROP_NAME[i]);
			if (ent != null && ent.length() > 0) {
				propVal[i] = PropValUtils.propStrToObject(ent, PROP_CLASS[i]);
			}
		}
		for (int i = PROP_NAME.length; i < propVal.length; i++) {
			String ent = prop.getProperty(PROP_NAME_RMP[i - PROP_NAME.length]);
			if (ent != null && ent.length() > 0) {
				propVal[i] = PropValUtils.propStrToObject(ent, PROP_CLASS_RMP[i - PROP_NAME.length]);
			}
		}

		int maxFieldNum = 0;
		for (Iterator<Object> it = prop.keySet().iterator(); it.hasNext();) {
			String k = (String) it.next();
			if (k.startsWith(POP_PROP_INIT_PREFIX)) {
				if (prop.getProperty(k) != null) {
					maxFieldNum = Math.max(maxFieldNum, Integer.parseInt(k.substring(POP_PROP_INIT_PREFIX.length())));
				}
			}
		}

		if (maxFieldNum >= 0) {
			propModelInitStr = new String[maxFieldNum + 1];
			for (int i = 0; i < propModelInitStr.length; i++) {
				String res = prop.getProperty(POP_PROP_INIT_PREFIX + i);
				if (res != null && res.length() > 0) {
					propModelInitStr[i] = res;
				}
			}
		}
	}

	@Override
	public Properties generateProperties() {
		Properties prop = new Properties();
		for (int i = 0; i < PROP_NAME.length; i++) {
			prop.setProperty(PROP_NAME[i], PropValUtils.objectToPropStr(propVal[i], PROP_CLASS[i]));
		}
		for (int i = PROP_CLASS.length; i < propVal.length; i++) {
			prop.setProperty(PROP_NAME_RMP[i - PROP_NAME.length],
					PropValUtils.objectToPropStr(propVal[i], PROP_CLASS_RMP[i - PROP_CLASS.length]));
		}

		return prop;
	}

	@Override
	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	@Override
	public void setSnapshotSetting(PersonClassifier[] snapshotCountClassifier, boolean[] snapshotCountAccum) {
		throw new UnsupportedOperationException("Not supported in this version.");
	}

	@Override
	public void setStopNextTurn(boolean stopNextTurn) {
		throw new UnsupportedOperationException("Not supported in this version.");
	}

	@Override
	public void generateOneResultSet() throws IOException, InterruptedException {

		try {

			Run_Population_Remote_MetaPopulation_COVID19 run = new Run_Population_Remote_MetaPopulation_COVID19(baseDir,
					propVal, propModelInitStr);

			if ("population.Population_Remote_MetaPopulation_COVID19_AS".equals(propVal[PROP_POP_TYPE])) {
				run.setCOVID_Pop_Type(Run_Population_Remote_MetaPopulation_COVID19.COVID_POP_TYPE_AGE_STURCTURED);
			}

			run.setRemoveAfterZip(!extraFlag.contains("-noZipRemove"));

			if (extraFlag.contains("-clearPrevResult")) {
				if (baseDir.getName().equals("Test_Prop_Covid19")) {
					run.setClearPrevResult(true);
				} else {
					System.out.print("Clear previous result? Y to confirm: ");
					java.io.BufferedReader in = new java.io.BufferedReader(new InputStreamReader(System.in));
					if (in.readLine().equals("Y")) {
						run.setClearPrevResult(true);
					}
				}

			}

			run.generateOneResultSet();

		} catch (IOException | InterruptedException ex) {
			ex.printStackTrace(System.err);
		}
	}

	public static void main(String[] arg) throws IOException, InterruptedException, ClassNotFoundException {

		final String USAGE_INFO = String.format(
				"Usage: java %s PROP_FILE_DIRECTORY <...>",
				Simulation_COVID19.class.getName());
		if(arg.length == 0) {
			System.out.println(USAGE_INFO);
			System.exit(0);
		}
		File resultsDir = new File(arg[0]);
		File[] singleSimDir;
		File propFile = new File(resultsDir, Simulation_COVID19.FILENAME_PROP);

		if (!propFile.exists()) {
			System.out.println("Checking for result folder(s) at " + resultsDir);
			if (arg.length > 1) {
				ArrayList<File> dirList = new ArrayList<>();
				for (int i = 1; i < arg.length; i++) {
					if (!arg[i].startsWith("-")) {

						final Pattern regEx = Pattern.compile(arg[i]);

						File[] matchedDir = resultsDir.listFiles(new FileFilter() {
							@Override
							public boolean accept(File file) {
								return file.isDirectory() && regEx.matcher(file.getName()).matches()
										&& new File(file, Simulation_COVID19.FILENAME_PROP).exists();
							}
						});

						dirList.addAll(Arrays.asList(matchedDir));
					}
				}

				singleSimDir = dirList.toArray(new File[dirList.size()]);

			} else {
				singleSimDir = resultsDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return file.isDirectory()
								&& new File(file, Simulation_COVID19.FILENAME_PROP).exists();
					}
				});
			}
		} else {
			singleSimDir = new File[] { resultsDir };
		}
		System.out.println("# results set to be generated = " + singleSimDir.length);

		for (File singleSetDir : singleSimDir) {
			Simulation_COVID19 sim = new Simulation_COVID19();
			Path propFilePath = new File(singleSetDir, Simulation_COVID19.FILENAME_PROP).toPath();
			Properties prop;
			prop = new Properties();
			try (InputStream inStr = java.nio.file.Files.newInputStream(propFilePath)) {
				prop.loadFromXML(inStr);
			}
			sim.setBaseDir(singleSetDir);
			sim.loadProperties(prop);
			if (arg[arg.length - 1].startsWith("-")) {
				sim.setExtraFlag(arg[arg.length - 1]);
			}
			sim.generateOneResultSet();
			

		}

	}

}
