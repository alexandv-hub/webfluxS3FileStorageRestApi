package com.example.webfluxS3FileStorageRestApi.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static com.example.webfluxS3FileStorageRestApi.messages.ErrorMessages.Database.*;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.Database.INFO_CONNECTED_TO_MYSQL_SERVER_SUCCESSFULLY;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.Database.INFO_DATABASE_SUCCESSFULLY_CREATED;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.INFO_CONNECTING_TO_MY_SQL_SERVER;
import static com.example.webfluxS3FileStorageRestApi.messages.InfoMessages.INFO_STARTING_CREATE_NEW_DATABASE;

@Slf4j
@Component
public class DatabaseInitializer {

    public static final String SQL_SHOW_DATABASES = "SHOW DATABASES;";
    public static final String SQL_CREATE_DATABASE = "CREATE DATABASE ";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.name}")
    private String dbName;

    @Value("${spring.r2dbc.username}")
    private String dbUsername;

    @Value("${spring.r2dbc.password}")
    private String dbPassword;

    @PostConstruct
    public void initialize() {
        log.info(INFO_CONNECTING_TO_MY_SQL_SERVER);

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery(SQL_SHOW_DATABASES);
            boolean dbExists = false;
            while (resultSet.next()) {
                if (dbName.equals(resultSet.getString(1))) {
                    dbExists = true;
                    log.info(INFO_CONNECTED_TO_MYSQL_SERVER_SUCCESSFULLY);
                    break;
                }
            }

            if (!dbExists) {
                try {
                    log.error(ERR_NO_DATABASE_FOUND);
                    log.info(INFO_STARTING_CREATE_NEW_DATABASE);
                    statement.executeUpdate(SQL_CREATE_DATABASE + dbName);
                    log.info(INFO_DATABASE_SUCCESSFULLY_CREATED);
                } catch (Exception e) {
                    log.error(ERR_DATABASE_CREATION_FAILED);
                    e.printStackTrace(System.out);
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error(ERR_DATABASE_INIT_FAILED);
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}

