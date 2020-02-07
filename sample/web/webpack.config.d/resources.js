var CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin([
        { from: '../../../../sample/web/src/main/resources',
            to: '../../../../sample/web/build/distributions' }
    ])
);
