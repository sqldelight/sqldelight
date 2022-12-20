const path = require("path");
const dist = path.resolve("../../node_modules/sql.js/dist/")
const wasm = path.join(dist, "sql-wasm.wasm")
const worker = path.resolve("kotlin/sqljs.worker.js")

console.error("Some text")

config.files.push({
    pattern: wasm,
    served: true,
    watched: false,
    included: false,
    nocache: false,
}, {
    pattern: worker,
    served: true,
    watched: false,
    included: false,
    nocache: false,
});

config.proxies["/sql-wasm.wasm"] = path.join("/absolute/", wasm)
config.proxies["/sqljs.worker.js"] = path.join("/absolute/", worker)
