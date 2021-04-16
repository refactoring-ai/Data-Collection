package refactoringml.db;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.jgit.revwalk.RevCommit;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import refactoringml.util.JGitUtils;

@Entity
@Table(name = "CommitMetaData")
public class CommitMetaData extends PanacheEntity {

    //use the unique commit hash to relate from Yes and No to this one
    // for RefactoringCommit, this commit points to the commit the refactoring has happened
    // For No, this commit points to the first commit where the class was stable
    // (i.e., if a class has been to [1..50] commits before considered as instance
    // of no refactoring, commitId = commit 1.
    public String commitId;

    //original commit message
    @Column(columnDefinition = "mediumtext")
    public String commitMessage;
    //url to the commit on its remote repository, e.g. https://github.com/mauricioaniche/predicting-refactoring-ml/commit/36016e4023cb74cd1076dbd33e0d7a73a6a61993
    public String commitUrl;
    //Date this commit was made
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar commitDate;

    //Number of this commit (number of previous commits plus 1)
    public int commitNumber;

    //id of the parent commit, if none exists:
    // the parent commit points to the commit that we calculate the code metrics
    // (we calculate the metrics in the version of file *before* the refactoring)
    public String parentCommitId;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(nullable = false)
	public Project project;

    public CommitMetaData() {this.commitId = "";}

    public CommitMetaData(String commitId, String fullMessage, String url, String parentId, Project project) {
        this.commitId = commitId.trim();
        this.commitDate = new GregorianCalendar();
        this.commitNumber = -1;
        this.commitMessage = fullMessage.trim();
        this.commitUrl = url;
        this.parentCommitId = parentId.trim();
        this.project = project;
    }

    public CommitMetaData(RevCommit commit, int commitNumber, Project project){
        this.commitId = commit.getName().trim();
        this.commitDate = JGitUtils.getGregorianCalendar(commit);
        this.commitNumber = commitNumber;
        this.commitMessage = commit.getFullMessage().trim();
        this.commitUrl = JGitUtils.generateCommitUrl(project.gitUrl, commitId, project.isLocal);
        this.parentCommitId = commit.getParentCount() == 0 ? "Null" : commit.getParent(0).getName().trim();
        this.project = project;
    }

    @Override
    public String toString() {
        return "CommitMetaData{" +
                "commit=" + commitId +
                ", commitDate=" + commitDate +
                ", commitNumber=" + commitNumber +
                ", commitMessage=" + commitMessage +
                ", commitUrl=" + commitUrl +
                ", parentCommit='" + parentCommitId + '\'' +
                '}';
    }
}