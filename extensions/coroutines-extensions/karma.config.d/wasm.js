const path = require("path");
const dist = path.resolve("../../node_modules/sql.js/dist/")
const wasm = path.join(dist, "sql-wasm.wasm")
const worker = path.join(dist, "worker.sql-wasm.js")

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

config.proxies["/sql-wasm.wasm"] = `/absolute${wasm}`
config.proxies["/worker.sql-wasm.js"] = `/absolute${worker}`
