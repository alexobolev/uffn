import React from "react"
import ReactDom from "react-dom"

import Uploader from "./uploader"
import "./styles.scss"
import "bootstrap-icons/font/bootstrap-icons.scss";

ReactDom.render(<Uploader/>, document.getElementById("uf-uploader"))
