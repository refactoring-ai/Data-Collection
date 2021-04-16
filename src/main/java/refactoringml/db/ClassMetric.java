package refactoringml.db;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "ClassMetric")
public class ClassMetric extends PanacheEntity {

	public boolean isInnerClass;
	public int classCbo;
	public int classWmc;
	public int classRfc;
	public int classLcom;
	public int classTCC;
	public int classLCC;
	public int classNumberOfMethods;
	public int classNumberOfStaticMethods;
	public int classNumberOfPublicMethods;
	public int classNumberOfPrivateMethods;
	public int classNumberOfProtectedMethods;
	public int classNumberOfDefaultMethods;
	public int classNumberOfVisibleMethods;
	public int classNumberOfAbstractMethods;
	public int classNumberOfFinalMethods;
	public int classNumberOfSynchronizedMethods;
	public int classNumberOfFields;
	public int classNumberOfStaticFields;
	public int classNumberOfPublicFields;
	public int classNumberOfPrivateFields;
	public int classNumberOfProtectedFields;
	public int classNumberOfDefaultFields;
	public int classNumberOfFinalFields;
	public int classNumberOfSynchronizedFields;
	public int classNosi;
	public int classLoc;
	public int classReturnQty;
	public int classLoopQty;
	public int classComparisonsQty;
	public int classTryCatchQty;
	public int classParenthesizedExpsQty;
	public int classStringLiteralsQty;
	public int classNumbersQty;
	public int classAssignmentsQty;
	public int classMathOperationsQty;
	public int classVariablesQty;
	public int classMaxNestedBlocks;
	public int classAnonymousClassesQty;
	public int classSubClassesQty;
	public int classLambdasQty;
	public int classUniqueWordsQty;

	@JoinColumn(nullable = false)
	@ManyToOne
	public Project project;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "classMetrics", targetEntity = RefactoringCommit.class)
	public Collection<Instance> instances;

	// @OneToOne(cascade = CascadeType.ALL, mappedBy = "classMetrics")
	// public Collection<Sta> refactoringCommits;

	@Deprecated // hibernate purposes
	public ClassMetric() {
	}

	public ClassMetric(boolean isInnerClass, int classCbo, int classWmc, int classRfc, int classLcom, float classTCC,
			float classLCC, int classNumberOfMethods, int classNumberOfStaticMethods, int classNumberOfPublicMethods,
			int classNumberOfPrivateMethods, int classNumberOfProtectedMethods, int classNumberOfDefaultMethods,
			int classNumberOfVisibleMethods, int classNumberOfAbstractMethods, int classNumberOfFinalMethods,
			int classNumberOfSynchronizedMethods, int classNumberOfFields, int classNumberOfStaticFields,
			int classNumberOfPublicFields, int classNumberOfPrivateFields, int classNumberOfProtectedFields,
			int classNumberOfDefaultFields, int classNumberOfFinalFields, int classNumberOfSynchronizedFields,
			int classNosi, int classLoc, int classReturnQty, int classLoopQty, int classComparisonsQty,
			int classTryCatchQty, int classParenthesizedExpsQty, int classStringLiteralsQty, int classNumbersQty,
			int classAssignmentsQty, int classMathOperationsQty, int classVariablesQty, int classMaxNestedBlocks,
			int classAnonymousClassesQty, int classSubClassesQty, int classLambdasQty, int classUniqueWordsQty,
			Project project) {
		this.isInnerClass = isInnerClass;
		this.classCbo = classCbo;
		this.classWmc = classWmc;
		this.classRfc = classRfc;
		this.classLcom = classLcom;
		this.classTCC = (int) (classTCC * 1024);
		this.classLCC = (int) (classLCC * 1024);
		this.classNumberOfMethods = classNumberOfMethods;
		this.classNumberOfStaticMethods = classNumberOfStaticMethods;
		this.classNumberOfPublicMethods = classNumberOfPublicMethods;
		this.classNumberOfPrivateMethods = classNumberOfPrivateMethods;
		this.classNumberOfProtectedMethods = classNumberOfProtectedMethods;
		this.classNumberOfDefaultMethods = classNumberOfDefaultMethods;
		this.classNumberOfVisibleMethods = classNumberOfVisibleMethods;
		this.classNumberOfAbstractMethods = classNumberOfAbstractMethods;
		this.classNumberOfFinalMethods = classNumberOfFinalMethods;
		this.classNumberOfSynchronizedMethods = classNumberOfSynchronizedMethods;
		this.classNumberOfFields = classNumberOfFields;
		this.classNumberOfStaticFields = classNumberOfStaticFields;
		this.classNumberOfPublicFields = classNumberOfPublicFields;
		this.classNumberOfPrivateFields = classNumberOfPrivateFields;
		this.classNumberOfProtectedFields = classNumberOfProtectedFields;
		this.classNumberOfDefaultFields = classNumberOfDefaultFields;
		this.classNumberOfFinalFields = classNumberOfFinalFields;
		this.classNumberOfSynchronizedFields = classNumberOfSynchronizedFields;
		this.classNosi = classNosi;
		this.classLoc = classLoc;
		this.classReturnQty = classReturnQty;
		this.classLoopQty = classLoopQty;
		this.classComparisonsQty = classComparisonsQty;
		this.classTryCatchQty = classTryCatchQty;
		this.classParenthesizedExpsQty = classParenthesizedExpsQty;
		this.classStringLiteralsQty = classStringLiteralsQty;
		this.classNumbersQty = classNumbersQty;
		this.classAssignmentsQty = classAssignmentsQty;
		this.classMathOperationsQty = classMathOperationsQty;
		this.classVariablesQty = classVariablesQty;
		this.classMaxNestedBlocks = classMaxNestedBlocks;
		this.classAnonymousClassesQty = classAnonymousClassesQty;
		this.classSubClassesQty = classSubClassesQty;
		this.classLambdasQty = classLambdasQty;
		this.classUniqueWordsQty = classUniqueWordsQty;
		this.project = project;
	}

	@Override
	public String toString() {
		return "ClassMetric{" + "isInnerClass=" + isInnerClass + ", classCbo=" + classCbo + ", classWmc=" + classWmc
				+ ", classRfc=" + classRfc + ", classLcom=" + classLcom + ", classNumberOfMethods="
				+ classNumberOfMethods + ", classNumberOfStaticMethods=" + classNumberOfStaticMethods
				+ ", classNumberOfPublicMethods=" + classNumberOfPublicMethods + ", classNumberOfPrivateMethods="
				+ classNumberOfPrivateMethods + ", classNumberOfProtectedMethods=" + classNumberOfProtectedMethods
				+ ", classNumberOfDefaultMethods=" + classNumberOfDefaultMethods + ", classNumberOfAbstractMethods="
				+ classNumberOfAbstractMethods + ", classNumberOfFinalMethods=" + classNumberOfFinalMethods
				+ ", classNumberOfSynchronizedMethods=" + classNumberOfSynchronizedMethods + ", classNumberOfFields="
				+ classNumberOfFields + ", classNumberOfStaticFields=" + classNumberOfStaticFields
				+ ", classNumberOfPublicFields=" + classNumberOfPublicFields + ", classNumberOfPrivateFields="
				+ classNumberOfPrivateFields + ", classNumberOfProtectedFields=" + classNumberOfProtectedFields
				+ ", classNumberOfDefaultFields=" + classNumberOfDefaultFields + ", classNumberOfFinalFields="
				+ classNumberOfFinalFields + ", classNumberOfSynchronizedFields=" + classNumberOfSynchronizedFields
				+ ", classNosi=" + classNosi + ", classLoc=" + classLoc + ", classReturnQty=" + classReturnQty
				+ ", classLoopQty=" + classLoopQty + ", classComparisonsQty=" + classComparisonsQty
				+ ", classTryCatchQty=" + classTryCatchQty + ", classParenthesizedExpsQty=" + classParenthesizedExpsQty
				+ ", classStringLiteralsQty=" + classStringLiteralsQty + ", classNumbersQty=" + classNumbersQty
				+ ", classAssignmentsQty=" + classAssignmentsQty + ", classMathOperationsQty=" + classMathOperationsQty
				+ ", classVariablesQty=" + classVariablesQty + ", classMaxNestedBlocks=" + classMaxNestedBlocks
				+ ", classAnonymousClassesQty=" + classAnonymousClassesQty + ", classSubClassesQty="
				+ classSubClassesQty + ", classLambdasQty=" + classLambdasQty + ", classUniqueWordsQty="
				+ classUniqueWordsQty + '}';
	}

}
