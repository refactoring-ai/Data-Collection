package refactoringml.db;

import java.util.Optional;
import java.util.Properties;

import com.zaxxer.hikari.HikariDataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public abstract class HibernateConfig {

	protected final Configuration configuration = new Configuration();

	public static SessionFactory getSessionFactory(String url, String user, String pwd) {
		return getSessionFactory(url, user, pwd, false);
	}

	public static SessionFactory getSessionFactory(String url, String user, String pwd, boolean drop) {
		HibernateConfig conf = new C3p0HibernateConfig();
		conf.setDatabaseEngineSpecificProperties(DatabaseEngine.MYSQL.driverClass, url,
				DatabaseEngine.MYSQL.dialectClass);
		return conf.buildSessionFactory(user, pwd, drop ? "create-drop" : "update");
	}

	public static SessionFactory getSessionFactory(String host, Optional<Short> port, String databaseName, String user,
			String pwd, String hbm2ddlStrategy, DatabaseEngine dbEngine, ConnectionPoolType cpType) {

		HibernateConfig conf = cpType.toHibernateConfig();
		String url = String.format("jdbc:%s://%s:%d/%s", dbEngine.protocol, host, port.orElse(dbEngine.defaultPort), databaseName);

		conf.setDatabaseEngineSpecificProperties(dbEngine.driverClass, url, dbEngine.dialectClass);
		return conf.buildSessionFactory(user, pwd, hbm2ddlStrategy);
	}

	protected abstract void setCommonProperties(String user, String pwd);

	protected abstract void setDatabaseEngineSpecificProperties(String driverClassName, String url,
			String dialectClassName);

	protected abstract ServiceRegistry buildServiceRegistry();

	private SessionFactory buildSessionFactory(String user, String pwd, String hbm2ddlStrategy) {
		setCommonProperties(user, pwd);
		configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, hbm2ddlStrategy);

		configuration.addAnnotatedClass(RefactoringCommit.class);
		configuration.addAnnotatedClass(StableCommit.class);
		configuration.addAnnotatedClass(Project.class);

		// features of Instance for DB normalization
		configuration.addAnnotatedClass(CommitMetaData.class);
		configuration.addAnnotatedClass(ClassMetric.class);
		configuration.addAnnotatedClass(MethodMetric.class);
		configuration.addAnnotatedClass(VariableMetric.class);
		configuration.addAnnotatedClass(FieldMetric.class);
		configuration.addAnnotatedClass(ProcessMetrics.class);

		return configuration.buildSessionFactory(buildServiceRegistry());
	}

	private static class HikariHibernateConfig extends HibernateConfig {

		private final HikariDataSource ds = new HikariDataSource();

		@Override
		protected void setCommonProperties(String user, String pwd) {
			ds.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() + 4);
			ds.addDataSourceProperty("user", user);
			ds.addDataSourceProperty("password", pwd);
			ds.setAutoCommit(false);
		}

		@Override
		protected void setDatabaseEngineSpecificProperties(String driverClassName, String url,
				String dialectClassName) {
			ds.setDriverClassName(driverClassName);
			ds.setJdbcUrl(url);
			configuration.setProperty(AvailableSettings.DIALECT, dialectClassName);
		}

		@Override
		protected ServiceRegistry buildServiceRegistry() {
			return new StandardServiceRegistryBuilder().applySettings(configuration.getProperties())
					.applySetting(AvailableSettings.DATASOURCE, ds).build();
		}

	}

	private static class C3p0HibernateConfig extends HibernateConfig {

		private final Properties settings = new Properties();

		@Override
		protected void setCommonProperties(String user, String pwd) {

			settings.put(AvailableSettings.USER, user);
			settings.put(AvailableSettings.PASS, pwd);

			settings.put("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
			settings.put("hibernate.c3p0.acquire_increment", 1);
			settings.put("hibernate.c3p0.idle_test_period", 60);
			settings.put("hibernate.c3p0.min_size", 1);
			settings.put("hibernate.c3p0.max_size", 5);
			settings.put("hibernate.c3p0.max_statements", 50);

			settings.put("hibernate.c3p0.timeout", 300);
			settings.put("hibernate.c3p0.maxConnectionAge", 3600);

			settings.put("hibernate.c3p0.maxIdleTimeExcessConnections", 3600);

			settings.put("hibernate.c3p0.acquireRetryAttempts", 10);
			settings.put("hibernate.c3p0.acquireRetryDelay", 60);

			settings.put("hibernate.c3p0.preferredTestQuery", "SELECT 1");
			settings.put("hibernate.c3p0.testConnectionOnCheckout", true);

			settings.put("hibernate.c3p0.autoCommitOnClose", false);
			settings.put("hibernate.c3p0.unreturnedConnectionTimeout", 300);

			configuration.setProperties(settings);
		}

		@Override
		protected void setDatabaseEngineSpecificProperties(String driverClassName, String url,
				String dialectClassName) {
			settings.put(AvailableSettings.DRIVER, driverClassName);
			settings.put(AvailableSettings.URL, url);

			settings.put(AvailableSettings.DIALECT, dialectClassName);
			settings.put(AvailableSettings.SHOW_SQL, "false");
		}

		@Override
		protected ServiceRegistry buildServiceRegistry() {
			return new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
		}

	}

	public enum DatabaseEngine {
		MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "org.hibernate.dialect.MySQL8Dialect", (short) 3306),
		MARIADB("mariadb", "org.mariadb.jdbc.Driver", "org.hibernate.dialect.MariaDBDialect", (short) 3306),
		POSTGRESQL("postgresql", "org.postgresql.Driver", "org.hibernate.dialect.PostgreSQL95Dialect", (short) 5432);

		private final String protocol;
		private final String driverClass;
		private final String dialectClass;
		private final short defaultPort;

		private DatabaseEngine(String protocol, String driverClass, String dialectClass, short defaultPort) {
			this.protocol = protocol;
			this.driverClass = driverClass;
			this.dialectClass = dialectClass;
			this.defaultPort = defaultPort;
		}
	}

	public enum ConnectionPoolType {
		C3P0, HIKARICP;

		public HibernateConfig toHibernateConfig() {
			switch (this) {
				case C3P0:
					return new C3p0HibernateConfig();
				case HIKARICP:
					return new HikariHibernateConfig();
			}
			throw new IllegalArgumentException("Not a supported ConnectionPoolType type: " + toString());
		}
	}
}
