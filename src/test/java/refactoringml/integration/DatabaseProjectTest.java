package refactoringml.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static refactoringml.util.FileUtils.createTmpDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.AppBean;
import refactoringml.db.Project;

/*
    Test if a project can be added twice to a database both for the Single and Queue version of the data collection.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseProjectTest {
    private final String repo1 = "https://github.com/refactoring-ai-testing/toyrepo-r1.git";
    private final String repo2 = "https://github.com/refactoring-ai-testing/toyrepo-r2.git";

    @Inject
    AppBean appBean;

    private Path outputDir;
    private String tmpDir;

    @BeforeAll
    private void initTests() throws InterruptedException, IOException {
        resetTests();
        outputDir = Files.createTempDirectory(null);
        tmpDir = createTmpDir();
    }

    @AfterEach
    void resetTests() {
        deleteProject(repo1);
        deleteProject(repo2);
    }

    @AfterAll
    protected void cleanTests() throws IOException, InterruptedException {
        deleteProject(repo1);
        deleteProject(repo2);
        FileUtils.deleteDirectory(new File(tmpDir));
        FileUtils.deleteDirectory(outputDir.toFile());
    }

    @Transactional
    protected void deleteProject(String repo) {
        try {

            List<Project> projects = Project.list("gitUrl", repo);

            for (Project project : projects) {
                project.delete();
            }

        } catch (Exception e) {
            System.out.println("Could not delete the project before starting the test");
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * Test if two different projects are processed.
     */
    @Test
    public void different() throws Exception {
        appBean.run("repo1", repo1, outputDir, Files.createTempDirectory(null), false);
        appBean.run("repo2", repo2, outputDir, Files.createTempDirectory(null), false);
    }

    /*
     * Test if the same project is not processed twice.
     */
    @Test
    public void twice() throws Exception {
        appBean.run("repo1", repo1, outputDir, Files.createTempDirectory(null), false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            appBean.run("repo1", repo1, outputDir, Files.createTempDirectory(null), false);
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        Assertions.assertTrue(passedMessage.contains(expectedMessage));
    }

    /*
     * Test if the same project is not processed twice. TODO: check why it is
     * failing. Probably because if you run all the tests together, one of the repos
     * might be there already...
     */
    @Test
    @Disabled
    public void alternating() throws Exception {
        appBean.run("repo1", repo1, outputDir, Files.createTempDirectory(null), false);
        appBean.run("repo2", repo2, outputDir, Files.createTempDirectory(null), false);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            appBean.run("repo1", repo1, outputDir, Files.createTempDirectory(null), false);
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        Assertions.assertTrue(passedMessage.contains(expectedMessage));
    }
}