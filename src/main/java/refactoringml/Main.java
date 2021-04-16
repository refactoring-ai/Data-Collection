package refactoringml;

import io.quarkus.runtime.Quarkus;

public class Main {

	public static void main(String[] args) {
		Quarkus.run(DataCollector.class, args);
	}

}
