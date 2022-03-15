import React from "react"

export default function UploadEntryButton(props) {
    const classDisabled = props.enabled ? '' : 'disabled';
    const linkTarget = props.newTab ? '_blank' : '_self';

    const callback = props.action ?? ((e) => {});

    return (
        <a href={props.href} target={linkTarget}
           className={ `btn btn-sm ${classDisabled}` }
           onClick={ (e) => { callback(e) } }
           {...props.extraAttrs}><i className={ `bi bi-${props.iconClass}` } /></a>
    );
}

UploadEntryButton.defaultProps = {
    enabled: true,
    extraAttrs: [],
    href: '#',
    iconClass: 'exclamation-circle',
    newTab: false,
    action: null
};
