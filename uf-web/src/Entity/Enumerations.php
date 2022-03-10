<?php
namespace App\Entity;

enum UploadStatus: string {
    case Pending = 'PENDING';
    case Fetching = 'FETCHING';
    case Completed = 'COMPLETED';
    case Errored = 'ERRORED';
    case Cancelled = 'CANCELLED';
}

enum UploadError: string {
    case PageUnavailable = "PAGE_UNAVAILABLE";
    case StoryInaccessible = "STORY_INACCESSIBLE";
    case MarkupUnexpected = "MARKUP_UNEXPECTED";
    case Freeform = "ERROR_FREEFORM";
}

enum LogLevel: string {
    case Debug = 'DEBUG';
    case Info = 'INFO';
    case Warn = 'WARN';
    case Error = 'ERROR';
}
