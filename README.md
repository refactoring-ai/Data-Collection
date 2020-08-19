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

### Compiling and building the tool
To compile: Use Maven: `mvn clean compile`. Or just import it via IntelliJ; it will know what to do.

To generate executable jars, run `mvn clean package -DskipTests`

If you want to export a jar file and run it somewhere else, just do `mvn clean package`. A .jar file will be created under the `target/` folder. You can use this jar to run the tool manually.

To run the tests please run a local mariaDB database instance, for details see `src/main/test/java/integration/DataBaseInfo for details`.

## Running via Docker

The **recommend** **way** to use this tool. 

The data collection tool can be executed via Docker containers. It should be as easy as:

1. Move the built jar with dependencies from target dir into the lib dir: `mv target/data-collection-0.0.1-SNAPSHOT-jar-with-dependencies.jar lib/`
2. Run the data-collection: `sudo ./run-data-collection.sh projects/projects-final.csv 4`

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
After building you can use the jar at `target/data-collection-RunImportStandalone-jar-with-dependencies.jar`. usage is as follows:

```
Usage: java -jar data-collection-RunImportStandalone-jar-with-dependencies.jar 
      [options] some-input-file.csv
  Options:
    -c, --connection-pool
      The connection pooling library used. currently supported values are 
      MYSQL, MARIADB or POSTGRESQL
      Default: HIKARICP
    -d, --db-engine
      The database engine being used, currently supported values are MYSQL, 
      MARIADB or POSTGRESQL
      Default: MARIADB
    -h, --db-host
      Host IP or address of database
      Default: 127.0.0.1
    -n, --db-name
      Name of the database to use
      Default: refactoringdb
    -p, --db-password
      Database password
      Default: pass
    -P, --db-port
      Port of database
    -u, --db-user
      Database user
      Default: <your user name>
    -b, --hbm2ddl
      The hibernate.hbm2ddl.auto in the database connection
      Default: create
    --help
	  Prints this help message.
    -r, --repo-clone-location
      Repo clone location
      Default: /tmp/repos
    -s, --store-source-code
      Store the source code of the metrics
      Default: false
    -t, thread-amount
      Amount of threads to run in parallel.
      available 
      Default: <your-available-cores>
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
