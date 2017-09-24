
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import static org.apache.commons.io.FileUtils.readFileToString;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.at4j.comp.bzip2.BZip2EncoderExecutorService;
import org.at4j.comp.bzip2.BZip2OutputStream;
import org.at4j.comp.bzip2.BZip2OutputStreamSettings;



public class Prioritization {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

            List<Test> tests = new ArrayList<Test>();
            List<Test> prioritizedTests = null;
            double dist[][] = null;

            // Load the test cases
            switch (args[0]) {
                case "AB": 
                	for (int i = 1; i <= countLines(args[1]); i++) {
                        Test t = new Test(i);
                        loadTestGcov(t, args[2] + "test" + i);
                        tests.add(t);
                    }
                    break;
                case "ASS":
                    List<String> statements = loadSpanningStatements(args[3]);
                    for (int i = 1; i <= countLines(args[1]); i++) {
                        Test t = new Test(i);
                        loadTestGcovSpanningStatements(t, args[2] + "test" + i, statements);
                        tests.add(t);
                    }
                    break;
                case "ASB":
                    List<String> branches = loadSpanningBranches(args[3]);
                    for (int i = 1; i <= countLines(args[1]); i++) {
                        Test t = new Test(i);
                        loadTestGcovSpanningBranches(t, args[2] + "test" + i, branches);
                        tests.add(t);
                    }
                    break;

                case "t-W":
                	String line;
                    for (int i = 1; i <= countLines(args[1]); i++) {
                        Test t = new Test(i);
                        tests.add(t);

                    }

                    HashMap<HashSet<Integer>, Integer> mapCasaTests = new HashMap<>();

                    BufferedReader in = new BufferedReader(new FileReader(args[2]));

                    ArrayList<Set<Integer>> casaConfs = new ArrayList<>();
                    HashSet<HashSet<Integer>> tsets = new HashSet<>();
                    HashMap<HashSet<Integer>, HashSet<HashSet<Integer>>> mapTestTSets = new HashMap<>();

                    int strength = Integer.parseInt(args[3]); // t

                    int i = 0;
                    while ((line = in.readLine()) != null) {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        HashSet<Integer> conf = new HashSet<Integer>();
                        while (st.hasMoreTokens()) {
                            conf.add(Integer.parseInt(st.nextToken()));
                        }
                        casaConfs.add(conf);
                        mapCasaTests.put(conf, tests.get(i).getId());
                        HashSet<HashSet<Integer>> tsetsConf = getTSets(strength, conf);
                        mapTestTSets.put(conf, tsetsConf);
                        tsets.addAll(tsetsConf);
                        i++;

                    }
                    in.close();

                    Collections.shuffle(casaConfs);
                    ArrayList<Set<Integer>> prio = globalPrioConf(casaConfs, tsets, strength, mapTestTSets);
                    prioritizedTests = new ArrayList<>();

                    for (Set<Integer> conf : prio) {
                        prioritizedTests.add(new Test(mapCasaTests.get(conf)));
                    }

                    break;

                case "IMD":

                    int nTests = countLines(args[1]);
                    for (int n = 1; n <= nTests; n++) {
                        Test t = new Test(n);
                        tests.add(t);
                    }

                    in = new BufferedReader(new FileReader(args[2]));
                    casaConfs = new ArrayList<>();
                    while ((line = in.readLine()) != null) {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        Set<Integer> conf = new HashSet<Integer>();
                        while (st.hasMoreTokens()) {
                            conf.add(Integer.parseInt(st.nextToken()));
                        }
                        casaConfs.add(conf);

                    }
                    in.close();

                    // Jaccard distances between the model inputs
                    dist = new double[nTests][nTests];

                    for (i = 0; i < nTests; i++) {
                        for (int j = 0; j < nTests; j++) {
                            if (j > i) {
                                double d = getJaccardDistance(casaConfs.get(i), casaConfs.get(j));
                                dist[i][j] = d;
                            }

                        }
                    }
                    break;
                case "I-TSD":
                    nTests = countLines(args[1]);
                    for (int n = 1; n <= nTests; n++) {
                        Test t = new Test(n);
                        tests.add(t);
                    }

                    List<InOut> inputs_ = new ArrayList<>();
                    i = 1;

                    in = new BufferedReader(new FileReader(args[1]));
                    while ((line = in.readLine()) != null) {
                        inputs_.add(new InOut(i, line, compress(line)));
                        i++;

                    }
                    in.close();

                    Collections.shuffle(inputs_);
                    List<Integer> prioritized = TSDmPrioritization(inputs_);

                    prioritizedTests = new ArrayList<>();
                    for (Integer i_ : prioritized) {
                        prioritizedTests.add(new Test(i_));
                    }
                    break;

                default:
                    printUsage();
                    break;
            }

            // Shuffle the test cases
            Collections.shuffle(tests);

            //Prioritize if not done before
            switch (args[0]) {
                case "AB":
                    prioritizedTests = prioritizeAdditional(tests, "branch");
                    break;
                case "ASS":
                    prioritizedTests = prioritizeAdditional(tests, "statement");
                    break;
                case "ASB":
                    prioritizedTests = prioritizeAdditional(tests, "branch");
                    break;
                case "t-W":
                case "I-TSD":
                    // already prioritized
                    break;
                case "IMD":
                    prioritizedTests = globalMaxDist(tests, dist, 1);
                    break;
                default:
                    break;
            }

            //Print the prioritized test suite
            for (Test test : prioritizedTests) {
                System.out.println(test.getId());
            }

        } catch (Exception ex) {
            printUsage();
            System.out.println("");
            ex.printStackTrace();
        }
    }

    public static void printUsage() {
        System.out.println("-----------------------------WB---------------------------------------");
        System.out.println("Usage: java -jar Prioritization.jar AB testSuiteFile gcovFilesDir");
        System.out.println("Usage: java -jar Prioritization.jar ASS testSuiteFile gcovFilesDir spanningStatementsFile");
        System.out.println("Usage: java -jar Prioritization.jar ASB testSuiteFile gcovFilesDir spanningBranchesFile");
        System.out.println("-----------------------------BB---------------------------------------");
        System.out.println("Usage: java -jar Prioritization.jar t-W testSuiteFile casaTestSuiteFile t");
        System.out.println("Usage: java -jar Prioritization.jar IMD testSuiteFile modelMutantsMatrixFile");
        System.out.println("Usage: java -jar Prioritization.jar I-TSD testSuiteFile");
    }

    public static void loadTestGcov(Test test, String dir) {
        Set<String> methodsCovered = new HashSet<String>();
        Set<String> instrCovered = new HashSet<String>();
        Set<String> branchCovered = new HashSet<String>();

        File[] files = new File(dir).listFiles();
        if (files.length > 0) {
            for (File f : files) {
                try {

                    String fname = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("/") + 1);

                    BufferedReader in = new BufferedReader(new FileReader(f));

                    String line;
                    int lineN = 1;
                    while ((line = in.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("branch")) {
                            if (line.contains("taken ") && !line.contains("taken 0")) {
                                branchCovered.add(fname + ":branch" + lineN);
                            }

                        } else if (line.startsWith("function")) {

                            if (!line.contains("called 0")) {
                                methodsCovered.add(fname + ":method" + lineN);
                            }

                        } else if (!line.isEmpty() && !line.startsWith("-") && !line.startsWith("funct") && !line.startsWith("branch") && !line.startsWith("call")) {

                            if (!line.startsWith("#") && !line.startsWith("0")) {
                                instrCovered.add(fname + ":instr" + lineN);
                            }
                        }
                        lineN++;
                    }
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }

        test.setMethods(methodsCovered);
        test.setBranch(branchCovered);
        test.setStatements(instrCovered);
    }

    public static List<Test> prioritizeAdditional(List<Test> tests, String type) {
        List<Test> prioritized = new ArrayList<Test>();
        List<Test> testsCopy = new ArrayList<Test>(tests);

        Set<String> covered = new HashSet<String>();

        while (!testsCopy.isEmpty()) {

            int max = -1;
            int toAdd = -1;

            for (int i = 0; i < testsCopy.size(); i++) {

                Set<String> coveredcp = new HashSet<String>(covered);
                switch (type) {
                    case "branch":
                        coveredcp.addAll(testsCopy.get(i).getBranch());
                        break;
                    case "statement":
                        coveredcp.addAll(testsCopy.get(i).getStatements());
                        break;
                    case "method":
                    case "mutants": //also mutant, i.e., the methods object in Test.java is also sued for storing mutants
                        coveredcp.addAll(testsCopy.get(i).getMethods());
                        break;
                    default:
                        break;
                }

                if (coveredcp.size() > max) {
                    toAdd = i;
                    max = coveredcp.size();
                }
            }
            switch (type) {
                case "branch":
                    covered.addAll(testsCopy.get(toAdd).getBranch());
                    break;
                case "statement":
                    covered.addAll(testsCopy.get(toAdd).getStatements());
                    break;
                case "method":
                    covered.addAll(testsCopy.get(toAdd).getMethods());
                    break;
                default:
                    break;
            }
            prioritized.add(testsCopy.get(toAdd));
            testsCopy.remove(toAdd);

        }

        return prioritized;
    }

    public static List<Test> prioritizeTotal(List<Test> tests, String type) {
        List<Test> prioritized = new ArrayList<Test>();
        List<Test> testsCopy = new ArrayList<Test>(tests);

        while (!testsCopy.isEmpty()) {

            int max = -1;
            int toAdd = -1;

            for (int i = 0; i < testsCopy.size(); i++) {

                int size = -1;
                switch (type) {
                    case "branch":
                        size = testsCopy.get(i).getBranch().size();
                        break;
                    case "statement":
                        size = testsCopy.get(i).getStatements().size();
                        break;
                    case "method":
                        size = testsCopy.get(i).getMethods().size();
                        break;
                    default:
                        break;
                }

                if (size > max) {
                    max = size;
                    toAdd = i;
                }
            }
            prioritized.add(testsCopy.get(toAdd));
            testsCopy.remove(toAdd);
        }
        return prioritized;

    }

    public static int countLines(String file) {
        int totalNumberOfLines = 0;
        try {

            LineNumberReader lineReader = new LineNumberReader(new FileReader(Paths
                    .get(file).toFile()));
            lineReader.skip(Long.MAX_VALUE);

            totalNumberOfLines = lineReader.getLineNumber();

            lineReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return totalNumberOfLines;
    }

    private static List<String> loadSpanningBranches(String file) {
        BufferedReader in = null;
        List<String> branches = new ArrayList<String>();
        try {

            in = new BufferedReader(new FileReader(new File(file)));
            String line;
            while ((line = in.readLine()) != null) {
                branches.add(line.trim());
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return branches;
    }

    private static List<String> loadSpanningStatements(String file) {
        BufferedReader in = null;
        List<String> statements = new ArrayList<String>();
        try {

            in = new BufferedReader(new FileReader(new File(file)));
            String line;
            while ((line = in.readLine()) != null) {
                statements.add(line.trim());
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return statements;
    }

    public static void loadTestGcovSpanningBranches(Test test, String dir, List<String> branches) {

        Set<String> branchCovered = new HashSet<String>();

        File[] files = new File(dir).listFiles();
        if (files.length > 0) {
            for (File f : files) {
                try {

                    String fname = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("/") + 1);

                    BufferedReader in = new BufferedReader(new FileReader(f));

                    String line;
                    int lineN = 1;
                    while ((line = in.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("branch")) {

                            if (line.contains("taken ") && !line.contains("taken 0")) {

                                String b = fname + ":branch" + lineN;
                                if (branches.contains(b)) {
                                    branchCovered.add(b);
                                }

                            }

                        }
                        lineN++;
                    }
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        test.setBranch(branchCovered);

    }

    public static void loadTestGcovSpanningStatements(Test test, String dir, List<String> statements) {

        Set<String> instrCovered = new HashSet<String>();
        File[] files = new File(dir).listFiles();
        if (files.length > 0) {
            for (File f : files) {
                try {

                    String fname = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf("/") + 1);
                    BufferedReader in = new BufferedReader(new FileReader(f));

                    String line;
                    int lineN = 1;
                    while ((line = in.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("-") && !line.startsWith("funct") && !line.startsWith("branch") && !line.startsWith("call")) {
                            if (!line.startsWith("#") && !line.startsWith("0")) {

                                String s = fname + ":instr" + lineN;

                                if (statements.contains(s)) {
                                    instrCovered.add(s);
                                }

                            }
                        }
                        lineN++;
                    }
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        test.setStatements(instrCovered);

    }

    public static Set<Integer> testStringtoInt(Set<String> test, Set<String> allbs, Map<String, Integer> bsmap) {

        Set<Integer> testi = new HashSet<Integer>();

        for (String s : allbs) {
            if (!test.contains(s)) {
                testi.add(bsmap.get(s));
            } else {
                testi.add(-bsmap.get(s));
            }
        }
        return testi;
    }

    public static double getSetBasedDistance(Set<Integer> p1, Set<Integer> p2, double weight) {
        Set<Integer> intersection = new HashSet<Integer>(p1);
        Set<Integer> union = new HashSet<Integer>(p1);
        intersection.retainAll(p2);
        union.addAll(p2);
        double intersectionSize = intersection.size();
        double unionSize = union.size();

        return 1.0 - (intersectionSize / (intersectionSize + weight * (unionSize - intersectionSize)));
    }

    public static double getJaccardDistance(Set<Integer> p1, Set<Integer> p2) {
        return getSetBasedDistance(p1, p2, 1.0);
    }

    public static List<Test> globalMaxDist(List<Test> tests, double[][] distancesMatrix, int offset) {

        Random rand = new Random();

        List<Integer> possibleIndices = new ArrayList<>();
        List<Integer> doneIndices = new ArrayList<>();

        List<Test> prioritizedTests = new ArrayList<>();

        for (int i = 0; i < tests.size(); i++) {
            possibleIndices.add(i);

        }

        int size = tests.size();

        double maxDistance = -1;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (j > i) {

                    double d = distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    if (d > maxDistance) {
                        maxDistance = d;
                    }
                }
            }
        }

        List<Integer> candidateIndicesI = new ArrayList<>();
        List<Integer> candidateIndicesJ = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (j > i) {

                    double d = distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    if (d == maxDistance) {
                        candidateIndicesI.add(i);
                        candidateIndicesJ.add(j);
                    }
                }
            }
        }

        int r = rand.nextInt(candidateIndicesI.size());
        int toAddIIndex = candidateIndicesI.get(r);
        int toAddJIndex = candidateIndicesJ.get(r);

        Test test1 = tests.get(toAddIIndex);
        Test test2 = tests.get(toAddJIndex);

        prioritizedTests.add(test1);
        prioritizedTests.add(test2);

        possibleIndices.remove((Integer) toAddIIndex);
        possibleIndices.remove((Integer) toAddJIndex);
        doneIndices.add(toAddIIndex);
        doneIndices.add(toAddJIndex);

        while (!possibleIndices.isEmpty()) {

            if (possibleIndices.size() > 1) {
                double maxDist = -1;

//                List<Integer> candidateIndices = new ArrayList<>();
                HashMap<Double, ArrayList<Integer>> mapDistIndices = new HashMap<>();

                for (Integer i : possibleIndices) {

                    double distance = 0;
                    for (Integer j : doneIndices) {
                        distance += distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    }

                    if (mapDistIndices.get(distance) == null) {
                        mapDistIndices.put(distance, new ArrayList<Integer>());
                    }

                    mapDistIndices.get(distance).add(i);

                    if (distance > maxDist) {
                        maxDist = distance;
                    }
                }

                int toAdd = mapDistIndices.get(maxDist).get(rand.nextInt(mapDistIndices.get(maxDist).size()));

                Test test = tests.get(toAdd);

                prioritizedTests.add(test);

                possibleIndices.remove((Integer) toAdd);
                doneIndices.add(toAdd);

            } else {
                prioritizedTests.add(tests.get(possibleIndices.get(0)));
                possibleIndices.clear();
            }
        }
        return prioritizedTests;
    }

    public static HashSet<HashSet<Integer>> getTSets(int t, HashSet<Integer> conf) {

        HashSet<HashSet<Integer>> tsets = new HashSet<HashSet<Integer>>();
        List<Integer> a = new ArrayList<Integer>(conf);

        int size = a.size();

        double total = getBinomCoeff(size, t);

        for (int i = 0; i < total; i++) {
            tsets.add(getITSet(size, t, i, a, total));

        }

        return tsets;

    }

    public static double getBinomCoeff(int n, int k) {
        if (k > n) {
            return 0.0;
        } else if (n == k || k == 0) {
            return 1.0;
        } else {
            return ArithmeticUtils.binomialCoefficient(n, k);
        }
    }

    public static HashSet getITSet(int n, int k, double m, List<Integer> featuresList, double total) {

        //double total = getBinomCoeff(n, k);
        if (m >= total) {
            m = total - 1.0;
        }
        HashSet tSet = new HashSet();
        int a = n;
        int b = k;
        double x = (total - 1.0) - m;  // x is the "dual" of m

        for (int i = 0; i < k; i++) {
            a = largestV(a, b, x);          // largest value v, where v < a and vCb < x
            x = x - getBinomCoeff(a, b);
            b = b - 1;
            tSet.add(featuresList.get(n - 1 - a));
        }

        return tSet;
    }

    public static int largestV(int a, int b, double x) {
        int v = a - 1;

        while (getBinomCoeff(v, b) > x) {
            v--;
        }

        return v;
    }

    public static ArrayList<Set<Integer>> globalPrioConf(ArrayList<Set<Integer>> confs, HashSet<HashSet<Integer>> tsets, int t, HashMap<HashSet<Integer>, HashSet<HashSet<Integer>>> mapTestTSets) {

        Random rand = new Random();

        int size = confs.size();

        ArrayList<Set<Integer>> prioritizedConfs = new ArrayList<Set<Integer>>(size);

        List<Integer> possibleIndices = new ArrayList<Integer>();
        List<Integer> doneIndices = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            possibleIndices.add(i);

        }

        int toAddIndex = possibleIndices.get(rand.nextInt(possibleIndices.size()));

        Set<Integer> conf = confs.get(toAddIndex);

        prioritizedConfs.add(conf);
        //confsCopy.remove(conf);
        tsets.removeAll(mapTestTSets.get(conf));//getTSets(t, conf));

        possibleIndices.remove((Integer) toAddIndex);
        doneIndices.add(toAddIndex);

        while (!possibleIndices.isEmpty()) {

            if (possibleIndices.size() > 1) {
                double maxCovered = -1;

                HashMap<Double, ArrayList<Integer>> mapCoveredIndices = new HashMap<>();

                //List<Integer> candidatesIndices = new ArrayList<Integer>();
                int toAdd = -1;
                for (Integer i : possibleIndices) {

                    HashSet<HashSet<Integer>> tsetsCopy = new HashSet<>(tsets);
                    int prevSize = tsetsCopy.size();
                    double covered = 0;

                    Set<Integer> c = confs.get(i);
                    tsetsCopy.removeAll(mapTestTSets.get(c));//getTSets(t, confs.get(i)));

                    covered = prevSize - tsetsCopy.size();

                    if (mapCoveredIndices.get(covered) == null) {
                        mapCoveredIndices.put(covered, new ArrayList<Integer>());
                    }

                    mapCoveredIndices.get(covered).add(i);
                    if (covered > maxCovered) {
                        maxCovered = covered;
                    }
                }

                toAdd = mapCoveredIndices.get(maxCovered).get(rand.nextInt(mapCoveredIndices.get(maxCovered).size()));

                Set<Integer> c = confs.get(toAdd);

                prioritizedConfs.add(c);

                tsets.removeAll(mapTestTSets.get(c));

                possibleIndices.remove((Integer) toAdd);
                doneIndices.add(toAdd);

            } else {
                prioritizedConfs.add(confs.get(possibleIndices.get(0)));
                possibleIndices.clear();
            }
        }

        return prioritizedConfs;
    }

    public static int compress(String str) throws Exception {

        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final BZip2CompressorOutputStream os = new BZip2CompressorOutputStream(bout);
        os.write(str.getBytes());
        os.finish();
        final byte[] compressed = bout.toByteArray();
        os.close();
        return compressed.length;
    }

    public static double ncd(String x, String y, int cx, int cy) throws Exception {

        if (x.equals(y)) {
            return 0;
        }

        int cxy = compress(x + y);

        return (cxy - (double) Math.min(cx, cy)) / Math.max(cx, cy);
    }

    public static List<Test> globalMinDist(List<Test> tests, double[][] distancesMatrix, int offset) {

        Random rand = new Random();

        List<Integer> possibleIndices = new ArrayList<>();
        List<Integer> doneIndices = new ArrayList<>();

        List<Test> prioritizedTests = new ArrayList<>();

        for (int i = 0; i < tests.size(); i++) {
            possibleIndices.add(i);

        }

        int size = tests.size();

        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (j > i) {

                    double d = distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    if (d < minDistance) {
                        minDistance = d;
                    }
                }
            }
        }

        List<Integer> candidateIndicesI = new ArrayList<>();
        List<Integer> candidateIndicesJ = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (j > i) {

                    double d = distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    if (d == minDistance) {
                        candidateIndicesI.add(i);
                        candidateIndicesJ.add(j);
                    }
                }
            }
        }

        int r = rand.nextInt(candidateIndicesI.size());
        int toAddIIndex = candidateIndicesI.get(r);
        int toAddJIndex = candidateIndicesJ.get(r);

        Test test1 = tests.get(toAddIIndex);
        Test test2 = tests.get(toAddJIndex);

        prioritizedTests.add(test1);
        prioritizedTests.add(test2);

        possibleIndices.remove((Integer) toAddIIndex);
        possibleIndices.remove((Integer) toAddJIndex);
        doneIndices.add(toAddIIndex);
        doneIndices.add(toAddJIndex);

        while (!possibleIndices.isEmpty()) {

            if (possibleIndices.size() > 1) {
                double minDist = Double.MAX_VALUE;

                HashMap<Double, ArrayList<Integer>> mapDistIndices = new HashMap<>();

                for (Integer i : possibleIndices) {

                    double distance = 0;
                    for (Integer j : doneIndices) {
                        distance += distancesMatrix[tests.get(i).getId() - offset][tests.get(j).getId() - offset];
                    }

                    if (mapDistIndices.get(distance) == null) {
                        mapDistIndices.put(distance, new ArrayList<Integer>());
                    }

                    mapDistIndices.get(distance).add(i);

                    if (distance < minDist) {
                        minDist = distance;
                    }
                }

                int toAdd = mapDistIndices.get(minDist).get(rand.nextInt(mapDistIndices.get(minDist).size()));

                Test test = tests.get(toAdd);

                prioritizedTests.add(test);

                possibleIndices.remove((Integer) toAdd);
                doneIndices.add(toAdd);

            } else {
                prioritizedTests.add(tests.get(possibleIndices.get(0)));
                possibleIndices.clear();
            }
        }
        return prioritizedTests;
    }

    public static int getLevenshteinDistance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];

    }

    public static List<Integer> TSDmPrioritization(List<InOut> inouts) throws Exception {

        List<Integer> prioritized = new ArrayList<Integer>();
        List<InOut> inoutsCp = new ArrayList<InOut>(inouts);

        BZip2EncoderExecutorService executor
                = BZip2OutputStream.createExecutorService(8);
        BZip2OutputStreamSettings settings = new BZip2OutputStreamSettings().
                setExecutorService(executor);

        while (!inoutsCp.isEmpty()) {

            int maxIndex = 0;
            int maxCompress = 0;

            for (int i = 0; i < inoutsCp.size(); i++) {

                int c = compressExcept(inoutsCp, i, settings);

                if (c > maxCompress) {
                    maxCompress = c;
                    maxIndex = i;
                }
            }

            prioritized.add(inoutsCp.get(maxIndex).getId());
            inoutsCp.remove(maxIndex);

        }
        executor.shutdown();
        return prioritized;

    }

    public static int compressExcept(List<InOut> inouts, int except, BZip2OutputStreamSettings settings) throws Exception {

        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (InOut inout : inouts) {

            if (i != except) {

                sb.append(inout.getInout());

            }
            i++;
        }

        return compress(sb.toString(), settings);
    }

    public static int compress(String str, BZip2OutputStreamSettings settings) throws Exception {

        final ByteArrayOutputStream bout = new ByteArrayOutputStream();

        OutputStream bzos = new BZip2OutputStream(bout, settings);

        byte[] b = str.getBytes();

        bzos.write(b);
        bzos.close();

        return bout.size();//compressed.length;
    }

}