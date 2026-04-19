package com.n11.oop.annotation;

import java.lang.annotation.Annotation;

@BilgiRuntime(bilgi = "Annotation Örnek",tarih="26.03.2024", yazar="ibrahim gökyar")

public class TestAnnotation {

    public static void main(String[] args) {

        Class<?> c;
        try
        {
            c = Class.forName("com.n11.oop.annotation.TestAnnotation");
            Annotation[] ann = c.getDeclaredAnnotations();
            for(Annotation a : ann)
            {
                Class<?> annType =  a.annotationType();
                System.out.println("Annotation tipi : " +annType);
            }

            BilgiRuntime  infoAnn = c.getAnnotation(BilgiRuntime.class);
            System.out.println("Bilgi : " + infoAnn.bilgi());
            System.out.println("Tarih : " + infoAnn.tarih());
            System.out.println("Yazar : " + infoAnn.yazar());


        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

    }
}
