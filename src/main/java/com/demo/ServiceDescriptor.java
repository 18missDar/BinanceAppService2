package com.demo;


import lombok.*;

@Data
@Builder
@NoArgsConstructor //If there's @Builder with @Data in class then Lombok doesnt generate no args constructor
@AllArgsConstructor
@ToString
public class ServiceDescriptor {

    private String id;

    private String host;

    private int port;

    private String datasource_url;

    private String datasource_username;

    private String datasource_password;
}
