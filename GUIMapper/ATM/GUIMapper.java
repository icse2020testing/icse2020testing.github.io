import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import util.Event;
import util.FiniteStateMachine;
import util.LemmatizationAndPOSTagger;
import util.Pair;
import util.State;
import util.StateAbstraction;
import util.uiautomator.BasicTreeNode;
import util.uiautomator.UiHierarchyXmlLoader;
import util.uiautomator.UiNode;

import com.opencsv.CSVWriter;


public class GUIMapper {
	
	private LemmatizationAndPOSTagger lemmatizationAndPOSTagger = new LemmatizationAndPOSTagger();
	private Map<String, Double> dictionary = new HashMap<String, Double>();
	 private OkHttpClient client = new OkHttpClient.Builder().authenticator(new Authenticator() {
	        public Request authenticate(Route route, Response response) throws IOException {
	            String credential = Credentials.basic("neo4j", "123456");
	            return response.request().newBuilder().header("Authorization", credential).build();
	        }
	    })
	    .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
	    .build();

	 /**
	  * args[0] srcAppDir ends with /
	  * args[1] tgtAppDir ends with /
	  * @param args
	  */
	public static void main(String[] args) {
		
		String srcAppDirStr = args[0];
		String tgtAppDirStr = args[1];
		
		String[] tmp = srcAppDirStr.split("/");
		String srcApp = tmp[tmp.length -2];
		tmp = tgtAppDirStr.split("/");
		String tgtApp = tmp[tmp.length -2];

		GUIMapper guiMapper = new GUIMapper();
		try {
			guiMapper.outputScores(srcAppDirStr, tgtAppDirStr, srcApp, tgtApp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		guiMapper.compareAllPairs("/Users/felicitia/Documents/Research/Android_Testing_Research/ATM/examples/App2.xml", 
//				"/Users/felicitia/Documents/Research/Android_Testing_Research/ATM/examples/App1.xml");
		
//		double score = guiMapper.compareTextWithLemmatization("sign up", "create account");
//		System.out.println(score);
		
	}
	
	private void compareAllPairs(String srcFile, String tgtFile){
		UiNode srcRootNode = getRoot(srcFile.toString());
		List<Pair<Event, List<Double>>> srcClickables = findClickables(srcRootNode, new ArrayList<Pair<Event, List<Double>>>(), false);
		System.out.println("srcClickables size = " + srcClickables.size() + "\tin file " + srcFile.toString());
		UiNode tgtRootNode = getRoot(tgtFile.toString());
		List<Pair<Event, List<Double>>> tgtClickables = findClickables(tgtRootNode, new ArrayList<Pair<Event, List<Double>>>(), false);
		System.out.println("tgtClickables size = " + tgtClickables.size() + "\tin file " + tgtFile.toString());
		for(Pair<Event, List<Double>> srcClickable: srcClickables){
				for(Pair<Event, List<Double>> tgtClickable: tgtClickables){
					double matchingScore = compareWidgets(srcClickable.first.getTargetElement(), tgtClickable.first.getTargetElement());
					if(matchingScore >= 0.4){
						System.out.println("src = " + srcClickable.first.getTargetElement());
						System.out.println("tgt = " + tgtClickable.first.getTargetElement());
						System.out.println("score = " + matchingScore);
						System.out.println();
					}
				}
			
		}
		
	}
	
	/**
	 * generate scoreMap with src/target UiNode pairs and corresponding similarity scores
	 * @param srcAppDirStr
	 * @param tgtAppDirStr
	 * @param srcApp
	 * @param tgtApp
	 * @throws IOException 
	 */
	private void outputScores(String srcAppDirStr, String tgtAppDirStr, String srcApp, String tgtApp) throws IOException{
//		Map<Pair<UiNode, UiNode>, Double> scoreMap = new HashMap<Pair<UiNode, UiNode>, Double>();

		File srcAppDir = new File(srcAppDirStr);
		File[] srcFiles = srcAppDir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".uix");
		    }
		});
		
		File tgtAppDir = new File(tgtAppDirStr);
		File[] tgtFiles = tgtAppDir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".uix");
		    }
		});
		for(File srcFile: srcFiles){
			UiNode srcRootNode = getRoot(srcFile.toString());
			List<Pair<Event, List<Double>>> srcClickables = findClickables(srcRootNode, new ArrayList<Pair<Event, List<Double>>>(), false);
			System.out.println("srcClickables size = " + srcClickables.size() + "\tin file " + srcFile.toString());
			for(File tgtFile: tgtFiles){
				UiNode tgtRootNode = getRoot(tgtFile.toString());
				List<Pair<Event, List<Double>>> tgtClickables = findClickables(tgtRootNode, new ArrayList<Pair<Event, List<Double>>>(), false);
				System.out.println("tgtClickables size = " + tgtClickables.size() + "\tin file " + tgtFile.toString());
				FileWriter writer = new FileWriter(srcApp+"_"+tgtApp+"_Scores_"+srcFile.getName()+"_"+tgtFile.getName()+".csv");
		        CSVWriter csvWriter = new CSVWriter(writer); 
				outputScoresPerAppPair(srcClickables, tgtClickables, csvWriter);
				csvWriter.flush();
				csvWriter.close();
			}
		}
		
//		 for (Map.Entry<Pair<UiNode, UiNode>, Double> entry : scoreMap.entrySet()) { 
//	            System.out.println("Key = " + entry.getKey() + 
//	                             ", Value = " + entry.getValue()); 
//	    } 
	}
	
	private void outputScoresPerAppPair(List<Pair<Event, List<Double>>> srcClickables, List<Pair<Event, List<Double>>> tgtClickables, CSVWriter csvWriter){
		
		for(Pair<Event, List<Double>> srcClickable: srcClickables){
			for(Pair<Event, List<Double>> tgtClickable: tgtClickables){
				UiNode srcNode = srcClickable.first.getTargetElement();
				UiNode tgtNode = tgtClickable.first.getTargetElement();
				double matchingScore = compareWidgets(srcNode, tgtNode);
				if(matchingScore >= 0.4) {
					List<String> line = new ArrayList<String>();
					line.add(srcNode.getAttribute("resource-id"));
					line.add(srcNode.getAttribute("class"));
					line.add(srcNode.getAttribute("bounds"));
					line.add(tgtNode.getAttribute("resource-id"));
					line.add(tgtNode.getAttribute("class"));
					line.add(tgtNode.getAttribute("bounds"));
					line.add(""+matchingScore);
					csvWriter.writeNext(line.toArray(new String[line.size()]));
				}
			}
		}
		csvWriter.writeNext(new String[]{"done"});
	}
	
	private double compareWidgets(UiNode sourceNode, UiNode targetNode){
		return findMatchedEvents(sourceNode, findTargetElementText(targetNode), findStateNodeTargetElementId(targetNode));
	}
	
    private double findMatchedEvents(UiNode sourceNode, String stateNodeTargetElementText, String stateNodeTargetElementId) {
        String eventTargetElementText = findTargetElementText(sourceNode);

        String eventTargetElementId = sourceNode.getAttribute("resource-id");
        if (eventTargetElementId != null && eventTargetElementId.contains("/"))
            eventTargetElementId = eventTargetElementId.split("/")[1];
        else eventTargetElementId = "";

        double textMatchingScore = 0;
        if (eventTargetElementText != null && stateNodeTargetElementText != null && !eventTargetElementText.equals("") && !stateNodeTargetElementText.equals("")) {
            textMatchingScore = compareTextWithLemmatization(eventTargetElementText.toLowerCase(), stateNodeTargetElementText.toLowerCase());
        }

        double idMatchingScore = 0;
        if (eventTargetElementId != null && stateNodeTargetElementId != null && !eventTargetElementId.equals("") && !stateNodeTargetElementId.equals("")) {
            idMatchingScore = compareTextWithLemmatization(eventTargetElementId.toLowerCase(), stateNodeTargetElementId.toLowerCase());
        }

        double textIdMatchingScore = 0;
        if (eventTargetElementText != null && stateNodeTargetElementId != null && !eventTargetElementText.equals("") && !stateNodeTargetElementId.equals("")) {
            textIdMatchingScore = compareTextWithLemmatization(eventTargetElementText.toLowerCase(), stateNodeTargetElementId.toLowerCase());
        }

        double idTextMatchingScore = 0;
        if (eventTargetElementId != null && stateNodeTargetElementText != null && !eventTargetElementId.equals("") && !stateNodeTargetElementText.equals("")) {
            idTextMatchingScore = compareTextWithLemmatization(eventTargetElementId.toLowerCase(), stateNodeTargetElementText.toLowerCase());
        }

        double matchingScore = Math.max(textMatchingScore, Math.max(idMatchingScore * 0.9, Math.max(textIdMatchingScore * 0.9, idTextMatchingScore * 0.9)));

        return matchingScore;
    }
	
    private String findTargetElementText(UiNode stateNodeTargetElement) {
        String stateNodeTargetElementText = stateNodeTargetElement.getAttribute("text");
        if (stateNodeTargetElementText == null || stateNodeTargetElementText.equals(""))
            stateNodeTargetElementText = stateNodeTargetElement.getAttribute("content-desc");
        if (stateNodeTargetElementText == null || stateNodeTargetElementText.equals(""))
            stateNodeTargetElementText = stateNodeTargetElement.getAttribute("hint");
        return stateNodeTargetElementText;
    }
    
    private String findStateNodeTargetElementId(UiNode stateNodeTargetElement) {
        String stateNodeTargetElementId = "";
        UiNode listViewParentNode = (UiNode)stateNodeTargetElement.getParent();
        while (listViewParentNode != null && (!listViewParentNode.getAttribute("class").equals("android.widget.ListView") &&
                !listViewParentNode.getAttribute("class").equals("android.support.v7.widget.RecyclerView") &&
                !listViewParentNode.getAttribute("class").equals("android.widget.ExpandableListView"))) {
            listViewParentNode = (UiNode) listViewParentNode.getParent();
        }
        if (listViewParentNode == null) {
            stateNodeTargetElementId = stateNodeTargetElement.getAttribute("resource-id");
            if (stateNodeTargetElementId != null && stateNodeTargetElementId.contains("/"))
                stateNodeTargetElementId = stateNodeTargetElementId.split("/")[1];
        }
        return stateNodeTargetElementId;
    }
    
	private double compareTextWithLemmatization(String currentStateLeafNodeText, String scenarioStateLeafNodeText) {
        if (!scenarioStateLeafNodeText.trim().equals("") && !currentStateLeafNodeText.trim().equals("") && scenarioStateLeafNodeText.trim().equals(currentStateLeafNodeText.trim())) {
            return 1.01 + (currentStateLeafNodeText.length() - currentStateLeafNodeText.replace(" ", "").length());
        }

        Map<Pair<String, String>, Double> scores_map = new HashMap<>();

        List<String> currentStateLeafNodeLemmatizedText = null;
        try {
            currentStateLeafNodeLemmatizedText = lemmatizationAndPOSTagger.getLemmatizedWord(filterWord(currentStateLeafNodeText));
        } catch (IOException e) {
            e.printStackTrace();
//            Log.e("Unable to get lemmatized word for " + currentStateLeafNodeText, e.getMessage());
        }
        List<String> scenarioStateLeafNodeLemmatizedText = null;
        try {
            scenarioStateLeafNodeLemmatizedText = lemmatizationAndPOSTagger.getLemmatizedWord(filterWord(scenarioStateLeafNodeText));
        } catch (IOException e) {
            e.printStackTrace();
//            Log.e("Unable to get lemmatized word for " + scenarioStateLeafNodeText, e.getMessage());
        }

        if (currentStateLeafNodeLemmatizedText != null && scenarioStateLeafNodeLemmatizedText != null) {
            int currentStateLeafNodeLemmatizedTextSize = currentStateLeafNodeLemmatizedText.size();

            int currentStateLeafNodeLemmatizedTextCount = 0;
            int numberOfMatchedTokens = 0;
            for (String currentStateText : currentStateLeafNodeLemmatizedText) {
                currentStateText = filterWord(currentStateText);
                if (currentStateText.equals(""))    continue;
                for (String scenarioStateText : scenarioStateLeafNodeLemmatizedText) {
                    scenarioStateText = filterWord(scenarioStateText);
                    if (scenarioStateText.equals(""))    continue;
                    String key = currentStateText + scenarioStateText;
                    double score = 0;
                    if (currentStateText.equals(scenarioStateText)) {
                        score = 1;
                    } else if (dictionary.containsKey(key)) {
                        score = dictionary.get(key);
                    } else {
                        try {
                            score = computeSimilarityScore(currentStateText, scenarioStateText);
                        } catch (IOException e) {
                            e.printStackTrace();
//                            Log.e("Unable to compute similarity score " + currentStateLeafNodeText, e.getMessage());
                        }
                        if (score < 0.4) {
                            double delta = stringDiff(filterWord(currentStateText), filterWord(scenarioStateText));
                            double sim = Math.max(filterWord(currentStateText).length(), filterWord(scenarioStateText).length()) - delta;
                            if ((sim / (sim + delta)) >= 0.8)   score = sim / (sim + delta);
                        }
                    }

                    dictionary.put(key, score);
                    if (score >= 0.4) {
                        numberOfMatchedTokens++;
                        scores_map.put(new Pair<>(currentStateText, scenarioStateText), score);
                    }
                }
                currentStateLeafNodeLemmatizedTextCount++;
            }

            double avgScores = 0;
            if (currentStateLeafNodeLemmatizedTextSize > 0 && scores_map.size() > 0)
                avgScores = getAvgScores(currentStateLeafNodeLemmatizedTextSize, scores_map).second;

            return avgScores;
        } else return 0;
    }

    private static String filterWord(String str) {
        if (str == null)    return "";

        str = str.toLowerCase();

        List<String> wordsToFilter = new ArrayList<String>() {{
            add("highlight");
            add("bar");
            add("action");
            add("input");
            add("content");
            add("white");
            add("black");
            add("autocomplete");
            add("circle");
            add("menu");
            add("-web");
            add("outline");
            add("button");
            add("sign");
            add("red");
            add("default");
            add("mode");
            add("arrow");
            add("editor");
            add("edittext");
            add("fab");
            add("grey");
            add("action");
            add("picker");
            add("text");
            add("title");
        }};

        List<String> additionalWordsToFilter = new ArrayList<String>() {{
            add("ic");
            add("dp");
            add("sp");
        }};
        additionalWordsToFilter.addAll(wordsToFilter);

        for (String wordToFilter : wordsToFilter) {
            if (str.contains(wordToFilter)) {
                int startIndex = str.indexOf(wordToFilter);
                int endIndex = str.length();
                int splitIndex = str.indexOf(" ", startIndex);
                if (str.indexOf("_", startIndex) > -1 && (splitIndex < 0 || str.indexOf("_", startIndex) < splitIndex))
                    splitIndex = str.indexOf("_", startIndex);
                if (splitIndex > -1)    endIndex = splitIndex + 1;
                str = str.replace(str.substring(startIndex, endIndex), "");
            }
        }

        StringBuilder strResult = new StringBuilder();
        String[] splittedStr = str.split("[_]");
        if (splittedStr.length > 0) {
            for (int i = 0; i < splittedStr.length; i++) {
                if (additionalWordsToFilter.contains(splittedStr[i])) {
                    if (i != 0 && i != splittedStr.length - 1) strResult.append(" ");
                } else {
                    if (i != splittedStr.length - 1) strResult.append(splittedStr[i]).append(" ");
                    else strResult.append(splittedStr[i]);
                }
            }
        } else if (additionalWordsToFilter.contains(str)) return "";

        String filteredWord = strResult.toString();

        try {
            double strNumber = Double.parseDouble(filteredWord);
            long iPart = (long) strNumber;
            double fPart = strNumber - iPart;
            if (fPart == 0) filteredWord = Long.toString(iPart);
        } catch (NumberFormatException e) {
            // do nothing
        }

        return filteredWord.replaceAll("\\s*-\\s*", "").replace("+", "").replace("$", "").replace("...", " ").replace(":", " ").replace("/", " ").replace("(", " ").replace(")", " ").toLowerCase();
    }
    

    private Double computeSimilarityScore(String vocab1, String vocab2) throws IOException {
    	Request request = new Request.Builder()
                .url("http://127.0.0.1:5000/")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"vocab1\": \"" + vocab1 + "\", \"vocab2\": \"" + vocab2 + "\"}"))
                .build();
        Response response = client.newCall(request).execute();
        String result = "";
        if (response.body() != null)    result = response.body().string();
        try {
            return Double.parseDouble(result);
        }
        catch(NumberFormatException e) {
            return 0.0;
        }
    }
    

    public static int stringDiff(CharSequence s, CharSequence t) {
        int n = s.length();
        int m = t.length();
        if (n == 0) return m;
        if (m == 0) return n;

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;

        for (int i = 1; i <= n; ++i) {
            char s_i = s.charAt(i - 1);
            for (int j = 1; j <= m; ++j) {
                char t_j = t.charAt(j - 1);
                int cost = (s_i == t_j ? 0 : 1);
                d[i][j] = min3(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
            }
        }

        return d[n][m];
    }
    
    private static int min3(int a, int b, int c) {
        if (b < a) a = b;
        if (c < a) a = c;
        return a;
    }
    
    private <T> Pair<Map<Pair<T, T>, Double>, Double> getAvgScores(int size, Map<Pair<T, T>, Double> scores_map) {
        Map<Pair<T, T>, Double> scores_map_sorted = sortScores(scores_map);

        List<T> alreadyPickedScenarioStateTexts = new ArrayList<>();
        List<T> alreadyPickedCurrentStateText = new ArrayList<>();
        Map<Pair<T, T>, Double> scores_map_highest_Score = new LinkedHashMap<>();
        int counter = 0;
        for (Pair<T, T> key : scores_map_sorted.keySet()) {
            T currentStateText = key.first;
            T scenarioStateText = key.second;

            if (!alreadyPickedScenarioStateTexts.contains(scenarioStateText) && !alreadyPickedCurrentStateText.contains(currentStateText) && counter < size) {
                scores_map_highest_Score.put(key, scores_map_sorted.get(key));

                alreadyPickedScenarioStateTexts.add(scenarioStateText);
                alreadyPickedCurrentStateText.add(currentStateText);

                counter++;
            }
        }

        double sumScores = 0;
        counter = 0;
        for (Pair<T, T> key : scores_map_highest_Score.keySet()) {
            if (scores_map_highest_Score.get(key) != 0 && scores_map_highest_Score.get(key) >= 0.4) {
                sumScores += scores_map_highest_Score.get(key);
                counter++;
            }
        }

        return new Pair<>(scores_map_highest_Score, sumScores);
    }
    
    private static <T> Map<T, Double> sortScores(Map<T, Double> map) {
        List<Entry<T, Double>> list = new LinkedList<>(map.entrySet());

        Collections.sort(list, new Comparator<Entry<T, Double>>() {
                public int compare(Entry<T, Double> o1,
                                   Entry<T, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
        });

        Map<T, Double> map_sorted = new LinkedHashMap<>();
        for (Entry<T, Double> entry : list) {
            map_sorted.put(entry.getKey(), entry.getValue());
        }

        return map_sorted;
    }
    
    private UiNode getRoot(String UIdumpFilePath) {
        String dumpWindowHierarchy = null;
        try {
            dumpWindowHierarchy = new String(Files.readAllBytes( Paths.get(UIdumpFilePath) ));
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**  skip updating UI hierarchy with image's filename b/c it requires source code
        if (id_image.size() > 0) {
            boolean needsImageTranslation = false;
            for (String key : id_image.keySet()) {
                if (dumpWindowHierarchy != null && dumpWindowHierarchy.contains(key)) {
                    needsImageTranslation = true;
                    break;
                }
            }

            if (needsImageTranslation) {
                ImageTranslator imageTranslator = new ImageTranslator(id_image);
                String updatedDumpWindowHierarchy = imageTranslator.updateHierarchyAttributes(dumpWindowHierarchy, System.getProperty("user.dir") + "/" + module + "/main/res");
                if (updatedDumpWindowHierarchy != null) {
                    dumpWindowHierarchy = updatedDumpWindowHierarchy;
                }
            }
        }
         **/
        UiHierarchyXmlLoader xmlLoader = new UiHierarchyXmlLoader();
        return (UiNode) xmlLoader.parseXml(dumpWindowHierarchy);
    }
    
    private State getCurrentState(FiniteStateMachine fsm, String UIdumpFilePath) {
        State currState;

        String dumpWindowHierarchy = null;
        try {
            dumpWindowHierarchy = new String(Files.readAllBytes( Paths.get(UIdumpFilePath) ));
        } catch (IOException e) {
            e.printStackTrace();
//            Log.e("Unable to dump window hierarchy", e.getMessage());
        }

        UiNode root = getRoot(UIdumpFilePath);
        updateCheckBoxText(root);

        StateAbstraction abs = new StateAbstraction();
        currState = new State(dumpWindowHierarchy, abs.computeFeatureVector(root), findClickables(root, new ArrayList<Pair<Event, List<Double>>>(), false)
        		/*getActionables(root)*/, null); //last parameter null was getCurrentActivitywithPackage()

        if (fsm.getState(currState) != null) {
            currState = fsm.getState(currState);
        }

        return currState;
    }
    
    private void updateCheckBoxText(UiNode root) {
        BasicTreeNode[] nodes = root.getChildren();

        for (int index = 0; index < nodes.length; index++) {
            UiNode node = (UiNode) nodes[index];

            String clazz = node.getAttribute("class");
            if (clazz.contains("CheckBox") || clazz.contains("Switch")) {
                StringBuilder textOrDesc = new StringBuilder();
                while (index + 1 < nodes.length) {
                    index++;

                    UiNode nextNode = (UiNode) nodes[index];
                    if (nextNode.getAttribute("text") != null)
                        textOrDesc.append(nextNode.getAttribute("text")).append(" ");
                    else if (nextNode.getAttribute("content-desc") != null)
                        textOrDesc.append(nextNode.getAttribute("content-desc")).append(" ");
                }
                int idx = index;
                while (idx - 1 >= 0) {
                    idx--;

                    UiNode prevNode = (UiNode) nodes[idx];
                    if (prevNode.getAttribute("text") != null)
                        textOrDesc.append(prevNode.getAttribute("text")).append(" ");
                    else if (prevNode.getAttribute("content-desc") != null)
                        textOrDesc.append(prevNode.getAttribute("content-desc")).append(" ");
                }

                node.addAtrribute("text", textOrDesc.toString());
            }

            updateCheckBoxText(node);
        }
    }
    
    private List<Pair<Event, List<Double>>> findClickables(UiNode root, List<Pair<Event, List<Double>>> clickables, boolean navigateUp) {
        if (root == null)   return new ArrayList<Pair<Event, List<Double>>>();

        BasicTreeNode[] nodes = root.getChildren();
        for (BasicTreeNode n : nodes) {
            UiNode node = (UiNode) n;

            String id = node.getAttribute("resource-id");
            String type = node.getAttribute("class");
            String clickable = node.getAttribute("clickable");
            String long_clickable = node.getAttribute("long-clickable");
            String checkable = node.getAttribute("checkable");
            String content_desc = node.getAttribute("content-desc");

            if (id.contains("com.android.systemui") || id.contains("statusBarBackground") || id.contains("navigationBarBackground"))    continue;

            Event testRecorderEvent = null;
            if (type.contains("EditText")) testRecorderEvent = new Event("VIEW_TEXT_CHANGED", node, "", "0");
            else if ((type.equals("android.support.v7.widget.RecyclerView") || type.equals("android.widget.ListView") || type.equals("android.widget.ExpandableListView")
                    || type.contains("RelativeLayout") || type.contains("LinearLayout") || type.contains("FrameLayout") || type.contains("Spinner")) && node.getChildCount() > 0) {
                List<Pair<UiNode, Boolean>> leafNodes = new ArrayList<>();
                findLeafNodes(node, leafNodes, false);

                for (Pair<UiNode, Boolean> leaf : leafNodes) {
                    String leaf_type = leaf.first.getAttribute("class");
                    if (leaf_type.contains("EditText")) testRecorderEvent = new Event("VIEW_TEXT_CHANGED", leaf.first, "", "0");
                    else if (type.equals("android.support.v7.widget.RecyclerView") || type.equals("android.widget.ListView")) {
                        if (!leaf.second) {
                            testRecorderEvent = new Event("LIST_ITEM_CLICKED", leaf.first, "", "0");
                        } else {
                            testRecorderEvent = new Event("LIST_ITEM_CLICKED/VIEW_LONG_CLICKED", leaf.first, "", "0");
                        }
                    } else {
                        if (clickable.equals("true") && long_clickable.equals("true"))
                            testRecorderEvent = new Event("VIEW_LONG_CLICKED/VIEW_CLICKED", leaf.first, "", "0");
                        else if (clickable.equals("true"))
                            testRecorderEvent = new Event("VIEW_CLICKED", leaf.first, "", "0");
                        else if (long_clickable.equals("true"))
                            testRecorderEvent = new Event("VIEW_LONG_CLICKED", leaf.first, "", "0");
                        else if (leaf.first.getAttribute("long-clickable").equals("true"))
                            testRecorderEvent = new Event("VIEW_LONG_CLICKED", leaf.first, "", "0");
                        else if (leaf.first.getAttribute("clickable").equals("true"))
                            testRecorderEvent = new Event("VIEW_CLICKED", leaf.first, "", "0");
                    }

                    if (testRecorderEvent != null && !alreadyContainsClickable(testRecorderEvent, clickables)) {
                        List<UiNode> webkitNodes = new ArrayList<>();
                        findWebkitAncestors(testRecorderEvent.getTargetElement(), webkitNodes);
                        if (webkitNodes.size() < 1) {
                            List<Double> indexes = new ArrayList<>();
                            clickables.add(new Pair<>(testRecorderEvent, indexes));
                        }
                    }
                }
            } else {
                if (long_clickable.equals("true") && clickable.equals("true"))
                    testRecorderEvent = new Event("VIEW_LONG_CLICKED/VIEW_CLICKED", node, "", "0");
                else if (long_clickable.equals("true") && clickable.equals("false"))
                    testRecorderEvent = new Event("VIEW_LONG_CLICKED", node, "", "0");
                else if (long_clickable.equals("false") && clickable.equals("true"))
                    testRecorderEvent = new Event("VIEW_CLICKED", node, "", "0");
                else if (checkable.equals("true"))
                    testRecorderEvent = new Event("VIEW_LONG_CLICKED", node, "", "0");
            }

            if (testRecorderEvent != null && !alreadyContainsClickable(testRecorderEvent, clickables)) {
                List<UiNode> webkitNodes = new ArrayList<>();
                findWebkitAncestors(testRecorderEvent.getTargetElement(), webkitNodes);
                if (webkitNodes.size() < 1) {
                    List<Double> indexes = new ArrayList<>();
                    switch (content_desc) {
                        case "Navigate up":
                            clickables.add(0, new Pair<>(testRecorderEvent, indexes));

                            navigateUp = true;
                            break;
                        case "More options":
                            if (navigateUp)
                                clickables.add(1, new Pair<>(testRecorderEvent, indexes));
                            else
                                clickables.add(0, new Pair<>(testRecorderEvent, indexes));
                            break;
                        default:
                            clickables.add(new Pair<>(testRecorderEvent, indexes));
                            break;
                    }
                }
            }

            findClickables(node, clickables, navigateUp);
        }

        return clickables;
    }
    
    private void findLeafNodes(UiNode node, List<Pair<UiNode, Boolean>> leafNodes, Boolean longClickable) {
        if (node.getChildCount() == 0 && !node.getAttribute("class").equals("android.support.v7.widget.RecyclerView"))
            leafNodes.add(new Pair<>(node, longClickable));

        for (BasicTreeNode leafNode : node.getChildren()) {
            if (((UiNode)leafNode).getAttribute("long-clickable").equals("true")) longClickable = true;

            findLeafNodes((UiNode) leafNode, leafNodes, longClickable);
        }
    }
    
    private boolean alreadyContainsClickable(Event event, List<Pair<Event, List<Double>>> clickables) {
        for (Pair<Event, List<Double>> c : clickables) {
            UiNode targetElement = c.first.getTargetElement();
            UiNode eventTargetElement = event.getTargetElement();
            if (targetElement == null || eventTargetElement == null)  continue;
            if (targetElement.toString().equals(eventTargetElement.toString())) return true;
        }

        return false;
    }
    
    private void findWebkitAncestors(UiNode node, List<UiNode> webkitNodes) {
        while (node != null) {
            if (node.getAttribute("class") != null && node.getAttribute("class").equals("android.webkit.WebView")) webkitNodes.add(node);

            node = (UiNode) node.getParent();
        }
    }
    
}
