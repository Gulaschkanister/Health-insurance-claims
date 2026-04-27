package de.gkvtransmitter.hibernate.sqllite;

import org.hibernate.SessionFactory;

import de.gkvtransmitter.util.HibernateUtil;

public class HibernateSqllite {
    private SessionFactory sf = HibernateUtil.getSessionFactory();

    public void initSqlite() {
        HibernateSqllite hsql = new HibernateSqllite();
    }
}
