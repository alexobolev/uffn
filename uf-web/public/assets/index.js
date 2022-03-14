import React from "react"
import ReactDom from "react-dom"

import Uploader from "./uploader/Uploader"
import "./styles.scss"
import "bootstrap-icons/font/bootstrap-icons.scss";

const uploadInstance = document.getElementById("uf-uploader")
if (uploadInstance !== null) {
    ReactDom.render(<Uploader/>, document.getElementById("uf-uploader"))
}
