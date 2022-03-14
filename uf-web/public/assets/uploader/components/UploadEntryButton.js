import React from "react"

export default function UploadEntryButton(props) {
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
