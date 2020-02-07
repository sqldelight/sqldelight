var CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin([
        { from: '../../node_modules/sql.js/dist/sql-wasm.wasm',
            to: '../../../../sample/web/build/distributions' }
    ])
);
