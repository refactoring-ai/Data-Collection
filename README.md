# Machine Learning for Software refactoring
[![Build Status](https://travis-ci.org/refactoring-ai/predicting-refactoring-ml.svg?branch=master)](https://travis-ci.org/refactoring-ai/predicting-refactoring-ml)

This repository contains the data collection part on the use
of machine learning methods to recommend software refactoring.

It currently contains the following projects:

* `data-collection`: The java tool that collects refactoring and non-refactorings instances that are later used to train the ML algorithms. A docker script is available.

* `machine-learning`: The python scripts that train the different ML algorithms.

## Paper and appendix 

* The paper can be found here: https://arxiv.org/abs/2001.03338
* The raw dataset can be found here: https://zenodo.org/record/3547639
* The appendix with our full results can be found here: https://zenodo.org/record/3583980 


## The data collection tool

### Compiling the tool

Use Maven: `mvn clean compile`. Or just import it via IntelliJ; it will know what to do.

If you want to export a jar file and run it somewhere else, just do `mvn clean package`. A .jar file will be created under the `target/` folder. You can use this jar to run the tool manually.

To run the tests please run a local mariaDB database instance, for details see `src/main/test/java/integration/DataBaseInfor for details`.

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

### Running via Docker

The data collection tool can be executed via Docker containers. It should be as easy as:

```
git clone https://github.com/mauricioaniche/predicting-refactoring-ml.git
cd predicting-refactoring-ml
sudo ./run-data-collection.sh projects-final.csv 4
```

Configurations can be done with the following **arguments**:
 1. [FILE_TO_IMPORT] - Csv file with all projects
 1. [Worker_Count] - Number of concurrent worker for the data collection, running the `RunQueue` class
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

### Cleaning up the final database

When running in scale, e.g., in thousands of projects, some projects might fail due to e.g., AST parser failure. Although the app tries to remove problematic rows, if the process dies completely (e.g., maybe out of memory in your machine), partially processed projects will be in the database. 

We use the following queries to remove half-baked projects completely from our database:

```
delete from RefactoringCommit where project_id in (select id from project where finishedDate is null);
delete from StableCommit where project_id in (select id from project where finishedDate is null);
delete from project where finishedDate is null;
```

## Authors

This project was initially envisioned by Maurício Aniche, Erick Galante Maziero, Rafael Durelli, and Vinicius Durelli.

## License

This project is licensed under the MIT license.
