<?php
namespace App\Entity;

enum Archive: string {
    case AO3 = 'AO3';
    case FFN = 'FFN';
}

enum Rating: string {
    case K = 'K';
    case T = 'T';
    case M = 'M';
    case E = 'E';
}

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
