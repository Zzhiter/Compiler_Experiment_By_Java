import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

public class AnalyseList {

	// 成员变量,产生式集，终结符集，非终结符集
	ArrayList<Production> productions;
	ArrayList<String> terminals;
	ArrayList<String> nonterminals;
	HashMap<String, ArrayList<String>> firsts;
	HashMap<String, ArrayList<String>> follows;

	public AnalyseList() {
		productions = new ArrayList<Production>();
		terminals = new ArrayList<String>();
		nonterminals = new ArrayList<String>();
		firsts = new HashMap<String, ArrayList<String>>();
		follows = new HashMap<String, ArrayList<String>>();

		setProductions();
		setNonTerminals();
		setTerminals();

		getFirst();
		getFollow();
		getSelect();

		Predict();

	}

	// 从文件中读取产生式
	public void setProductions() {
		try {
			File file = new File("grammar.txt");
			RandomAccessFile randomfile = new RandomAccessFile(file, "r");
			String line;
			String left;
			String right;
			Production production;
			while ((line = randomfile.readLine()) != null) {
				left = line.split("->")[0].trim();
				right = line.split("->")[1].trim();
				// 这里right.split(" ")完以后，就变成string数组了
				production = new Production(left, right.split(" "));
				productions.add(production);
			}
			randomfile.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	// 获得非终结符集
	public void setNonTerminals() {
		try {
			File file = new File("grammar.txt");
			RandomAccessFile randomfile = new RandomAccessFile(file, "r");
			String line;
			String left;
			while ((line = randomfile.readLine()) != null) {
				// 因为是非终结符集，所以只取产生式左边的元素，还需要判重
				left = line.split("->")[0].trim();
				if (nonterminals.contains(left)) {
					continue;
				} else {
					nonterminals.add(left);
				}
			}
			randomfile.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	// 获得终结符集,依赖于获得产生式函数
	public void setTerminals() {
		// 遍历所有的产生式
		String[] rights;
		for (int i = 0; i < productions.size(); i++) {
			rights = productions.get(i).returnRights();
			// 从右侧寻找终结符
			for (int j = 0; j < rights.length; j++) {
				if (nonterminals.contains(rights[j]) || rights[j].equals("$")) {
					continue;
				} else {
					terminals.add(rights[j]);
				}
			}
		}
	}

	// 获取First集
	public void getFirst() {
		// 终结符全部求出first集，终结符的first集就是终结符本身
		ArrayList<String> first;
		for (int i = 0; i < terminals.size(); i++) {
			first = new ArrayList<String>();
			first.add(terminals.get(i));
			firsts.put(terminals.get(i), first);
		}
		// 给所有非终结符在哈希表firsts中注册一下
		for (int i = 0; i < nonterminals.size(); i++) {
			first = new ArrayList<String>();
			firsts.put(nonterminals.get(i), first);
		}

		// 非终结符的first集
		boolean flag;
		while (true) {
			flag = true;
			String left;
			String right;
			String[] rights;
			// 对于每个产生式
			for (int i = 0; i < productions.size(); i++) {
				left = productions.get(i).returnLeft();
				rights = productions.get(i).returnRights();
				// 对于每个产生式的右部right
				for (int j = 0; j < rights.length; j++) {

					right = rights[j];
					// right是否存在，遇到空怎么办，不为空的话
					if (!right.equals("$")) {
						// 对于right的first集中的每一个元素
						for (int l = 0; l < firsts.get(right).size(); l++) {
							// 如果产生式左部的left的first集中已经包含了right的first集中第l个元素，则跳过
							if (firsts.get(left).contains(firsts.get(right).get(l))) {
								continue;
							}
							// 否则加入到left的first集中
							else {
								firsts.get(left).add(firsts.get(right).get(l));
								flag = false;
							}
						}
					}

					// 如果产生式右部right可以产生$, 则跳过这个产生式
					if (isCanBeNull(right)) {
						continue;
					} else {
						break;
					}
				}
			}

			// 如果flag为true，表示没有新的加进来，那就应该结束了。
			if (flag == true) {
				break;
			}

		}
	}

	// 获得Follow集
	public void getFollow() {
		// 所有非终结符的follow集初始化一下
		ArrayList<String> follow;
		for (int i = 0; i < nonterminals.size(); i++) {
			follow = new ArrayList<String>();
			follows.put(nonterminals.get(i), follow);
		}
		// 将#加入到follow(S)中， #代表结束标记
		follows.get("S").add("#");
		boolean flag;
		boolean fab;
		while (true) {
			flag = true;
			// 循环
			for (int i = 0; i < productions.size(); i++) {
				String left;
				String right;
				String[] rights;
				rights = productions.get(i).returnRights();
				for (int j = 0; j < rights.length; j++) {
					right = rights[j];

					// 非终结符
					if (nonterminals.contains(right)) {
						fab = true;
						for (int k = j + 1; k < rights.length; k++) {

							// 查找first集
							for (int v = 0; v < firsts.get(rights[k]).size(); v++) {
								// 将后一个元素的first集加入到前一个元素的follow集中
								if (follows.get(right).contains(firsts.get(rights[k]).get(v))) {
									continue;
								} else {
									follows.get(right).add(firsts.get(rights[k]).get(v));
									flag = false;
								}
							}
							if (isCanBeNull(rights[k])) {
								continue;
							} else {
								fab = false;
								break;
							}
						}
						if (fab) {
							left = productions.get(i).returnLeft();
							for (int p = 0; p < follows.get(left).size(); p++) {
								if (follows.get(right).contains(follows.get(left).get(p))) {
									continue;
								} else {
									follows.get(right).add(follows.get(left).get(p));
									flag = false;
								}
							}
						}
					}

				}
			}

			// 如果flag为true，表示没有新的加进来，那就应该结束了。
			if (flag == true) {
				break;
			}
		}

		// 清除follow集中的#
		String left;
		for (int j = 0; j < nonterminals.size(); j++) {
			left = nonterminals.get(j);
			for (int v = 0; v < follows.get(left).size(); v++) {
				if (follows.get(left).get(v).equals("#"))
					follows.get(left).remove(v);
			}
		}
	}

	// 获取Select集
	public void getSelect() {
		String left;
		String right;
		String[] rights;
		ArrayList<String> follow = new ArrayList<String>();
		ArrayList<String> first = new ArrayList<String>();

		for (int i = 0; i < productions.size(); i++) {
			left = productions.get(i).returnLeft();
			rights = productions.get(i).returnRights();
			if (rights[0].equals("$")) {
				// select(i) = follow(A)
				follow = follows.get(left);
				for (int j = 0; j < follow.size(); j++) {
					if (productions.get(i).select.contains(follow.get(j))) {
						continue;
					} else {
						productions.get(i).select.add(follow.get(j));
					}
				}
			} else {
				boolean flag = true;
				for (int j = 0; j < rights.length; j++) {
					right = rights[j];
					first = firsts.get(right);
					for (int v = 0; v < first.size(); v++) {
						if (productions.get(i).select.contains(first.get(v))) {
							continue;
						} else {
							productions.get(i).select.add(first.get(v));
						}
					}
					if (isCanBeNull(right)) {
						continue;
					} else {
						flag = false;
						break;
					}
				}
				if (flag) {
					follow = follows.get(left);
					for (int j = 0; j < follow.size(); j++) {
						if (productions.get(i).select.contains(follow.get(j))) {
							continue;
						} else {
							// 刚刚这里出现了一个问题，已经被解决啦
							productions.get(i).select.add(follow.get(j));
						}
					}
				}
			}
		}
	}

	// 生成产生式
	public void Predict() {
		Production production;
		String line;
		String[] rights;
		try {
			File file = new File("predictldy.txt");
			RandomAccessFile randomfile = new RandomAccessFile(file, "rw");
			for (int i = 0; i < productions.size(); i++) {
				production = productions.get(i);
				for (int j = 0; j < production.select.size(); j++) {
					line = production.returnLeft() + "#" + production.select.get(j) + " ->";
					rights = production.returnRights();
					for (int v = 0; v < rights.length; v++) {
						line = line + " " + rights[v];
					}
					line = line + "\n";
					// 写入文件
					randomfile.writeBytes(line);
				}
			}
			randomfile.close();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	// 判断是否是终结符
	public boolean isTerminal(String symbol) {
		if (terminals.contains(symbol))
			return true;
		else {
			return false;
		}
	}

	// 判断如果产生式左部是symbol，是否产生$
	public boolean isCanBeNull(String symbol) {
		String[] rights;
		// 对于每个产生式
		for (int i = 0; i < productions.size(); i++) {
			// 如果产生式的左部为symbol
			if (productions.get(i).returnLeft().equals(symbol)) {
				rights = productions.get(i).returnRights();
				// 如果产生式右部第一个元素为$，返回true
				if (rights[0].equals("$")) {
					return true;
				}
			}
		}
		return false;
	}
}