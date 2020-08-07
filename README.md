# Machine Learning for Software refactoring
[![Build Status](https://travis-ci.org/refactoring-ai/Data-Collection.svg?branch=master)](https://travis-ci.org/refactoring-ai/Data-Collection)

This repository contains the data-collection part on the use of machine learning methods to recommend software refactoring.

It currently contains the following projects:

* `data-collection`: The java tool that collects **refactoring** and **non-refactoring** instances from **java source code** that are later used to **train** the ML algorithms with a large variety of metrics.

## Paper and appendix 

* The paper can be found here: https://arxiv.org/abs/2001.03338
* The raw dataset can be found here: https://zenodo.org/record/3547639
* The appendix with our full results can be found here: https://zenodo.org/record/3583980 

## The data collection tool

### Dependencies

* Java 11, or higher
* Maven

**Optional**

* [Docker](https://docs.docker.com/engine/install/)
* [Docker-compose](https://docs.docker.com/compose/install/)

### Compiling the tool

Use Maven: `mvn clean compile`. Or just import it via IntelliJ; it will know what to do.

If you want to export a jar file and run it somewhere else, just do `mvn clean package`. A .jar file will be created under the `target/` folder. You can use this jar to run the tool manually.

To run the tests please run a local mariaDB database instance, for details see `src/main/test/java/integration/DataBaseInfor for details`.

## Running via Docker

The **recommend** **way** to use this tool. 

The data collection tool can be executed via Docker containers. It should be as easy as:

1. Clone the project: `git clone https://github.com/refactoring-ai/Data-Collection.git`
2. Build the project: `mvn clean package -DskipTests`
3. Move the built jar with dependencies from target dir into the lib dir: `mv target/data-collection-0.0.1-SNAPSHOT-jar-with-dependencies.jar lib/`
4. Run the data-collection: `sudo ./run-data-collection.sh projects/projects-final.csv 4`

**Configurations** can be done with the following **arguments**:

 1. [FILE_TO_IMPORT] - Csv file with all projects, see projects dir for examples.
 1. [Worker_Count] - Number of concurrent workers for the data collection, executing the `RunQueue` class.

 * **Optional**: 
   1. [DB_URL] - fully qualified url to a custom database, e.g. `jdbc:mysql://db:3306/refactoringdb`
   1. [DB_USER] - user name for the custom database
   1. [DB_PWD] - password for the custom database

* The default database is a containerized MySQL database (mariaDB), y can directly access it via localhost:3308, root, refactoringdb. You can change it in `docker-compose_db.yml->db` and `docker-compose.yml->worker->environment->REF_URL`, `REF_USER`, and `REF_PWD`.
* The default MySQL database, the RabbitMQ queue, and the source code are all stored in the `data-collection/volumes` folder. Feel free to change where the volumes are stored.
* The configurations of the workers (basically the same as defined in the manual execution) can be defined in `docker-compose.yml->worker->environment`.
* `http://localhost:15672` takes you to the RabbitMQ admin (user: guest, pwd:guest) and `localhost:8080` takes you to adminer, a simple DB interface.
  Feel free to start as many workers as you want and/or your infrastructure enables you!

_Tip:_ If you are restarting everything, make sure to not import the projects again. Otherwise, you will have duplicated entries. Simply leave the file name blank in `import -> environment -> FILE_TO_IMPORT`.

### Running in a manual way

You can run the data collection by simply running the `RunSingleProject.java` class. This class contains a program that requires the following parameters, in this order:

1. _The dataset name_: A hard-coded string with the name of the dataset (e.g., "apache", "fdroid"). This information appears in the generated data later on, so that you can use it as a filter.

1. _The git URL_: The git url of the project to be analyzed. Your local machine must have all the permissions to clone it (i.e., _git clone url_ should work). Cloning will happen in a temporary directory.

1. _Storage path_: The directory where the tool is going to store the source code before and after the refactoring. This step is important if you plan to do later analysis on the refactored files. The directory structure basically contains the hash of the refactoring, as well as the file before and after. The name of the file also contains the refactoring it suffered, to facilitate parsing. For more details on the name of the file, see our implementation.

1. _Database URL_: JDBC URL that points to your MySQL. The database must exist and be empty. The tool will create the required tables.

1. _Database user_: Database user.

1. _Database password_: Database password. 

1. _Store full source code?_: True if you want to store the source code before and after in the storage path.

These parameters can be passed via command-line, if you exported a JAR file. 
Example:

```
java -jar refactoring.jar <dataset> <git-url> <output-path> <database-url> <database-user> <database-password> <k-threshold>
```

## Database Clean-up

The enormous variety refactoring types, projects and programming styles in the mined repositories can lead to various issues with the data. Therefore, we explain two common problems and potential solutions here.

**!MAKE A COPY OF YOUR DATA BEFORE ALTERING IT!**

### Remove unfinished projects

The data-collection tool can fail due to various reasons, e.g. OutOfMemoryErrors, unhandled Exceptions in the mining phase, in will thus not finish all projects. If you want to rerun the data-collection or remove unfinished projects for other reasons, you can do this with the following commands:

```mysql
/*
Remove refactoring and stable instances, before the projects.
DELETE
	RefactoringCommit
FROM
	RefactoringCommit
	INNER JOIN 
		CommitMetaData ON RefactoringCommit.commitmetadata_id = CommitMetaData.id
	INNER JOIN 
		ClassMetric ON refactoringcommit.classMetrics_id = ClassMetric.id
	LEFT JOIN 
		ProcessMetrics ON refactoringcommit.processMetrics_id = ProcessMetrics.id
	LEFT JOIN 
		FieldMetric ON refactoringcommit.fieldMetrics_id = FieldMetric.id
	LEFT JOIN 
		MethodMetric ON refactoringcommit.methodMetrics_id = MethodMetric.id
	LEFT JOIN 
		VariableMetric ON refactoringcommit.variableMetrics_id = VariableMetric.id
WHERE
	RefactoringCommit.project_id IN 
	(SELECT id FROM project WHERE finishedDate IS NULL);

DELETE
	StableCommit
FROM
	StableCommit
	INNER JOIN 
		CommitMetaData ON StableCommit.commitmetadata_id = CommitMetaData.id
	INNER JOIN 
		ClassMetric ON StableCommit.classMetrics_id = ClassMetric.id
	LEFT JOIN 
		ProcessMetrics ON StableCommit.processMetrics_id = ProcessMetrics.id
	LEFT JOIN 
		FieldMetric ON StableCommit.fieldMetrics_id = FieldMetric.id
	LEFT JOIN 
		MethodMetric ON StableCommit.methodMetrics_id = MethodMetric.id
	LEFT JOIN 
		VariableMetric ON StableCommit.variableMetrics_id = VariableMetric.id
WHERE
	StableCommit.project_id IN 
	(SELECT id FROM project WHERE finishedDate IS NULL);
	
DELETE
	project
FROM 
	project 
WHERE 
	finishedDate IS NULL;
*/
```

### Invalid Refactorings

Refactoring-miner is a great tool and quite stable, but not perfect. Thus, in some occasions it detects enormous numbers of refactorings for single class files, these seem to be in-correct and should be marked in-valid.

```
/*
Mark all in-valid refactorings in the RefactoringCommit table. This allows you manually inspect potentially in-valid refactorings and decide how to handle them.
In the last line you specify the threshold of refactorings on the same commit and the same class file to be considered in-valid.
*/

UPDATE 
	RefactoringCommit
SET 
	isValid = FALSE
WHERE
	commitMetaData_id IN
	(SELECT Distinct
		commitMetaData_id
	FROM
		RefactoringCommit
	GROUP BY
		commitMetaData_id, className
	HAVING
		COUNT(className) >= 50);
```

## Authors

This project was initially envisioned by Maurício Aniche, Erick Galante Maziero, Rafael Durelli, and Vinicius Durelli.

## License

This project is licensed under the MIT license.
