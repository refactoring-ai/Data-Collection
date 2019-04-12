import mysql.connector
import pandas as pd
import hashlib
import os.path

import configparser


config = configparser.ConfigParser()
config.read(os.path.join(os.getcwd(),'config.ini'))

mydb = mysql.connector.connect(
  host=config['db']["ip"],
  user=config['db']["user"],
  passwd=config['db']["pwd"],
  database=config['db']["database"]
)

# this method executes the query and stores the result in a local cache.
# we do not want to re-execute large queries.
# derived from https://medium.com/gousto-engineering-techbrunch/hash-caching-query-results-in-python-2d00f8058252
def execute_query(sql_query):
    """
    Method to query data from Redshift and return pandas dataframe
    Parameters
    ----------
    sql_query : str
        saved SQL query
    Returns
    -------
    df_raw : DataFrame
        Pandas DataFrame with raw data resulting from query
    """

    # print("DEBUG: Query to run: " + sql_query)
    # Hash the query
    query_hash = hashlib.sha1(sql_query.encode()).hexdigest()

    # Create the filepath
    file_path = os.path.join("_cache","{}.csv".format(query_hash))

    # Read the file or execute query
    if os.path.exists(file_path):
        # print("DEBUG: query is cached")
        df_raw = pd.read_csv(file_path)
    else:
        try:
            df_raw = pd.read_sql(sql_query, con=mydb)
        except (KeyboardInterrupt, SystemExit):
            mydb.close()
        if not os.path.isdir("_cache"):
            os.makedirs("_cache")
        df_raw.to_csv(file_path, index=False)


    return df_raw