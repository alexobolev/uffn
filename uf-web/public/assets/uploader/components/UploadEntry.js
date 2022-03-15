import React from "react"

import {archiveFullName, archiveName, statusClass} from "../misc/mappings";
import UploadEntryHeader from "./UploadEntryHeader";
import UploadEntryFooter from "./UploadEntryFooter";


export default class UploadEntry extends React.Component {
    constructor(props) {
        super(props);
        this.item = props.itemData
        this.id = props.id;
        this.onDeleteClick = props.onDeleteClick;
    }

    render() {
        const item = this.item;

        // "Default" title + meta, for when the upload is created but hasn't parsed story title yet.
        let title = <>{ archiveFullName(item.origin.archive) } <>&bull;</> { item.origin.ident }</>;
        let meta = <>{ item.timestamp }</>;

        // "Full" title + meta, for when the upload is well in progress.
        if (item.niceTitle) {
            title = <>{ item.niceTitle }</>;
            meta = <>{ archiveName(item.origin.archive) } <>&bull;</> { item.origin.ident } <>&bull;</> { item.timestamp }</>;
        }

        return (
            <div className={ `uf-uploads-entry ${ statusClass(item.status) }` } key={ item.guid }>
                <div className='digest'>
                    <UploadEntryHeader title={ title } meta={ meta } />
                    <UploadEntryFooter item={ item } id={ this.id }
                                       onDeleteClick={ (e) => { this.onDeleteClick(item.guid) } } />
                </div>
            </div>
        );
    }
}
