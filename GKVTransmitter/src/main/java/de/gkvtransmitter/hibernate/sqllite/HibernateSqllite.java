package de.gkvtransmitter.hibernate.sqllite;

import org.hibernate.SessionFactory;

import de.gkvtransmitter.util.HibernateUtil;

public class HibernateSqllite {
    private SessionFactory sf = HibernateUtil.getSessionFactory();
    private HibernateSqllite hbsqli;

    public HibernateSqllite getInstance() {
        if (this.hbsqli == null) {
            this.hbsqli = new HibernateSqllite();
        }
        return hbsqli;
    }

}
