package com.nexblocks.authguard.dal.model;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

public class H2 {
    private final List<Class<?>> mappedClasses;

    private Session session;
    private EntityManager entityManager;

    public H2() {
        this.mappedClasses = new ArrayList<>();
    }

    public H2 withMappedClass(final Class<?> mappedClass) {
        this.mappedClasses.add(mappedClass);
        return this;
    }

    public H2 prepare() {
        final Configuration configuration = new Configuration();

        mappedClasses.forEach(configuration::addAnnotatedClass);

        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem:test_db");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create");

        final SessionFactory sessionFactory = configuration.buildSessionFactory();

        session = sessionFactory.openSession();
        entityManager = session.getEntityManagerFactory().createEntityManager();

        return this;
    }

    public Session getSession() {
        return session;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
