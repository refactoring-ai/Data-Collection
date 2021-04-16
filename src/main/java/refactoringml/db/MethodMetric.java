package refactoringml.db;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "MethodMetric")
public class MethodMetric extends PanacheEntity {

	@Column(nullable = true, length = 2000)
	public String fullMethodName;
	@Column(nullable = true, length = 256)
	public String shortMethodName;

	@Column(nullable = true)
	public int startLine;
	@Column(nullable = true)
	public int methodCbo;
	@Column(nullable = true)
	public int methodWmc;
	@Column(nullable = true)
	public int methodRfc;
	@Column(nullable = true)
	public int methodLoc;
	@Column(nullable = true)
	public int methodReturnQty;
	@Column(nullable = true)
	public int methodVariablesQty;
	@Column(nullable = true)
	public int methodParametersQty;
	@Column(nullable = true)
	public int methodInvocationsQty;
	@Column(nullable = true)
	public int methodInvocationsLocalQty;
	@Column(nullable = true)
	public int methodInvocationsIndirectLocalQty;
	@Column(nullable = true)
	public int methodLoopQty;
	@Column(nullable = true)
	public int methodComparisonsQty;
	@Column(nullable = true)
	public int methodTryCatchQty;
	@Column(nullable = true)
	public int methodParenthesizedExpsQty;
	@Column(nullable = true)
	public int methodStringLiteralsQty;
	@Column(nullable = true)
	public int methodNumbersQty;
	@Column(nullable = true)
	public int methodAssignmentsQty;
	@Column(nullable = true)
	public int methodMathOperationsQty;
	@Column(nullable = true)
	public int methodMaxNestedBlocks;
	@Column(nullable = true)
	public int methodAnonymousClassesQty;
	@Column(nullable = true)
	public int methodSubClassesQty;
	@Column(nullable = true)
	public int methodLambdasQty;
	@Column(nullable = true)
	public int methodUniqueWordsQty;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(nullable = false)
	public Project project;

	public MethodMetric() {
	}

	public MethodMetric(String fullMethodName, String shortMethodName, int startLine, int methodCbo, int methodWmc,
			int methodRfc, int loc, int methodReturnQty, int methodVariablesQty, int methodParametersQty,
			int methodInvocations, int methodInvocationsLocal, int methodInvocationsIndirectLocal, int methodLoopQty,
			int methodComparisonsQty, int methodTryCatchQty, int methodParenthesizedExpsQty,
			int methodStringLiteralsQty, int methodNumbersQty, int methodAssignmentsQty, int methodMathOperationsQty,
			int methodMaxNestedBlocks, int methodAnonymousClassesQty, int methodSubClassesQty, int methodLambdasQty,
			int methodUniqueWordsQty, Project project) {
		this.fullMethodName = fullMethodName;
		this.shortMethodName = shortMethodName;
		this.startLine = startLine;
		this.methodInvocationsQty = methodInvocations;
		this.methodInvocationsLocalQty = methodInvocationsLocal;
		this.methodInvocationsIndirectLocalQty = methodInvocationsIndirectLocal;
		this.methodCbo = methodCbo;
		this.methodWmc = methodWmc;
		this.methodRfc = methodRfc;
		this.methodLoc = loc;
		this.methodReturnQty = methodReturnQty;
		this.methodVariablesQty = methodVariablesQty;
		this.methodParametersQty = methodParametersQty;
		this.methodLoopQty = methodLoopQty;
		this.methodComparisonsQty = methodComparisonsQty;
		this.methodTryCatchQty = methodTryCatchQty;
		this.methodParenthesizedExpsQty = methodParenthesizedExpsQty;
		this.methodStringLiteralsQty = methodStringLiteralsQty;
		this.methodNumbersQty = methodNumbersQty;
		this.methodAssignmentsQty = methodAssignmentsQty;
		this.methodMathOperationsQty = methodMathOperationsQty;
		this.methodMaxNestedBlocks = methodMaxNestedBlocks;
		this.methodAnonymousClassesQty = methodAnonymousClassesQty;
		this.methodSubClassesQty = methodSubClassesQty;
		this.methodLambdasQty = methodLambdasQty;
		this.methodUniqueWordsQty = methodUniqueWordsQty;
		this.project = project;
	}


	public String getFullMethodName() {
		return fullMethodName == null ? "" : fullMethodName;
	}

	@Override
	public String toString() {
		return "MethodMetric{" + "fullMethodName='" + fullMethodName + '\'' + ", shortMethodName='" + shortMethodName
				+ '\'' + ", startLine=" + startLine + ", methodCbo=" + methodCbo + ", methodWmc=" + methodWmc
				+ ", methodRfc=" + methodRfc + ", methodLoc=" + methodLoc + ", methodReturnQty=" + methodReturnQty
				+ ", methodVariablesQty=" + methodVariablesQty + ", methodParametersQty=" + methodParametersQty
				+ ", methodLoopQty=" + methodLoopQty + ", methodComparisonsQty=" + methodComparisonsQty
				+ ", methodTryCatchQty=" + methodTryCatchQty + ", methodParenthesizedExpsQty="
				+ methodParenthesizedExpsQty + ", methodStringLiteralsQty=" + methodStringLiteralsQty
				+ ", methodNumbersQty=" + methodNumbersQty + ", methodAssignmentsQty=" + methodAssignmentsQty
				+ ", methodMathOperationsQty=" + methodMathOperationsQty + ", methodMaxNestedBlocks="
				+ methodMaxNestedBlocks + ", methodAnonymousClassesQty=" + methodAnonymousClassesQty
				+ ", methodSubClassesQty=" + methodSubClassesQty + ", methodLambdasQty=" + methodLambdasQty
				+ ", methodUniqueWordsQty=" + methodUniqueWordsQty + '}';
	}

}
