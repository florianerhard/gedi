package gedi.core.data.table;

import org.h2.expression.ConditionAndOr;

public enum ConditionOperator {

	AND,OR,NEW;

	public static ConditionOperator fromConditionAndOrType(int type) {
		if (type==ConditionAndOr.AND) return AND;
		if (type==ConditionAndOr.OR) return OR;
		return NEW;
	}
	
}
