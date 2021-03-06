package org.nutz.quartz;

/**
 * 表达式的每个项目
 * 
 * @author zozoh(zozohtnt@gmail.com)
 */
class QzItem {

	/*---------------------------------------常量表-----*/
	/**
	 * 所有的值都可以被匹配
	 */
	final static int ANY = 0;

	/**
	 * 表范围: values[1] 表示最小值, values[2] 表示最大值
	 */
	final static int RANGE = 1;

	/**
	 * 枚举: 从 values[1] 开始表示可以允许的值
	 */
	final static int LIST = 2;

	/**
	 * 步长: values[1] 表示起始值， values[2] 表示步长
	 */
	final static int SPAN = 3;

	/**
	 * 单值: values[1] 表示被精确匹配的值
	 */
	final static int ONE = 4;

	/**
	 * 数组表示一个值的范围:
	 * 
	 * <pre>
	 *   [类型(ANY|RANGE|LIST|ONE)], [值1], [值2] ...
	 * </pre>
	 * 
	 * 值支持 -1，表示表达式中的 "L" 修饰符
	 */
	protected int[] values;

	protected void valueOf(String str) {
		// 看看是不是 ANY
		if ("?".equals(str) || "*".equals(str)) {
			values = new int[]{ANY};
			return;
		}

		// 看看是不是列表
		String[] ss = str.split(",");
		if (ss.length > 1) {
			values = new int[ss.length + 1];
			values[0] = LIST;
			int i = 1;
			for (String s : ss)
				values[i++] = eval4override(s);
			return;
		}

		// 看看是不是步长
		ss = str.split("/");
		if (ss.length > 1) {
			values = new int[]{SPAN, eval4override(ss[0]), eval4override(ss[1])};
			return;
		}

		// 看看是不是范围
		ss = str.split("-");
		if (ss.length > 1) {
			values = new int[]{RANGE, eval4override(ss[0]), eval4override(ss[1])};
			return;
		}
		// 那么一定是固定值了
		values = new int[]{ONE, eval4override(str)};
	}

	/**
	 * 判断给定值是否匹配, 给出一个值的范围，以便解析 -1 的值
	 * 
	 * @param v
	 *            被判断的值
	 * 
	 * @param min
	 *            最小值(包括) , -1 表示不限制
	 * 
	 * @param max
	 *            最大值(不包括) , -1 表示不限制
	 * 
	 * @param c
	 *            参考时间
	 */
	public boolean match(int v, int min, int max) {
		// 如果值不在范围中
		if (v < min || v >= max)
			return false;

		// 通配
		if (ANY == values[0])
			return true;

		// 准备一下要判断的数组
		int[] refs = prepare(max);

		// 判断
		return _match_(v, refs);
	}

	protected boolean _match_(int v, int[] refs) {
		switch (refs[0]) {
		case ONE:
			return v == refs[1];

		case RANGE:
			return v >= refs[1] && v <= refs[2];

		case LIST:
			for (int i = 1; i < refs.length; i++)
				if (refs[i] == v)
					return true;
			return false;

		case SPAN:
			return (v - refs[1]) % refs[2] == 0;

		case ANY:
			return true;
		}
		// 默认则不匹配
		return false;
	}

	/**
	 * 根据值的范围，返回一个新的 values 数组，这里解决的 "L" 修饰符的问题
	 * 
	 * @param max
	 *            最大值(不包括)
	 * 
	 * @return 新的数组， match 函数会根据这个数组进行判断
	 */
	protected int[] prepare(int max) {
		// 准备返回值
		int[] refs = new int[values.length];
		refs[0] = values[0];

		// 判断是否需要 L 一下 ...
		for (int i = 1; i < refs.length; i++) {
			int v = values[i];
			refs[i] = v < 0 ? max + v : v;
		}

		// 返回
		return refs;
	}

	/**
	 * 根据一个值，评估出一个数值来，这里判断了 "L" 的问题
	 * 
	 * @param str
	 *            原始字符串
	 * @param dict
	 *            字典，如果为 null 那么字符串不接受简名，如果匹配，则采用字典中的下标作为值
	 * @param dictOffset
	 *            从字典的哪个下标开始查
	 * @return
	 */
	protected int eval(String str, String[] dict, int dictOffset) {
		int off = 1;
		if (str.endsWith("L")) {
			off = -1;
			str = str.substring(0, str.length() - 1);
		}
		int re;
		try {
			re = Integer.parseInt(str);
		}
		catch (NumberFormatException e) {
			if (null != dict) {
				String s = str.toUpperCase();
				for (int i = dictOffset; i < dict.length; i++)
					if (s.equals(dict[i]))
						return i * off;
			}
			throw e;
		}
		return re * off;
	}

	// 子类重载它，可以支持更丰富的值
	protected int eval4override(String str) {
		return eval(str, null, -1);
	}

	/*--------------------------------------快捷名称-----*/
	static String[] DAYS_OF_WEEK = new String[]{null,
												"SUN",
												"MON",
												"TUE",
												"WED",
												"THU",
												"FRI",
												"SAT"};

	static String[] MONTH_OF_YEAR = new String[]{	null,
													"JAN",
													"FEB",
													"MAR",
													"APR",
													"MAY",
													"JUN",
													"JUL",
													"AUG",
													"SEP",
													"OCT",
													"NOV",
													"DEC"};

}
