import React from "react"
import ReactDom from "react-dom"

import "bootstrap-icons/font/bootstrap-icons.scss";
import { Alert, Dropdown, Tab } from "bootstrap";

import Uploader from "./uploader/Uploader"
import "./styles.scss"


const uploadInstance = document.getElementById("uf-uploader")
if (uploadInstance !== null) {
    ReactDom.render(<Uploader/>, document.getElementById("uf-uploader"))
}
