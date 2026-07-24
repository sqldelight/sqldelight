config.set({
  client: {
    ...(config.client || {}),
    mocha: {
      ...((config.client && config.client.mocha) || {}),
      timeout: 30_000,
    },
  },
  customHeaders: [
    {
      match: ".*",
      name: "Cross-Origin-Opener-Policy",
      value: "same-origin",
    },
    {
      match: ".*",
      name: "Cross-Origin-Embedder-Policy",
      value: "require-corp",
    },
  ],
});
