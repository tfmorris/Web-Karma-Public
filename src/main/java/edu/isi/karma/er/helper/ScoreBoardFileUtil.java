package edu.isi.karma.er.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import edu.isi.karma.er.helper.entity.MultiScore;
import edu.isi.karma.er.helper.entity.ResultRecord;
import edu.isi.karma.er.helper.entity.Score;
import edu.isi.karma.er.helper.entity.ScoreBoard;

public class ScoreBoardFileUtil {
	
	private double threshold = 0.9; 
	
	private String scoreBoardFile = Constants.PATH_SCORE_BOARD_FILE + "score_board.csv";
	
	private Logger log = Logger.getRootLogger();
	/**
	 * Load data of score template from scoreboard file.
	 * @return map of pair which saam uri as the key and scoreboard object as the value.
	 */
	public Map<String, ScoreBoard> loadScoreBoard() {
		Map<String, ScoreBoard> sMap = new HashMap<String, ScoreBoard>();
		File file = new File(scoreBoardFile);
		
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("the file input " + file.getAbsolutePath() + " does not exist actually.");
		}
		
		RandomAccessFile raf = null;
		
		
		try {
			String str = null;
			raf = new java.io.RandomAccessFile(file, "rw");
			
			
			while ((str = raf.readLine()) != null) {			// read a text line each time from ratio file
				String[] lines = split(str);
				
				if (lines.length >= 4) {						// 2 elements separated by ':' for a text line.
					ScoreBoard sb = new ScoreBoard();
					sb.setSubject(lines[0]);
					sb.setSaamUri(lines[1]);
					sb.setWikiUri(lines[2]);
					sb.setDbpediaUri(lines[3]);
					sMap.put(lines[0], sb);
				}
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return sMap;
	}
	
	public void write2Log(Map<String, ScoreBoard> map, JSONArray confArr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		write2Log(map, Constants.PATH_SCORE_BOARD_FILE + "result" + sdf.format(new Date()) + ".csv", confArr);
	}
	
	public void write2Log(Map<String, ScoreBoard> map, String logFile, JSONArray confArr) {
		File file = new File(logFile);
		if (file.exists()) {
			file.delete();
		}
		
		
		
		RandomAccessFile raf = null;
		int count = 0, i = 0, thresholdCount = 0; 
		DecimalFormat df = new DecimalFormat("0.0000000000");
		try {
			file.createNewFile();
			raf = new RandomAccessFile(file, "rw");
			raf.writeBytes("SAAM Person,URI in SAAM,URI in Wiki, URI in DBPedia,URI found by karma,matched, ,No.1,No.2,No.3,No.4,No.5 \r\n");
			List<ScoreBoard> list = sortResultList(map);
			int len = list.size();
			for (int j = 0; j < len; j++) {
				
				ScoreBoard s = list.get(j); 
				if (s.getDbpediaUri().trim().length() > 0){
					i ++;
				}
				if (s.getKarmaUri() != null && s.getKarmaUri().length() > 0) {
					count++;
					StringBuffer sb = new StringBuffer();
					List<MultiScore> rankList = s.getRankList();
					for (int k = 0; k < rankList.size(); k++) {
						MultiScore ms = rankList.get(k);
						sb.append(", [").append(df.format(ms.getFinalScore())).append("]");
						for (Score sc : ms.getScoreList()) {
							sc.setSrcObj(replaceComma(sc.getSrcObj()));
							sc.setDstObj(replaceComma(sc.getDstObj()));
							
							sb.append(" {").append(df.format(sc.getSimilarity()) + "|" + df.format(sc.getFreq())).append("== ")
								.append(sc.getSrcObj() == null ? "----" : sc.getSrcObj())
								.append(" | ")
								.append(sc.getDstObj() == null ? "----" : sc.getDstObj()).append("} ");
						}
						
					}
					if (s.getDbpediaUri().equals(s.getKarmaUri())) {
						raf.writeBytes(s.getSubject() + "," + s.getSaamUri() + ",\"" + s.getWikiUri() + "\",\"" + s.getDbpediaUri() + "\",\"" + s.getKarmaUri() + "\"," + df.format(s.getFound()) + ", same" + sb.toString() + "\r\n");
							if (Math.abs(s.getFound() -1) < 1e-5) {
							//perfCount ++;
						}
						thresholdCount ++;
					} else {
						raf.writeBytes(s.getSubject() + "," + s.getSaamUri() + ",\"" + s.getWikiUri() + "\",\"" + s.getDbpediaUri() + "\",\"" + s.getKarmaUri() + "\"," + df.format(s.getFound()) + ", not same" + sb.toString() + "\r\n");
						
					}
				} else {
					raf.writeBytes(s.getSubject() + "," + s.getSaamUri() + ",\"" + s.getWikiUri() + "\",\"" + s.getDbpediaUri() + "\",,\r\n");
				}
			}
			raf.writeBytes("(similarity >= " + threshold + ") precision: " + thresholdCount + " of " + count + " (" + df.format(thresholdCount*1.0/count) + ")\r\n");
			raf.writeBytes("(similarity >= " + threshold + ") recall: " + thresholdCount + " of " + i + " (" + df.format(thresholdCount*1.0/i) + ")\r\n");
			raf.writeBytes("(similarity >= " + threshold + ") F1 score:" + df.format(2.0*thresholdCount/(i+count)) + "\r\n");
			log.info("(similarity >= " + threshold + ") precision: " + thresholdCount + " of " + count + " (" + df.format(thresholdCount*1.0/count) + ")");
			log.info("(similarity >= " + threshold + ") recall: " + thresholdCount + " of " + i + " (" + df.format(thresholdCount*1.0/i) + ")");
			log.info("(similarity >= " + threshold + ") F1 score:" + df.format(2.0*thresholdCount/(i+count)) + "\r\n");
			
			raf.writeBytes(confArr.toString());
			//raf.writeBytes("(similarity = 1) precision: " + perfCount + " of " + count + " (" + df.format(perfCount*1.0/count) + ")\r\n");
			//raf.writeBytes("(similarity = 1) recall: " + perfCount + " of " + i + " (" + df.format(perfCount*1.0/i) + ")\r\n");
			
		} catch (IOException e) {
			
			e.printStackTrace();
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	public void write2Log(List<ResultRecord> list, JSONArray confArr) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		write2Log(list, Constants.PATH_SCORE_BOARD_FILE + "result" + sdf.format(new Date()) + ".csv", confArr);
	}
	
	public void write2Log(List<ResultRecord> list, String logFile, JSONArray confArr) {
		File file = new File(logFile);
		if (file.exists()) {
			file.delete();
		}
		
		RandomAccessFile raf = null;
		int i = 0;
		
		DecimalFormat df = new DecimalFormat("0.0000000000");
		try {
			file.createNewFile();
			raf = new RandomAccessFile(file, "rw");
			raf.writeBytes("Src URI,URI found by karma,matched, ,No.1,No.2,No.3,No.4,No.5 \r\n");
			
			int len = list.size();
			for (int j = 0; j < len; j++) {
				
				ResultRecord r = list.get(j); 
				if (r.getCurrentMaxScore() >= threshold) {
					i ++;
				
					StringBuffer sb = new StringBuffer();
					List<MultiScore> rankList = r.getRankList();
					MultiScore m = rankList.get(0);
					for (int k = 0; k < rankList.size(); k++) {
						MultiScore ms = rankList.get(k);
						sb.append(", [").append(df.format(ms.getFinalScore())).append("]");
						for (Score sc : ms.getScoreList()) {
							sc.setSrcObj(replaceComma(sc.getSrcObj()));
							sc.setDstObj(replaceComma(sc.getDstObj()));
							
							sb.append(" {").append(df.format(sc.getSimilarity()) + "|" + df.format(sc.getFreq())).append("== ")
								.append(sc.getSrcObj() == null ? "----" : sc.getSrcObj())
								.append(" | ")
								.append(sc.getDstObj() == null ? "----" : sc.getDstObj()).append("} ");
						}
						
					}
				
					raf.writeBytes(r.getRes().getSubject() + ",\"" + m.getDstSubj().getSubject()+ "\"," + df.format(r.getCurrentMaxScore()) + ", same" + sb.toString() + "\r\n");
						
					
				} else {
					if (r.getCurrentMaxScore() > 0) {
						StringBuffer sb = new StringBuffer();
						List<MultiScore> rankList = r.getRankList();
						MultiScore m = rankList.get(0);
						for (int k = 0; k < rankList.size(); k++) {
							MultiScore ms = rankList.get(k);
							sb.append(", [").append(df.format(ms.getFinalScore())).append("]");
							for (Score sc : ms.getScoreList()) {
								sc.setSrcObj(replaceComma(sc.getSrcObj()));
								sc.setDstObj(replaceComma(sc.getDstObj()));
								
								sb.append(" {").append(df.format(sc.getSimilarity()) + "|" + df.format(sc.getFreq())).append("== ")
									.append(sc.getSrcObj() == null ? "----" : sc.getSrcObj())
									.append(" | ")
									.append(sc.getDstObj() == null ? "----" : sc.getDstObj()).append("} ");
							}
							
						}
						raf.writeBytes(r.getRes().getSubject() + ",\"" + m.getDstSubj().getSubject() + "\",, not same,\r\n");
					} else {
						raf.writeBytes(r.getRes().getSubject() + "," + ",,,\r\n");
					}
				}
			}
			raf.writeBytes("(similarity >= " + threshold + ") found: " + i + " of " + len + " (" + df.format(i*1.0/len) + ")\r\n");
			log.info("(similarity >= " + threshold + ") found: " + i + " of " + len + " (" + df.format(i*1.0/len) + ")\r\n");
			
			raf.writeBytes(confArr.toString());
			//raf.writeBytes("(similarity = 1) precision: " + perfCount + " of " + count + " (" + df.format(perfCount*1.0/count) + ")\r\n");
			//raf.writeBytes("(similarity = 1) recall: " + perfCount + " of " + i + " (" + df.format(perfCount*1.0/i) + ")\r\n");
			
		} catch (IOException e) {
			
			e.printStackTrace();
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	private List<ScoreBoard> sortResultList(Map<String, ScoreBoard> map) {
		List<ScoreBoard> list = new ArrayList<ScoreBoard>();
		ScoreBoard rec;
		
		for (ScoreBoard s : map.values()) {
			int i;
			for (i = 0; i < list.size() ; i++) {
				rec = list.get(i);
				if (s.getFound() > rec.getFound()){
					break;
				}
			}
			list.add(i, s);
		}
		
		return list;
	}
	
	private String replaceComma(String str) {
		if (str != null && str.indexOf(",") > -1) {
			str = str.replaceAll(",", "%2C");
		}
		return str;
	}
	
	private String replaceCommaBack(String str) {
		if (str != null && str.indexOf("%2C") > -1) {
			str = str.replaceAll("%2C", ",");
		}
		return str;
	}
	
	private String[] split(String str) {
		ArrayList<String> list = new ArrayList<String>();
		int index = str.indexOf(',');
		while (index > -1) {
			list.add(str.substring(0, index));
			str = str.substring(index+1);
			index = str.indexOf(',');
		}
		list.add(str);
		String[] arr = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i);
		}
		
		return arr;
	}
	
	private String[] split(String str, String delimiter) {
		ArrayList<String> list = new ArrayList<String>();
		int index = str.indexOf(delimiter);
		while (index > -1) {
			list.add(str.substring(0, index));
			str = str.substring(index+delimiter.length());
			index = str.indexOf(delimiter);
		}
		list.add(str);
		String[] arr = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i);
		}
		
		return arr;
	}
	
	/**
	 * Load score results from result file based on score board.
	 * @param filename of result file
	 * @return score board data list with ground truth
	 */
	public List<ScoreBoard> loadScoreBoardFile(String filename) {
		File file = new File(Constants.PATH_SCORE_BOARD_FILE + filename);
		if (!file.exists()){
			throw new IllegalArgumentException("file " + file.getAbsolutePath() + " not exists.");
		}
		
		RandomAccessFile raf = null;
		List<ScoreBoard> list = new ArrayList<ScoreBoard>();
		
		try {
			raf = new RandomAccessFile(file, "r");
			String line;
			raf.readLine();
			double found = -1;
			List<MultiScore> rankList;
			
			while ((line = raf.readLine()) != null) {
				String[] arr = split(line);
				
				if (line.indexOf("http") > -1) {
					if (arr.length >= 8) {
						ScoreBoard s = new ScoreBoard();
						s.setSubject(arr[0]);
						s.setSaamUri(arr[1]);
						s.setWikiUri(removeDoubleQuotes(arr[2]));
						s.setDbpediaUri(removeDoubleQuotes(arr[3]));
						s.setKarmaUri(removeDoubleQuotes(arr[4]));
						try {
							found = Double.parseDouble(arr[5]);
						} catch (NumberFormatException nfe) {
							found = -1;
						}
						//System.out.println(s.getSubject());
						if (arr.length >= 11) {
							rankList = parseRankList(arr[7], arr[8], arr[9], arr[10]);
						} else if (arr.length == 10) {
							rankList = parseRankList(arr[7], arr[8], arr[9]);
						} else if (arr.length == 9) {
							rankList = parseRankList(arr[7], arr[8]);
						} else if (arr.length == 8) {
							rankList = parseRankList(arr[7]);
						} else {
							rankList = null;
						}
						s.setRankList(rankList);
						s.setFound(found);
						list.add(s);
					} else {
						ScoreBoard s = new ScoreBoard();
						s.setSubject(arr[0]);
						s.setSaamUri(arr[1]);
						s.setWikiUri(removeDoubleQuotes(arr[2]));
						s.setDbpediaUri(removeDoubleQuotes(arr[3]));
						s.setKarmaUri("");
						s.setFound(-1);
						list.add(s);
					}
				} 
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}
	
	/**
	 * load score result from file without score board
	 * @param filename of result file
	 * @return score board data list without ground truth
	 */
	public List<ScoreBoard> loadScoreResultFile(String filename) {
		File file = null;
		if (filename.indexOf(':') > -1 || filename.startsWith("/")) {
			file = new File(filename);
		} else {
			file = new File(Constants.PATH_SCORE_BOARD_FILE + filename);
		}
		if (!file.exists()){
			throw new IllegalArgumentException("file " + file.getAbsolutePath() + " not exists.");
		}
		
		RandomAccessFile raf = null;
		List<ScoreBoard> list = new ArrayList<ScoreBoard>();
		
		try {
			raf = new RandomAccessFile(file, "r");
			String line;
			raf.readLine();
			double found = -1;
			List<MultiScore> rankList;
			
			while ((line = raf.readLine()) != null) {
				String[] arr = split(line);
				
				if (line.indexOf("http") > -1) {
					if (arr.length >= 5) {
						ScoreBoard s = new ScoreBoard();
						s.setSubject(arr[0]);
						s.setSaamUri(arr[0]);
						s.setWikiUri("");
						s.setDbpediaUri("");
						s.setKarmaUri(removeDoubleQuotes(arr[1]));
						try {
							found = Double.parseDouble(arr[2]);
						} catch (NumberFormatException nfe) {
							found = -1;
						}
						//System.out.println(s.getSubject());
						if (arr.length >= 8) {
							rankList = parseRankList(arr[4], arr[5], arr[6], arr[7]);
						} else if (arr.length == 7) {
							rankList = parseRankList(arr[4], arr[5], arr[6]);
						} else if (arr.length == 6) {
							rankList = parseRankList(arr[4], arr[5]);
						} else if (arr.length == 5) {
							rankList = parseRankList(arr[4]);
						} else {
							rankList = null;
						}
						s.setRankList(rankList);
						s.setFound(found);
						list.add(s);
					} else {
						ScoreBoard s = new ScoreBoard();
						s.setSubject(arr[0]);
						s.setSaamUri(arr[0]);
						s.setWikiUri("");
						s.setDbpediaUri("");
						s.setKarmaUri("");
						s.setFound(-1);
						list.add(s);
					}
				} 
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}

	private List<MultiScore> parseRankList(String... strs) {
		String[] preds = {"http://americanart.si.edu/saam/deathYear", "http://americanart.si.edu/saam/birthYear", "http://americanart.si.edu/saam/fullName"};
		List<MultiScore> list = new ArrayList<MultiScore> ();
		List<Score> scoreList = null;
		
		String finalScore, sim, attr;
		for (String str : strs) {
			if (str.indexOf('{') > -1 && str.indexOf('}') > -1) {
				MultiScore ms = new MultiScore();
				
				scoreList = new ArrayList<Score>();
				finalScore = str.substring(str.indexOf('[')+1, str.indexOf(']'));
				str = str.substring(str.indexOf(']') + 2);
				ms.setFinalScore(Double.parseDouble(finalScore));
				
				String[] scoreStrs = split(str, "{");
				int i = 0;
				for (String substr : scoreStrs) {
					if (substr.indexOf('|') != substr.lastIndexOf('|')) {
						
						sim = substr.substring(0, substr.indexOf('='));
						attr = substr.substring(substr.indexOf('=') + 2);
						
						Score s = new Score();
						s.setPredicate(preds[i++]);
						
						s.setSimilarity(Double.parseDouble(sim.substring(0, sim.indexOf('|'))));
						s.setFreq(Double.parseDouble(sim.substring(sim.indexOf('|')+1)));
						s.setSrcObj(replaceCommaBack(attr.substring(0, attr.lastIndexOf('|'))));
						s.setDstObj(replaceCommaBack(attr.substring(attr.indexOf('|') + 1, attr.lastIndexOf('}'))));
						scoreList.add(s);
					}
				}
				ms.setScoreList(scoreList);
				list.add(ms);
			}
		}
		return list;
	}
	
	public String getLastestResult() {
		String path = Constants.PATH_SCORE_BOARD_FILE;
		File file = new File(path);
		if (!file.isDirectory()) {
			throw new IllegalArgumentException(path + " is not a directory");
		}
		FilenameFilter filter = new FilenameFilter() {

			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith(".csv")){
					return true;
				}
				return false;
			}};
		File[] files = file.listFiles(filter);
		long latest = files[0].lastModified();
		File latestFile = files[0];
		for (File f : files) {
			if (latest < f.lastModified()) {
				latest = f.lastModified();
				latestFile = f;
			}
		}
		return latestFile.getName();
	}
	
	public String removeDoubleQuotes(String str) {
		int p1, p2;
		p1 = str.indexOf('"');
		if (p1 > -1) {
			p2 = str.lastIndexOf('"');
			if (p2 > -1 && p2 != p1) {
				str = str.substring(p1+1, p2);
			} else {
				str = str.substring(p1+1);
			}
		}
		return str;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
}
