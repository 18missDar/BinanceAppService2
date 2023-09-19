package com.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ForServiceDescriptorService {

    private Map<String, ServiceDescriptor> serviceDescriptorMap;

    public void loadOutputFoldersDescriptors() {
        final File outputFoldersConfigurationFile = new File("src/main/resources/services.yml");

        try (FileInputStream in = new FileInputStream(outputFoldersConfigurationFile)) {
            Yaml yaml = YamlReaderCreator.create();

            ServiceConfiguration serviceConfiguration = parseServiceConfiguration(yaml.load(in));

            if (serviceConfiguration != null) {
                this.serviceDescriptorMap = readServiceDescriptors(serviceConfiguration.getAllServices());
            } else {
                System.out.println("services are empty, check service.yml");
            }
        } catch (YAMLException exception) {
            System.out.println(exception.getMessage());
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private ServiceConfiguration parseServiceConfiguration(Map<String, List<Map<String, Object>>> config){
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setAllServices(new ArrayList<>());
        for (Map<String, Object> descriptorConfig : config.get("services")) {
            serviceConfiguration.getAllServices().add(ServiceDescriptor.builder()
                    .id((String) descriptorConfig.get("id"))
                    .host((String) descriptorConfig.get("host"))
                    .port((Integer) descriptorConfig.get("port"))
                    .datasource_url((String) descriptorConfig.get("datasource_url"))
                    .datasource_username((String) descriptorConfig.get("datasource_username"))
                    .datasource_password((String) descriptorConfig.get("datasource_password"))
                    .build());
        }
        return serviceConfiguration;
    }

    private Map<String, ServiceDescriptor> readServiceDescriptors(List<ServiceDescriptor> serviceDescriptors){
        if (!CollectionUtils.isEmpty(serviceDescriptors)){
            final Map<String, ServiceDescriptor> descriptors = new TreeMap<>();
            for (ServiceDescriptor serviceDescriptor: serviceDescriptors){
                if (serviceDescriptor.getId() != null)
                    descriptors.put(serviceDescriptor.getId(), serviceDescriptor);
            }
            return descriptors;
        }
        return Collections.emptyMap();
    }

    public ServiceDescriptor getService(String idService){
        if (serviceDescriptorMap.containsKey(idService))
            return serviceDescriptorMap.get(idService);
        else return null;
    }

}
