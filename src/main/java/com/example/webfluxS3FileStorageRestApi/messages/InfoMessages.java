package com.example.webfluxS3FileStorageRestApi.messages;

public final class InfoMessages {

    private InfoMessages() {
    }

    public static class Database {
        public static final String INFO_CONNECTED_TO_MYSQL_SERVER_SUCCESSFULLY = "Connected to MySQL Server successfully...";
        public static final String INFO_DATABASE_SUCCESSFULLY_CREATED = "Database successfully created.";
    }

    public static final String INFO_CONNECTING_TO_MY_SQL_SERVER = "Connecting to MySQL Server...";
    public static final String INFO_STARTING_CREATE_NEW_DATABASE = "Starting create new database...";

    public static class Events {
        public static final String INFO_EVENT_FOUND_SUCCESSFULLY_WITH_ID = "Event found successfully with ID: {}";
        public static final String INFO_EVENT_UPDATED_SUCCESSFULLY_WITH_ID = "Event updated successfully with ID: {}";
        public static final String INFO_DELETING_EVENT_WITH_ID = "Deleting event with ID: {}";
        public static final String INFO_EVENT_DELETED_SUCCESSFULLY_WITH_ID = "Event deleted successfully with ID: {}";
        public static final String INFO_FIND_ALL_EVENTS_FINISHED_SUCCESSFULLY = "Find all events finished successfully";
        public static final String INFO_FIND_ALL_EVENTS_BY_USER_ID_FINISHED_SUCCESSFULLY = "Find all events by user ID finished successfully";
        public static final String INFO_EVENTS_DELETED_SUCCESSFULLY_WITH_USER_ID = "Events deleted successfully with user ID: {}";
        public static final String INFO_ALL_EVENTS_DELETED_SUCCESSFULLY = "All events deleted successfully";
    }

    public static class Files {
        public static final String INFO_FILE_FOUND_SUCCESSFULLY_WITH_ID = "File found successfully with ID: {}";
        public static final String INFO_FIND_ALL_FILES_FINISHED_SUCCESSFULLY = "Find all files finished successfully";
        public static final String INFO_FILE_UPDATED_SUCCESSFULLY_WITH_ID = "File updated successfully with ID: {}";
        public static final String INFO_DELETING_FILE_WITH_ID = "Deleting file with ID: {}";
        public static final String INFO_FILE_DELETED_SUCCESSFULLY_WITH_ID = "File deleted successfully with ID: {}";
        public static final String INFO_ALL_FILES_DELETED_SUCCESSFULLY = "All files deleted successfully";
        public static final String INFO_ALL_FILES_DELETED_SUCCESSFULLY1 = "All files deleted successfully";
    }

    public static class FileStorage {
        public static final String INFO_FILE_UPLOADED_SUCCESSFULLY_WITH_FILENAME = "File uploaded successfully with filename: {}";
        public static final String INFO_FILE_UPLOADED_SUCCESSFULLY_WITH_FILENAME_AND_USER_ID = "File uploaded successfully with filename and user ID: {}, {}";
        public static final String INFO_FILE_DOWNLOADED_SUCCESSFULLY_WITH_FILENAME = "File downloaded successfully with filename: {},";
    }

    public static class Users {
        public static final String INFO_IN_REGISTER_USER_USER_CREATED = "IN registerUser - user: {} created";
        public static final String INFO_FOUND_SUCCESSFULLY_USER_WITH_ID = "Found successfully User with ID: {}";
        public static final String INFO_USER_FOUND_SUCCESSFULLY_WITH_ID = "User found successfully with ID: {}";
        public static final String INFO_FOUND_SUCCESSFULLY_USER_WITH_USERNAME = "Found successfully User with username: {}";
        public static final String INFO_USER_UPDATED_SUCCESSFULLY_WITH_ID = "User updated successfully with ID: {}";
        public static final String INFO_DELETING_USER_WITH_ID = "Deleting user with ID: {}";
        public static final String INFO_FILE_DELETED_SUCCESSFULLY_WITH_ID = "File deleted successfully with ID: {}";
        public static final String INFO_ALL_USERS_DELETED_SUCCESSFULLY = "All users deleted successfully";
    }
}
