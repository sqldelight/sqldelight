config.set({
  client: {
    ...(config.client || {}),
    mocha: {
      ...((config.client && config.client.mocha) || {}),
      // Longer than kotlinx-coroutines-test's own 60s limit, so a stuck test reports which
      // coroutine never completed instead of a bare mocha timeout.
      timeout: 90_000,
    },
  },
  // Opening an OPFS database starts a Worker and instantiates a SQLite Wasm module, which can take
  // longer than Karma's default 30s activity window on a loaded CI machine.
  browserNoActivityTimeout: 120_000,
  customHeaders: [
    {
      // Every worker these tests create re-fetches the same SQLite build. Letting the browser cache
      // it keeps the suite from repeatedly downloading megabytes through the Karma server.
      match: ".*\\.wasm",
      name: "Cache-Control",
      value: "public, max-age=300",
    },
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
