
const archiveFullName = (archive) => {
    const names = {
        'ffn': 'fanfiction.net',
        'ao3': 'Archive of Our Own'
    };
    return names[archive.toLowerCase()];
};

const archiveName = (archive) => {
    const names = {
        'ffn': 'ff.net',
        'ao3': 'AO3'
    };
    return names[archive.toLowerCase()];
};

const statusClass = (status) => {
    switch (status.code.toLowerCase()) {
        case 'cancelled':
        case 'completed':
        case 'errored':
        case 'fetching':
        case 'parsing':
        case 'pending':
            return status.code.toLowerCase();
        default:
            console.log(`Unknown status code: ${status.code}`);
            return 'bg-danger';
    }
};

const statusName = (status) => {
    const names = {
        'cancelled': () => 'Cancelled',
        'completed': () => 'Completed',
        'fetching': () => 'Fetching',
        'parsing': () => 'Parsing',
        'pending': () => 'Pending',
        'errored': (type) => {
            switch (type?.toLowerCase()) {
                case 'page_unavailable': return 'Unavailable';
                case 'story_inaccessible': return 'Inaccessible';
                case 'markup_unexpected': return 'Bad markup';
                default: return 'Unknown error';
            }
        }
    };
    return names[status.code.toLowerCase()](status.type);
};

export {
    archiveFullName,
    archiveName,
    statusClass,
    statusName
};
