import React from "react"


export default function UploadEntryHeader(props) {
    return (
        <header>
            <span className='upload-title'><span>{ props.title }</span></span>
            <span className='upload-meta'>{ props.meta }</span>
        </header>
    );
}
