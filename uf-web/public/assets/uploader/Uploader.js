import React from "react"
import UploadEntry from "./components/UploadEntry";


/**
 * Turn an array of UploadInfo instances
 * into a map (object) of (guid -> UploadInfo).
 */
function dictifyByGuid(current) {
    let dict = {};

    current.forEach(upload => {
        const guid = upload["guid"];
        dict[guid] = upload;
    });

    return dict;
}


export default class Uploader extends React.Component {
    constructor(props) {
        super(props);

        this.authKey = window.staticData.key;
        this.wsUrlPostfix = window.staticData.ws;

        this.uploadInput = document.getElementById("uf-add-story-url");
        this.uploadBtn = document.getElementById("uf-add-story-btn");

        this.webSocket = null;

        this.handleUploadCreation = this.handleUploadCreation.bind(this);
        this.handleUploadRemoval = this.handleUploadRemoval.bind(this);

        this.state = {

            isLoading: true,

            // global initialization/protocol error which prevents normal flow
            globalError: null,

            // submission errors which is a part of the normal flow
            localErrors: [],

            // list of displayed entries
            uploads: {}

        };
    }

    handleUploadCreation(link) {
        console.log(`Uploading story @ ${link} ...`);
        const requestData = {
            "request": "upload.create",
            "payload": {
                "urls": [ link ]
            }
        };

        this.webSocket?.send(JSON.stringify(requestData));
    }

    handleUploadRemoval(guid) {
        console.log(`Removing story @ ${guid} ...`);
        this.webSocket?.send(JSON.stringify({
            "request": "upload.remove",
            "payload": {
                "guids": [ guid ]
            }
        }));
    }

    componentDidMount() {

        const getWebsocketProtocol = () => {
            if (window.location.protocol.toLowerCase() === 'https:') {
                return 'wss';
            } else {
                return 'ws';
            }
        };

        const wsUrl = getWebsocketProtocol() + "://" + location.hostname + this.wsUrlPostfix;
        console.log("connecting to websocket server at '" + wsUrl + "'...");
        this.webSocket = new WebSocket(wsUrl);

        this.webSocket.onopen = () => {
            this.webSocket.send(JSON.stringify({
                "request": "auth.login",
                "payload": { "key": this.authKey }
            }));
        };
        this.webSocket.onmessage = (event) => {
            const message = JSON.parse(event.data);

            const code = message.response.toLowerCase();
            const payload = message.payload;

            switch (code) {
                case "auth.login-succeeded": {
                    this.setState({
                        isLoading: false,
                        globalError: null,
                        localErrors: [],
                        uploads: dictifyByGuid(message.payload.uploads ?? [])
                    });
                    break;
                }
                case "auth.login-failed": {
                    this.setState({
                        isLoading: false,
                        globalError: "failed to authenticate - " + payload.reason,
                        localErrors: [],
                        uploads: {}
                    });
                    break;
                }
                case "upload.created": {
                    if (payload.failed) {
                        const errorTexts = payload.failed.map(failure =>
                            `Failed to upload '${failure.url}'. Reason: ${failure.reason}`);

                        this.setState({
                            localErrors: [...this.state.localErrors, ...errorTexts]
                        });
                    }
                    if (payload.created) {
                        const uploads = dictifyByGuid(payload.created);
                        this.setState({
                            uploads: {...this.state.uploads, ...uploads}
                        });
                    }
                    break;
                }
                case "upload.removed": {
                    if (payload.failed) {
                        const errorTexts = payload.failed.map(failure =>
                            `Failed to remove upload '${failure.guid}'. Reason: ${failure.reason}`);

                        this.setState({
                            localErrors: [...this.state.localErrors, ...errorTexts]
                        });
                    }
                    if (payload.removed) {
                        let uploads = {...this.state.uploads};
                        payload.removed.forEach(guid => { delete uploads[guid]; });

                        this.setState({
                            uploads: uploads
                        });
                    }
                    break;
                }
                case "upload.info": {
                    if (payload.uploads) {
                        const uploads = dictifyByGuid(payload.uploads);
                        this.setState({
                            uploads: {...this.state.uploads, ...uploads}
                        });
                    }
                    break;
                }
                default: {
                    this.setState((state, props) => ({
                        globalError: "failed to map server message to handler, code = " + code
                    }));
                }
            }
        };
        this.webSocket.onclose = (event) => {
            console.log(event);

            if (!event.wasClean) {
                this.setState({
                    globalError: "connection interrupted, code = " + event.code + ", reason = " + event.reason,
                    uploads: {}
                });
            } else {
                this.setState({
                    isLoading: true,
                    globalError: null,
                    localErrors: [],
                    uploads: {}
                });
            }
        };

        this.uploadBtnHandler = () => {
            const linkText = this.uploadInput.value;
            if (linkText !== null) {
                this.uploadInput.value = "";
                this.handleUploadCreation(linkText);
            } else {
                this.setState({
                    localErrors: [...this.state.localErrors, `Invalid URL provided: ${linkText}`]
                });
            }
        };

        this.uploadBtn.addEventListener("click", this.uploadBtnHandler);
    }

    componentWillUnmount() {
        this.webSocket?.close();
        this.uploadBtn.removeEventListener("click", this.uploadBtnHandler);
    }

    render() {
        if (this.state.globalError !== null) {
            return (
                <div className="card bg-danger text-white small">
                    <div className="card-body">
                        <p className="mb-0"><strong>Fatal error:</strong> { this.state.globalError }</p>
                    </div>
                </div>
            );
        }
        if (this.state.isLoading === true) {
            return (
                <div className="card-body text-center">
                    <div className="spinner-border text-secondary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            );
        }

        const makeWarningList = () => {
            const warnings = this.state.localErrors;
            if (warnings.length === 0) {
                return <></>;
            }
            const entries = warnings.map((warning, index) => {
                return (
                    <p className="text-danger small mb-3" key={`warning-${index}`}>
                        { warning }
                    </p>
                );
            });
            return <div className="mb-5 mt-n5">{ entries }</div>;
        };
        const makeEntryList = () => {
            const keys = Object.keys(this.state.uploads);
            if (keys.length === 0) {
                return (
                    <div className="card bg-light text-muted small">
                        <div className="card-body">
                            <p className="mb-0">Looks like nothing's here.</p>
                        </div>
                    </div>
                );
            }
            const entries = keys.map((itemKey, index) => {
                const item = this.state.uploads[itemKey];
                return <UploadEntry key={ item.guid + '-' + item.status.code } itemData={ item }
                                    id={ `upload-${index}` } onDeleteClick={ this.handleUploadRemoval } />;
            });
            return <>{ entries }</>;
        };

        return (
            <>
                { makeWarningList() }
                { makeEntryList() }
            </>
        );

    }
}
