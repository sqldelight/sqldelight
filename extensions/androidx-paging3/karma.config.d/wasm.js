const path = require("path");
const abs = path.resolve("../../node_modules/sql.js/dist/sql-wasm.wasm")

config.files.push({
    pattern: abs,
    served: true,
    watched: false,
    included: false,
    nocache: false,
});

config.proxies["/sql-wasm.wasm"] = `/absolute${abs}`
