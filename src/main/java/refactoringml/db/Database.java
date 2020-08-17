package refactoringml.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

public class Database {
	private final Session session;

	private static final Logger log = LogManager.getLogger(Database.class);

	public Database(Session session) {
		this.session = session;
	}

	public void close() {
		session.close();
	}

	public Transaction beginTransaction() {
		return session.beginTransaction();
	}

	public void persist(Object obj) {
		session.persist(obj);
	}

	public <T> T merge(T toMerge) {
		return (T) session.merge(toMerge);
	}

	public void persistComplete(Object toPersist) {
		Transaction t = beginTransaction();
		persist(toPersist);
		t.commit();
	}

	public <T> T mergeComplete(T toMerge) {
		Transaction t = beginTransaction();
		T merged = merge(toMerge);
		t.commit();
		return merged;

	}

	public boolean projectExists(String gitUrl) {
		return !session.createQuery("from Project p where p.gitUrl = :gitUrl").setParameter("gitUrl", gitUrl).list()
				.isEmpty();
	}

	public long findAllRefactoringCommits(long projectId) {
		return findAllInstances("RefactoringCommit", projectId);
	}

	public long findAllStableCommits(long projectId) {
		return findAllInstances("StableCommit", projectId);
	}

	private long findAllInstances(String instance, long projectId) {
		String query = "Select count(*) From " + instance + " where " + instance + ".project_id = " + projectId;
		Object result = session.createSQLQuery(query).getSingleResult();
		return Long.parseLong(result.toString());
	}

	public long findAllStableCommits(long projectId, int level) {
		String query = "Select count(*) From StableCommit where StableCommit.project_id = " + projectId
				+ " AND StableCommit.commitThreshold = " + level;
		Object result = session.createSQLQuery(query).getSingleResult();
		return Long.parseLong(result.toString());
	}

	// safely rollback a transaction with the db
	public void rollback(String logExtension) {
		// this session object itself should never be null, thus we don't check for it
		// nothing to do in this case, recovering the session or transaction is to much
		// effort and the db takes care of a failed transaction
		if (!session.isOpen()) {
			log.error("Session was already closed during attempted rollback: Doing Nothing." + logExtension);
			return;
		}

		if (!session.isConnected()) {
			try {
				Connection connection = session.getSessionFactory().getSessionFactoryOptions().getServiceRegistry()
						.getService(ConnectionProvider.class).getConnection();
				session.reconnect(connection);
			} catch (SQLException e) {
				log.error("Failed to reconnect session object." + logExtension, e);
			}
		}

		// standard case for a rollback
		if (session.isConnected() && session.getTransaction() != null) {
			try {
				session.getTransaction().rollback();
			} catch (TransactionException e) {
				log.error("Failed to rollback session: " + session.toString() + logExtension, e);
			}
		} else {
			// other cases:
			// 1. not connected to the DB : we could raise an error here, because something
			// is probably wrong with the db
			// 2. connected but no transaction object : nothing to do
			log.error("Session is in a bad state: " + session.toString() + logExtension);
		}
	}

	public CommitMetaData loadCommitMetaData(long id) {
		return session.get(CommitMetaData.class, id);
	}
}