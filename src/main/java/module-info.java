module pbouda.github.lang {

    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.web;
    requires spring.context;
    requires spring.core;

    requires java.net.http;

    requires org.apache.tomcat.embed.core;
    requires com.fasterxml.jackson.databind;
    requires jdk.incubator.concurrent;

    opens pbouda.github.lang;
}