package uk.gov.moj.cpp.progression.activiti.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.activiti.cdi.CdiJtaProcessEngineConfiguration;
import org.activiti.cdi.spi.ProcessEngineLookup;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.asyncexecutor.ManagedAsyncJobExecutor;
import org.activiti.engine.impl.history.HistoryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public class ProgrammaticProcessEngineConfiguration implements ProcessEngineLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgrammaticProcessEngineConfiguration.class.getCanonicalName());

    private ProcessEngine processEngine;

    private ManagedThreadFactory managedThreadFactory;

    @Override
    public ProcessEngine getProcessEngine() {
        managedThreadFactory = getThreadFactory();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ManagedAsyncJobExecutor managedJobExecutor = new ManagedAsyncJobExecutor() {

            @Override
            protected void startExecutingAsyncJobs() {
                if (threadFactory == null) {
                    super.startExecutingAsyncJobs();
                } else {
                    if (threadPoolQueue == null) { threadPoolQueue = new ArrayBlockingQueue<>(queueSize); }
                    if (executorService == null) {
                        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, threadPoolQueue, threadFactory) {
                            @Override
                            protected void beforeExecute(final Thread t, final Runnable r) { t.setContextClassLoader(classLoader); }
                        };
                        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                        executorService = threadPoolExecutor;
                    }
                    startJobAcquisitionThread();
                }
            }
        };

        managedJobExecutor.setThreadFactory(managedThreadFactory);

        final CdiJtaProcessEngineConfiguration cdiJtaProcessEngineConfiguration = new CdiJtaProcessEngineConfiguration();
        cdiJtaProcessEngineConfiguration.setTransactionManager(getTransactionManager());
        processEngine = cdiJtaProcessEngineConfiguration.setDatabaseSchemaUpdate("true")
                .setDataSourceJndiName("java:/DS.progressionactiviti").setDatabaseType("postgres")
                .setHistoryLevel(HistoryLevel.NONE)
                .setTransactionsExternallyManaged(true).setJobExecutorActivate(true)
                .setAsyncExecutorEnabled(true)
                .setAsyncExecutorActivate(true)
                .setAsyncExecutor(managedJobExecutor)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .buildProcessEngine();
        return processEngine;
    }

    @Override
    public void ungetProcessEngine() {
        processEngine.close();
    }

    @Override
    public int getPrecedence() {
        return 0;
    }


    public ManagedThreadFactory getThreadFactory() {
        try {
            return (ManagedThreadFactory) new InitialContext().lookup("java:jboss/ee/concurrency/factory/default");
        } catch (final NamingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public TransactionManager getTransactionManager() {
        try {
            return (TransactionManager) new InitialContext().lookup("java:/TransactionManager");
        } catch (final NamingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
