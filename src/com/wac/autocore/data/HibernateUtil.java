package com.wac.autocore.data;


import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private static SessionFactory buildSessionFactory(){
        try {
            return new Configuration().configure().buildSessionFactory();
        }catch (Throwable ex) {
            System.err.println("Could not create SessionFactory: " + ex);
            throw new IllegalStateException("Could not connect to database.", ex);
        }
    }

    public static SessionFactory getSessionFactory(){
        if(sessionFactory == null){
            sessionFactory = buildSessionFactory();
        }

        return  sessionFactory;
    }

    public  static void shutDown(){
        if(sessionFactory !=null && !sessionFactory.isClosed()){
            sessionFactory.close();
        }
    }


}
