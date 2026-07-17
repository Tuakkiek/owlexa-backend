package com.owlexa.owlexabackend.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class FlywayDependsOnPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
            BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] dependsOn = bd.getDependsOn();
            if (dependsOn == null) {
                bd.setDependsOn("flyway");
            } else {
                boolean hasFlyway = false;
                for (String dep : dependsOn) {
                    if ("flyway".equals(dep)) {
                        hasFlyway = true;
                        break;
                    }
                }
                if (!hasFlyway) {
                    String[] newDependsOn = Arrays.copyOf(dependsOn, dependsOn.length + 1);
                    newDependsOn[newDependsOn.length - 1] = "flyway";
                    bd.setDependsOn(newDependsOn);
                }
            }
        }
    }
}
