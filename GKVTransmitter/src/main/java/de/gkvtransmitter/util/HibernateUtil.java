package de.gkvtransmitter.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        try {
            StandardServiceRegistry stRegistry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml")
                    .build();
            Metadata meta = new MetadataSources(stRegistry).getMetadataBuilder().build();
            sessionFactory = meta.getSessionFactoryBuilder().build();
        } catch (Throwable th) {
            throw new ExceptionInInitializerError(th);
        }

    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
