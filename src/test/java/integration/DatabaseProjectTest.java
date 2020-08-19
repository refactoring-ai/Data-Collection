package integration;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static refactoringml.util.FileUtils.createTmpDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;

/*
    Test if a project can be added twice to a database both for the Single and Queue version of the data collection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseProjectTest {
    private final String repo1 = "https://github.com/refactoring-ai-testing/toyrepo-r1.git";
    private final String repo2 = "https://github.com/refactoring-ai-testing/toyrepo-r2.git";

    private String outputDir;
    private String tmpDir;

    private SessionFactory sf;

    @BeforeAll
    private void initTests() throws InterruptedException {
        outputDir = createTmpDir();
        tmpDir = createTmpDir();

        sf = HibernateConfig.getSessionFactory(DataBaseInfo.URL, DataBaseInfo.USER, DataBaseInfo.PASSWORD, false);
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
        FileUtils.deleteDirectory(new File(outputDir));
    }

    protected void deleteProject(String repo1) {
        try {
            Session session = sf.openSession();

            List<Project> projects = (List<Project>) session.createQuery("from Project p where p.gitUrl = :gitUrl")
                    .setParameter("gitUrl", repo1).list();

            if (!projects.isEmpty()) {
                session.beginTransaction();

                session.createQuery("delete from RefactoringCommit y where project in :project")
                        .setParameter("project", projects).executeUpdate();
                session.createQuery("delete from StableCommit where project in :project")
                        .setParameter("project", projects).executeUpdate();

                projects.stream().forEach(session::delete);
                session.getTransaction().commit();
            }

            session.close();
        } catch (Exception e) {
            System.out.println("Could not delete the project before starting the test");
            e.printStackTrace();
        }
    }

    /*
     * Test if two different projects are processed.
     */
    @Test
    void different() throws Exception {
        assertNotNull(new App("repo1", repo1, outputDir, new Database(sf.openSession()), false).run());
        assertNotNull(new App("repo2", repo2, outputDir, new Database(sf.openSession()), false).run());
    }

    /*
     * Test if the same project is not processed twice.
     */
    @Test
    void twice() throws Exception {
        new App("repo1", repo1, outputDir, new Database(sf.openSession()), false).run();
        App app = new App("repo1", repo1, outputDir, new Database(sf.openSession()), false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            app.run();
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        assertTrue(passedMessage.contains(expectedMessage));
    }

    /*
     * Test if the same project is not processed twice.
     */
    @Test
    void alternating() throws Exception {
        new App("repo1", repo1, outputDir, new Database(sf.openSession()), false).run();
        new App("repo2", repo2, outputDir, new Database(sf.openSession()), false).run();
        App repeatedApp = new App("repo1", repo1, outputDir, new Database(sf.openSession()), false);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            repeatedApp.run();
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        assertTrue(passedMessage.contains(expectedMessage));
    }
}