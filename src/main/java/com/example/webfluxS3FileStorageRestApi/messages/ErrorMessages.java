package com.example.webfluxS3FileStorageRestApi.messages;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static class Database {
        public static final String ERR_NO_DATABASE_FOUND = "No database found!";
        public static final String ERR_DATABASE_CREATION_FAILED = "Database creation failed!";
        public static final String ERR_DATABASE_INIT_FAILED = "Database initialization failed!";
    }

    public static final String ERR_ACCESS_DENIED = "Access denied";
    public static final String ERR_INVALID_AUTHENTICATION = "Invalid Authentication";

    public static class Files {
        public static final String ERR_FIND_FILE_WITH_ID = "Error find file with ID {}: {}";
        public static final String ERR_FILE_WITH_ID_NOT_FOUND = "File with ID = '%s' not found";
        public static final String ERR_FIND_ALL_FILES = "Error find all files: {}";
        public static final String ERR_FIND_ALL_FILES_BY_USER_ID = "Error find all files by user ID: {}";
        public static final String ERR_UPDATING_FILE_WITH_ID = "Error updating file with ID {}: {}";
        public static final String ERR_DELETING_ALL_FILES = "Error deleting all files: {}";
        public static final String ERR_DELETING_ALL_FILES_WITH_USER_ID = "Error deleting all files with user ID {}: {}";
        public static final String ERR_DELETING_FILE_WITH_ID = "Error deleting file with ID {}: {}";
    }

    public static class Events {
        public static final String ERR_FIND_EVENT_WITH_ID = "Error find event with ID {}: {}";
        public static final String ERR_EVENT_WITH_ID_NOT_FOUND = "Event with ID = '%s' not found";
        public static final String ERR_EVENT_NOT_FOUND = "Event not found";
        public static final String ERR_UPDATING_EVENT_WITH_ID = "Error updating event with ID {}: {}";
        public static final String ERR_DELETING_EVENT_WITH_ID = "Error updating event with ID {}: {}";
        public static final String ERR_FIND_ALL_EVENTS = "Error find all events: {}";
        public static final String ERR_FIND_ALL_EVENTS_BY_USER_ID = "Error find all events by user ID: {}";
        public static final String ERR_DELETING_ALL_EVENTS_WITH_USER_ID = "Error deleting all events with user ID: {}";
        public static final String ERR_DELETING_ALL_EVENTS = "Error deleting all events: {}";
    }

    public static class Users {
        public static final String ERR_USERNAME_ALREADY_TAKEN = "Username already taken";
        public static final String ERR_FIND_USER_WITH_ID = "Error find user with ID {}: {}";
        public static final String ERR_USER_WITH_ID_NOT_FOUND = "User with ID = '%s' not found";
        public static final String ERR_USER_WITH_USERNAME_NOT_FOUND = "User with username = '%s' not found";
        public static final String ERR_FIND_USER_WITH_USERNAME = "Error find user with username {}: {}";
        public static final String ERR_UPDATING_USER_WITH_ID = "Error updating user with ID {}: {}";
        public static final String ERR_DELETING_USER_WITH_ID = "Error deleting user with ID {}: {}";;
        public static final String ERR_DELETING_ALL_USERS = "Error deleting all users: {}";
    }

    public static class FileStorage {
        public static final String ERR_FILE_UPLOAD_FAILED = "File upload failed: ";
        public static final String ERR_UPLOADING_FILE_WITH_FILENAME_AND_USER_ID = "Error uploading file with filename and user ID: {}, {}, {}";
        public static final String ERR_DOWNLOADING_FILE_WITH_FILENAME = "Error downloading file with filename: {}, {}";

        public static final String ERR_FILE_NOT_FOUND_IN_S_3 = "File not found in S3: {}";
        public static final String ERR_CREATE_TEMP_DIRECTORY_FAILED = "Create temp directory failed: ";
        public static final String ERR_FILE_UPLOAD_TO_S_3_FAILED = "File upload to S3 failed: ";

        public static final String ERR_FILE_DOWNLOADED_FROM_S_3_FAILED = "File downloaded from S3 failed: ";

    }
}
