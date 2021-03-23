package refactoringml.db;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.transaction.Transactional;

@Entity
@Table(name = "StableCommit", indexes = { @Index(columnList = "project_id"), @Index(columnList = "level"),
		@Index(columnList = "isTest"), @Index(columnList = "commitThreshold") })
public class StableCommit extends Instance {
	// The commit threshold for which this class is considered as stable,
	public int commitThreshold;

	public StableCommit() {
	}

	public StableCommit(Project project, CommitMetaData commitMetaData, String filePath, String className,
			ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics,
			FieldMetric fieldMetrics, int level, int commitThreshold) {
		super(project, commitMetaData, filePath, className, classMetrics, methodMetrics, variableMetrics, fieldMetrics,
				level);
		this.commitThreshold = commitThreshold;
	}

	@Override
	public String toString() {
		return "StableCommit{" + super.toString() + ", commitThreshold=" + commitThreshold + '}';
	}

	public static long countForProjectAndThreshold(Project project, int threshold) {
		return count("project = ?1 AND commitThreshold = ?2", project, threshold);
	}

	@Transactional
	public static long countForProject(Project project) {
		return count("project", project);
	}
}