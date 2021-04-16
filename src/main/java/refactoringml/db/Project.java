package refactoringml.db;

import javax.persistence.*;
import javax.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import refactoringml.util.JGitUtils;

import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.CounterUtils.*;

@Entity
@Table(name = "project", indexes = { @Index(columnList = "datasetName"), @Index(columnList = "projectName") })
public class Project extends PanacheEntity {

	public String datasetName;
	public String gitUrl;
	public String projectName;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
	public Collection<ClassMetric> classMetrics;

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar dateOfProcessing;

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar finishedDate;

	public int commits;

	// Collect instances of non-refactorings with different Ks e.g, 25,50,100
	// commits on a file without refactorings, this is for hibernate
	public String commitCountThresholds;
	// Collect instances of non-refactorings with different Ks e.g, 25,50,100
	// commits on a file without refactorings
	@Transient
	public List<Integer> commitCountThresholdsInt;
	// this is only used by the ProcessMetricsCollector, in order to reset the
	// stable commits after the highest K was fulfilled
	@Transient
	public int maxCommitThreshold;

	public long javaLoc;

	public long numberOfProductionFiles;
	public long numberOfTestFiles;
	public long productionLoc;
	public long testLoc;
	public long projectSizeInBytes;

	public int exceptionsCount;

	public String lastCommitHash;

	// does the project have a remote origin, or is it a local one?
	public boolean isLocal;

	public Project() {
	}

	public Project(String datasetName, String gitUrl, String projectName, Calendar dateOfProcessing, int commits,
			String commitCountThresholds, String lastCommitHash, CounterResult c, long projectSizeInBytes) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.dateOfProcessing = dateOfProcessing;
		this.commits = commits;
		this.lastCommitHash = lastCommitHash;

		this.numberOfProductionFiles = c.getQtyOfProductionFiles();
		this.numberOfTestFiles = c.getQtyOfTestFiles();
		this.productionLoc = c.getLocProductionFiles();
		this.testLoc = c.getLocTestFiles();
		this.projectSizeInBytes = projectSizeInBytes;
		this.javaLoc = this.productionLoc + this.testLoc;
		this.isLocal = JGitUtils.isLocal(gitUrl);

		// clean the string to be more robust
		String cleanCommitCountThresholds = commitCountThresholds.replaceAll("[^\\d,.]", "");
		List<String> rawCommitThresholds = Arrays.asList(cleanCommitCountThresholds.split(","));

		this.commitCountThresholdsInt = rawCommitThresholds.stream().map(Integer::parseInt)
				.sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		this.commitCountThresholds = commitCountThresholdsInt.toString();
		this.maxCommitThreshold = Collections.max(this.commitCountThresholdsInt);
		this.exceptionsCount = c.getExceptionsCount();
	}

	// Every time an exception is reaching the App class, the db is rollback but not
	// the PMDatabase
	public boolean isInconsistent() {
		return exceptionsCount > 0;
	}

	@Override
	public String toString() {
		return "Project{" + ", datasetName='" + datasetName + '\'' + ", gitUrl='" + gitUrl + '\'' + ", commits="
				+ commits + '\'' + ", numberOfProductionFiles=" + numberOfProductionFiles + '\''
				+ ", numberOfTestFiles=" + numberOfTestFiles + '\'' + ", projectName='" + projectName + '\'' + '}';
	}

	@Transactional
	public static boolean projectExists(String gitUrl) {
		return Project.find("gitUrl", gitUrl).singleResultOptional().isPresent();
	}

	public void merge() {
		getEntityManager().merge(this);
	}
}