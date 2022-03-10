import React from "react"
import ReactDom from "react-dom";
import { Dropdown, Tab } from "bootstrap";



const archiveFullName = (archive) => {
    const names = {
        'ffn': 'fanfiction.net',
        'ao3': 'Archive of Our Own'
    };
    return names[archive];
};
const archiveName = (archive) => {
    const names = {
        'ffn': 'ff.net',
        'ao3': 'AO3'
    };
    return names[archive];
};
const statusClass = (status) => {
    switch (status.code) {
        case 'cancelled':
        case 'completed':
        case 'error':
        case 'fetching':
        case 'parsing':
        case 'pending':
            return status.code;
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
        'error': (type) => {
            switch (type) {
                case 'page_unavailable': return 'Unavailable';
                case 'story_inaccessible': return 'Inaccessible';
                case 'markup_unexpected': return 'Bad markup';
                default: return 'Unknown error';
            }
        }
    };
    return names[status.code](status.type);
};
const originLink = (origin) => {
    const domains = {
        'ffn': 'https://fanfiction.net/s/',
        'ao3': 'https://archiveofourown.org/work/'
    };
    return domains[origin.archive] + origin.ident.toString();
};



function UploadEntryHeader(props) {
    return (
        <header>
            <span className='upload-title'><span>{ props.title }</span></span>
            <span className='upload-meta'>{ props.meta }</span>
        </header>
    );
}

function UploadEntryBadge(props) {
    const name = statusName(props.status);
    return (<span className="badge">{ name } { props.status.extra }</span>);
}

function UploadEntryButton(props) {
    const classDisabled = props.enabled ? '' : 'disabled';
    const linkTarget = props.newTab ? '_blank' : '_self';
    return (
        <a href={props.href} className={ `btn btn-sm ${classDisabled}` } target={linkTarget} {...props.extraAttrs}>
            <i className={ `bi bi-${props.iconClass}` } />
        </a>
    );
}

UploadEntryButton.defaultProps = {
    enabled: true,
    extraAttrs: [],
    href: '#',
    iconClass: 'exclamation-circle',
    newTab: false
};

function UploadEntryFooter(props) {
    const canRetry = props.item.status.code === 'error';
    const canDelete = props.item.status.code !== 'fetching';

    return (
        <footer>
            <div className="status">
                <UploadEntryBadge status={ props.item.status } />
            </div>
            <div className="buttons">
                <UploadEntryButton iconClass="arrow-clockwise" enabled={canRetry} />
                <UploadEntryButton iconClass="globe" href={ originLink(props.item.origin) } newTab={true} />
                <UploadEntryButton iconClass="file-text" href={ '#' + props.id } extraAttrs={ { 'data-bs-toggle': 'collapse' } } />
                <UploadEntryButton iconClass="trash" enabled={canDelete} />
            </div>
        </footer>
    );
}

function UploadEntryLogs(props) {
    if (props.logs.length > 0) {
        return (
            <table className="table table-borderless table-sm table-hover table-dark small mb-0">
                <tbody>
                { props.logs.map((log, index) =>
                    <tr key={ index } className={ log.level + '-row' }>
                        <td>{ log.time }</td>
                        <td><span>{ log.level }</span></td>
                        <td dangerouslySetInnerHTML={{__html: log.message}}/>
                    </tr>
                ) }
                </tbody>
            </table>
        );
    } else {
        return (
            <div className="d-flex justify-content-center py-3">
                <div className="spinner-grow my-5" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            </div>
        );
    }
}


class UploadEntry extends React.Component {
    constructor(props) {
        super(props);
        this.item = props.itemData
        this.id = props.id;
    }

    render() {
        const item = this.item;

        // "Default" title + meta, for when the upload is created but hasn't parsed story title yet.
        let title = <>{ archiveFullName(item.origin.archive) } - { item.origin.ident }</>;
        let meta = <>{ item.guid }</>;

        // "Full" title + meta, for when the upload is well in progress.
        if (item.niceTitle) {
            title = <>{ item.niceTitle }</>;
            meta = <>{ archiveName(item.origin.archive) } - { item.origin.ident } - { item.timestamp }</>;
        }

        return (
            <div className={ `uf-uploads-entry ${ statusClass(item.status) }` } key={ item.guid }>
                <div className='digest'>
                    <UploadEntryHeader title={ title } meta={ meta } />
                    <UploadEntryFooter item={ item } id={ this.id } />
                </div>
                <div className='extended collapse' id={ this.id }>
                    <UploadEntryLogs logs={ item.logs } />
                </div>
            </div>
        );
    }
}

class UploadList extends React.Component {
    constructor(props) {
        super(props);
        this.targetId = props.targetId;
        this.prefix = props.targetPrefix;
        this.items = props.uploads;
    }

    render() {
        return ReactDom.createPortal(
            <React.Fragment>
                { this.items.map((item, index) =>
                    <UploadEntry key={ item.guid } itemData={ item } id={ this.prefix + index } />)
                }
            </React.Fragment>,
            document.getElementById(this.targetId)
        );
    }
}


class Uploader extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            uploads: []
        };
    }

    componentDidMount() {
        this.setState((state, props) => ({
            uploads: [
                {
                    'guid': '273D3D48-A9F7-40C9-ABB8-DE9549A031A3',
                    'origin': {
                        'archive': 'ffn',
                        'ident': '11178712'
                    },
                    'status': {
                        'code': 'fetching',
                        'type': null,
                        'extra': '3/58'
                    },
                    'timestamp': '22:52 09/01/21',
                    'niceTitle': 'OSaBC II : That Which Cannot Die',
                    'logs': []
                },
                {
                    'guid': 'd16f0007-6073-48f5-931a-b80d38132735',
                    'origin': {
                        'archive': 'ao3',
                        'ident': '33931603'
                    },
                    'status': {
                        'code': 'pending',
                        'type': null,
                        'extra': null
                    },
                    'timestamp': '22:02 09/01/21',
                    'niceTitle': null,
                    'logs': []
                },
                {
                    'guid': '8380F478-FFF9-407E-ABB2-0116145586B0',
                    'origin': {
                        'archive': 'ffn',
                        'ident': '13954844'
                    },
                    'status': {
                        'code': 'error',
                        'type': 'page_unavailable',
                        'extra': null
                    },
                    'timestamp': '22:02 09/01/21',
                    'niceTitle': 'When the Roses Bloom Again',
                    'logs': [
                        {
                            'time': '09/01/2021 22:01:11.374',
                            'level': 'info',
                            'message': 'Received request for <strong>FFN:13954844</strong>'
                        },
                        {
                            'time': '09/01/2021 22:01:11.376',
                            'level': 'debug',
                            'message': 'Created a private story with ID = <strong>133</strong>'
                        },
                        {
                            'time': '09/01/2021 22:01:11.377',
                            'level': 'debug',
                            'message': 'Created an upload <strong>8380F478-FFF9-407E-ABB2-0116145586B0</strong>'
                        },
                        {
                            'time': '09/01/2021 22:01:11.380',
                            'level': 'info',
                            'message': 'Made request to <strong>https://www.fanfiction.net/s/13954844/1</strong>'
                        },
                        {
                            'time': '09/01/2021 22:01:12.121',
                            'level': 'debug',
                            'message': 'Got HTTP 200 OK, found contents'
                        },
                        {
                            'time': '09/01/2021 22:01:12.121',
                            'level': 'info',
                            'message': 'Found <strong>14</strong> chapter(s), created k-v entries'
                        },
                        {
                            'time': '09/01/2021 22:01:14.125',
                            'level': 'info',
                            'message': 'Made request to <strong>https://www.fanfiction.net/s/13954844/2</strong>'
                        },
                        {
                            'time': '09/01/2021 22:01:17.005',
                            'level': 'error',
                            'message': 'Got HTTP 429 Rate Limit with k-v ID = <strong>781</strong>'
                        },
                    ]
                }
            ]
        }));
    }

    componentWillUnmount() {

    }

    render() {
        const isCompleted = (upload) => ['cancelled', 'completed', 'error'].includes(upload.status.code);
        let current = { uploads: this.state.uploads.filter((upload) => !isCompleted(upload)) };
        let previous = { uploads: this.state.uploads.filter((upload) => isCompleted(upload)) };

        return (
            <React.Fragment>
                <><UploadList targetId='uf-uploads-current' targetPrefix='current-upload-' uploads={ current.uploads } key={ current.uploads } /></>
                <><UploadList targetId='uf-uploads-previous' targetPrefix='previous-upload-' uploads={ previous.uploads } key={ previous.uploads } /></>
            </React.Fragment>
        )
    }
}

export default Uploader
