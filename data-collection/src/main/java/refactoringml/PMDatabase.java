package refactoringml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PMDatabase {

	private Map<String, ProcessMetric> database;
	private int commitThreshold;

	public PMDatabase (int commitThreshold) {
		this.commitThreshold = commitThreshold;
		this.database = new HashMap<>();
	}


	public boolean containsKey (String fileName) {
		return database.containsKey(fileName);
	}

	public void put (String key, ProcessMetric value) {
		database.put(key, value);
	}

	public ProcessMetric get (String key) {
		return database.get(key);
	}

	public void updateNotRefactored (Set<String> refactoredClasses) {
		database.values().stream()
				.filter(p -> !refactoredClasses.contains(p.getFileName()))
				.forEach(p -> p.notRefactoredInThisCommit());
	}

	public List<ProcessMetric> refactoredLongAgo () {
		return database.values().stream()
				.filter(p -> p.lastRefactoring() >= commitThreshold)
				.collect(Collectors.toList());
	}

	public void remove (ProcessMetric clazz) {
		remove(clazz.getFileName());
	}

	public void remove (String key) {
		database.remove(key);
	}
}
