import React from "react"
import UploadEntryButton from "./UploadEntryButton";
import {statusName} from "../misc/mappings";


function UploadEntryBadge(props) {
    const name = statusName(props.status);
    return (
        <span className="badge">
            { name } { props.status.extra }
        </span>
    );
}

export default function UploadEntryFooter(props) {
    const canRetry = props.item.status.code.toLowerCase() === 'errored';
    const canDelete = props.item.status.code.toLowerCase() !== 'fetching';

    return (
        <footer>
            <div className="status">
                <UploadEntryBadge status={ props.item.status } />
            </div>
            <div className="buttons">
                <UploadEntryButton iconClass="arrow-clockwise" enabled={canRetry} />
                <UploadEntryButton iconClass="globe" href={ props.item.url } newTab={true} />
                <UploadEntryButton iconClass="trash" enabled={canDelete} />
            </div>
        </footer>
    );
}
