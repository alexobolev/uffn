package fi.sobolev.uffn.common.data


enum class Archive {
    AO3, FFN;
}

enum class Rating {
    K, T, M, E;
}

enum class UploadStatus {
    PENDING, FETCHING, COMPLETED, ERRORED, CANCELLED;
}

enum class UploadError {
    PAGE_UNAVAILABLE, STORY_INACCESSIBLE, MARKUP_UNEXPECTED, ERROR_FREEFORM;
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR;
}
