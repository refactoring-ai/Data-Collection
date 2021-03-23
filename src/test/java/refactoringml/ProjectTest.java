package refactoringml;

import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import refactoringml.db.Project;
import refactoringml.util.CounterUtils;

public class ProjectTest {
    @Test
    public void constructor(){
        CounterUtils.CounterResult counterResult = CounterUtils.countProductionAndTestFiles(Paths.get(""));
        Calendar time = Calendar.getInstance();

        Project projectTrueth = new Project("test", "test", "testName", time,
                0, List.of(3, 5, 6).toString(), "a", counterResult, 0);
        Assertions.assertEquals(List.of(3, 5, 6), projectTrueth.commitCountThresholdsInt);

        Project project = new Project("test", "test", "testName", time,
                0, List.of(6, 5, 3).toString(), "a", counterResult, 0);
        Assertions.assertEquals(projectTrueth.toString(), project.toString());

        project = new Project("test", "test", "testName", time,
                0, "6, 3, 5   sf", "a", counterResult, 0);
        Assertions.assertEquals(projectTrueth.toString(), project.toString());

        project = new Project("test", "test", "testName", time,
                0, "[6,3,5]", "a", counterResult, 0);
        Assertions.assertEquals(projectTrueth.toString(), project.toString());
    }
}