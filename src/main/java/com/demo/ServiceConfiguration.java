package com.demo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceConfiguration {
    List<ServiceDescriptor> allServices;
}
