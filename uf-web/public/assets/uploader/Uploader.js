import React from "react"
import ReactDom from "react-dom";
import { Dropdown, Tab } from "bootstrap";

import UploadList from "./components/UploadList"


export default class Uploader extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            uploads: [],
            error: null
        };
    }

    componentDidMount() {
        this.webSocket = new WebSocket('ws://127.0.0.1:7070/upload');
        this.webSocket.onopen = (event) => {
            console.log("[UF] Connected!");

            this.webSocket.send(JSON.stringify({
                "request": "auth.login",
                "payload": {
                    "key": "bc4bed3fc8d14caab50d7162bcfb99ea7ac205a6541b33026b65d2c5fda4c771"
                }
            }));

        };
        this.webSocket.onmessage = (event) => {
            console.log(JSON.parse(event.data));

            const message = JSON.parse(event.data);
            const code = message.response.toLowerCase();
            const payload = message.payload;

            switch (code) {
                case "auth.login-succeeded": {
                    console.log("[UF] Login succeeded");
                    this.setState((state, props) => ({
                        uploads: message.payload.uploads
                    }));
                    break;
                }
                case "auth.login-failed": {
                    console.log("[UF] Login failed, reason = " + payload.reason);
                    this.setState((state, props) => ({
                        error: payload.reason
                    }));
                    break;
                }
                case "upload.created": {
                    console.log("TODO: upload.created");
                    break;
                }
                case "upload.removed": {
                    console.log("TODO: upload.removed");
                    break;
                }
                case "upload.info": {
                    console.log("TODO: upload.info");
                    break;
                }
                default: {
                    console.log("[UF] Unmapped server message, code = " + code);
                    this.setState((state, props) => ({
                        error: "Failed to map server message to handler, code = " + code
                    }));
                }
            }
        };
        this.webSocket.onclose = (event) => {
            console.log("[UF] Closed!");
            this.setState((state, props) => ({
                uploads: []
            }));
        };
    }

    componentWillUnmount() {
        this.webSocket?.close();
    }

    render() {
        const isCompleted = (upload) => ['cancelled', 'completed', 'errored'].includes(upload.status.code.toLowerCase());

        const currentList = this.state.uploads.filter((upload) => !isCompleted(upload));
        const previousList = this.state.uploads.filter((upload) => isCompleted(upload));

        return (
            <>
                <><UploadList targetId='uf-uploads-current' targetPrefix='current-upload-' uploads={ currentList } key={ currentList } /></>
                <><UploadList targetId='uf-uploads-previous' targetPrefix='previous-upload-' uploads={ previousList } key={ previousList } /></>
            </>
        )
    }
}
