import React from "react"
import ReactDom from "react-dom";

import UploadEntry from "./UploadEntry";


function UploadListContents(props) {
    const prefix = props.prefix;
    const items = props.items;

    if (items.length !== 0) {
        const entries = items.map((item, index) =>
            <UploadEntry key={ item.guid } itemData={ item } id={ prefix + index } />);

        return <>{ entries }</>;
    } else {
        return (
            <div className="card bg-light text-muted small">
                <div className="card-body">
                    <p className="mb-0">No uploads in this category!</p>
                </div>
            </div>
        );
    }
}

export default class UploadList extends React.Component {
    constructor(props) {
        super(props);
        this.targetId = props.targetId;
        this.prefix = props.targetPrefix;
        this.items = props.uploads;
    }

    render() {
        const target = document.getElementById(this.targetId);
        return ReactDom.createPortal(<UploadListContents items={this.items} prefix={this.prefix}/>, target);
    }
}
