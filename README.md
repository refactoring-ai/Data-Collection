# Machine Learning for Software refactoring
[![codecov](https://codecov.io/gh/refactoring-ai/Data-Collection/branch/master/graph/badge.svg)](https://codecov.io/gh/refactoring-ai/Data-Collection)

This repository contains the data-collection part on the use of machine learning methods to recommend software refactoring that collects **refactoring** and **non-refactoring** instances from **java source code** that are later used to **train** the ML algorithms with a large variety of metrics.

## Quickstart

1. Prepare a MariaDB instance and create a database with the name `refactoring_ai` (if you have docker a quick way is: `docker run -p 127.0.0.1:3306:3306 --name some-mariadb -e MYSQL_ROOT_PASSWORD=root -d mariadb` these are also the default credentials)
2. Build jar with dependencies: `./gradlew quarkusBuild`
3. Define the projects to mine refactorings in `input.csv`
4. Start mining: `java -jar java -jar build/datacollection-0.1.0-runner.jar`

## Configuration
Configuration can be done with environment variables. 
You can also create an .env file with the variables.
See .env.example for each variable and its explanation

## Paper and appendix 

* The paper can be found here: https://arxiv.org/abs/2001.03338
* The raw dataset can be found here: https://zenodo.org/record/3547639
* The appendix with our full results can be found here: https://zenodo.org/record/3583980 

## The data collection tool

### Dependencies

* Java 11, or higher


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
