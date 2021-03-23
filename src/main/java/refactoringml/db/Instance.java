package refactoringml.db;

import static refactoringml.util.FileUtils.isTestFile;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

//Base class for all commits saved in the DB
@MappedSuperclass
public abstract class Instance extends PanacheEntity {

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(nullable = false)
    // project id: referencing the project information, e.g. name or gitUrl
    public Project project;

    @ManyToOne(cascade = CascadeType.ALL)
    public CommitMetaData commitMetaData;

    // relative filepath to the java file of the class file
    @Column(columnDefinition = "TEXT")
    public String filePath;
    // name of the class, @Warning: might differ from the filename
    public String className;
    // is this commit affecting a test?
    public boolean isTest;

    // Describes the level of the class being affected, e.g. class level or method
    // level refactoring
    // For a mapping see: RefactoringUtils
    // TODO: make this an enum, for better readibility
    public int level;

    @ManyToOne(cascade = CascadeType.ALL)
    public ClassMetric classMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    public MethodMetric methodMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    public VariableMetric variableMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    public FieldMetric fieldMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    public ProcessMetrics processMetrics;

    public Instance() {
    }

    public Instance(Project project, CommitMetaData commitMetaData, String filePath, String className,
            ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics,
            FieldMetric fieldMetrics, int level) {
        this.project = project;
        this.commitMetaData = commitMetaData;
        this.filePath = filePath;
        this.className = className;
        this.classMetrics = classMetrics;
        this.methodMetrics = methodMetrics;
        this.variableMetrics = variableMetrics;
        this.fieldMetrics = fieldMetrics;
        this.level = level;

        this.isTest = isTestFile(filePath);
    }

    public String getCommit() {
        return commitMetaData.commitId;
    }

    public String getClassName() {
        return className;
    }

    public String getCommitMessage() {
        return commitMetaData.commitMessage;
    }

    public String getCommitUrl() {
        return commitMetaData.commitUrl;
    }

    @Override
    public String toString() {
        return ", project=" + project + ", commitMetaData=" + commitMetaData + ", filePath='" + filePath + '\''
                + ", className='" + className + '\'' + ", classMetrics=" + classMetrics + ", methodMetrics="
                + methodMetrics + ", variableMetrics=" + variableMetrics + ", fieldMetrics=" + fieldMetrics
                + ", processMetrics=" + processMetrics + ", level=" + level;
    }

    public void merge() {
        getEntityManager().merge(this);
    }
}
